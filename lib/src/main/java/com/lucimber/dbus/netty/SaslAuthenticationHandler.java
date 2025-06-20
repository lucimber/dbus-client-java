/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.netty.sasl.*;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SaslAuthenticationHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaslAuthenticationHandler.class);
  private static final String CRLF = "\r\n";
  private static final int MAX_SASL_LINE_LENGTH = 2048; // Increased slightly for safety

  private enum SaslState {
    /**
     * Waiting for SASL_NUL_BYTE_SENT event
     */
    IDLE,
    /**
     * Sent initial AUTH, waiting for server's mechanism list or OK/REJECTED
     */
    AWAITING_SERVER_MECHS,
    /**
     * Mechanism selected, exchanging DATA/challenge-response
     */
    NEGOTIATING,
    /**
     * Sent BEGIN, technically SASL is done, but waiting for this to ensure it's out.
     */
    AWAITING_BEGIN_CONFIRMATION,
    /**
     * SASL protocol phase considered complete after BEGIN is sent.
     */
    AUTHENTICATED,
    FAILED
  }

  private SaslState currentState = SaslState.IDLE;
  private final List<SaslMechanism> clientMechanismsPreference;
  private SaslMechanism currentMechanism = null;
  private List<String> serverSupportedMechanisms = null;
  private int currentMechanismAttemptIndex = 0;

  private static final String SASL_LINE_DECODER_NAME = "saslLineDecoder";
  private static final String SASL_STRING_DECODER_NAME = "saslStringDecoder";
  private static final String SASL_STRING_ENCODER_NAME = "saslStringEncoder";


  public SaslAuthenticationHandler(List<SaslMechanism> preferredClientMechanisms) {
    this.clientMechanismsPreference = new ArrayList<>(
          Objects.requireNonNull(preferredClientMechanisms, "Client mechanisms list cannot be null.")
    );
    if (this.clientMechanismsPreference.isEmpty()) {
      LOGGER.info("No preferred SASL mechanisms provided, using default order: EXTERNAL, DBUS_COOKIE_SHA1, ANONYMOUS");
      this.clientMechanismsPreference.add(new ExternalSaslMechanism());
      this.clientMechanismsPreference.add(new CookieSaslMechanism());
      this.clientMechanismsPreference.add(new AnonymousSaslMechanism());
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == DbusChannelEvent.SASL_NUL_BYTE_SENT && currentState == SaslState.IDLE) {
      LOGGER.debug("SASL_NUL_BYTE_SENT event received. Adding SASL string codecs and initiating AUTH.");
      ChannelPipeline pipeline = ctx.pipeline();
      // Add handlers before this one
      pipeline.addBefore(ctx.name(), SASL_LINE_DECODER_NAME, new LineBasedFrameDecoder(MAX_SASL_LINE_LENGTH, true, true));
      pipeline.addBefore(ctx.name(), SASL_STRING_DECODER_NAME, new StringDecoder(StandardCharsets.US_ASCII));
      pipeline.addBefore(ctx.name(), SASL_STRING_ENCODER_NAME, new StringEncoder(StandardCharsets.US_ASCII)); // Assumes commands are sent as Strings

      sendSaslCommand(ctx, "AUTH");
      currentState = SaslState.AWAITING_SERVER_MECHS;
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof String)) {
      LOGGER.warn("Received unexpected non-string message type during SASL:" +
            " {} (Pipeline misconfiguration?)", msg.getClass().getName());
      // This might indicate string decoder wasn't present or removed too early
      // Or it's data after SASL phase but before pipeline reconfig for DBus messages
      if (currentState == SaslState.AUTHENTICATED) {
        LOGGER.debug("Passing non-string message up, assuming post-SASL and pre-DBus-pipeline data.");
        ctx.fireChannelRead(msg); // Pass it on
      } else {
        failAuthentication(ctx, "Received non-string data during active SASL exchange.");
      }
      return;
    }
    String line = ((String) msg).trim(); // Trim CRLF handled by LineBasedFrameDecoder
    LOGGER.debug("SASL RECV: {}", line);

    String[] parts = line.split(" ", 2);
    String cmd = parts[0].toUpperCase(); // Ensure command is uppercase for matching

    switch (currentState) {
      case AWAITING_SERVER_MECHS:
      case NEGOTIATING:
        handleSaslServerResponse(ctx, cmd, parts.length > 1 ? parts[1] : "");
        break;
      default:
        LOGGER.warn("Received SASL command '{}' in unexpected state: {}. Full line: {}",
              cmd, currentState, line);
        if (currentState != SaslState.AUTHENTICATED && currentState != SaslState.FAILED) {
          failAuthentication(ctx, "Unexpected SASL command '" + cmd + "' in state " + currentState);
        } else {
          // If already authenticated or failed, this message is out of place for SASL.
          // Could be a late/extra message from server, or pipeline issue.
          LOGGER.warn("Ignoring SASL command '{}' as SASL state is already {}.", cmd, currentState);
        }
    }
  }

  private void handleSaslServerResponse(ChannelHandlerContext ctx, String cmd, String args) {
    switch (cmd) {
      case "OK":
        String guid = args.trim();
        LOGGER.info("SASL server sent OK. GUID: {}. Sending BEGIN command.", guid);
        // Client MUST now send BEGIN
        sendSaslCommand(ctx, "BEGIN").addListener(future -> {
          if (future.isSuccess()) {
            LOGGER.debug("BEGIN command sent successfully.");
            currentState = SaslState.AUTHENTICATED;
            cleanupAndSignalCompletion(ctx, true);
          } else {
            LOGGER.error("Failed to send BEGIN command after SASL OK.", future.cause());
            failAuthentication(ctx, "Failed to send BEGIN: " + future.cause().getMessage());
          }
        });
        // State change and cleanup will happen in the listener of BEGIN command write.
        break;

      case "REJECTED":
        serverSupportedMechanisms = Arrays.asList(args.split(" "));
        LOGGER.warn("SASL mechanism {} rejected by server. Server supports: {}",
              (currentMechanism != null ? currentMechanism.getName() : "initial AUTH or previous mechanism"),
              serverSupportedMechanisms);
        disposeCurrentMechanism();
        tryNextMechanism(ctx);
        break;

      case "DATA":
        if (currentMechanism == null || currentState != SaslState.NEGOTIATING) {
          failAuthentication(ctx, "Received DATA challenge in unexpected state" +
                " or without active mechanism.");
          return;
        }
        String challengeHex = args.trim();
        currentMechanism.processChallengeAsync(ctx, challengeHex)
              .addListener(future -> {
                if (!ctx.channel().isActive()) return;
                if (future.isSuccess()) {
                  String responseHex = (String) future.getNow();
                  if (responseHex != null) {
                    sendSaslCommand(ctx, "DATA", responseHex);
                  } else {
                    LOGGER.debug("Mechanism {} client-side complete after challenge," +
                          " awaiting server OK/REJECTED.", currentMechanism.getName());
                  }
                } else {
                  LOGGER.error("SASL mechanism {} failed to process challenge",
                        currentMechanism.getName(), future.cause());
                  sendSaslCommand(ctx, "CANCEL"); // Abort current mechanism attempt
                  // Server should respond with REJECTED, which will trigger tryNextMechanism
                  // Or we could call tryNextMechanism here after a delay or based on error type
                }
              });
        break;

      case "ERROR": // Server indicates a problem with client's command
        LOGGER.error("SASL server reported an ERROR: {}", args);
        sendSaslCommand(ctx, "CANCEL"); // Client should try to cancel on server error
        // Server should respond with REJECTED.
        break;

      case "AGREE_UNIX_FD": // Server agrees to FD passing
        LOGGER.info("Server agreed to UNIX FD passing." +
              " This typically happens after OK and client sending NEGOTIATE_UNIX_FD.");
        // The main flow is OK -> BEGIN. FD negotiation is an optional sub-flow.
        // If we sent NEGOTIATE_UNIX_FD and received this, we'd still proceed to BEGIN for D-Bus messages.
        // Currently, we don't send NEGOTIATE_UNIX_FD.
        break;

      default: // Usually the list of mechanisms from the server after initial "AUTH"
        String regex = "[A-Z0-9_]+([-A-Z0-9_]*[A-Z0-9_]+)?";
        if (currentState == SaslState.AWAITING_SERVER_MECHS && cmd.matches(regex)) {
          String allMechsString = cmd + (args.isEmpty() ? "" : " " + args);
          serverSupportedMechanisms = Arrays.asList(allMechsString.split(" "));
          LOGGER.debug("Received server mechanisms: {}", serverSupportedMechanisms);
          currentMechanismAttemptIndex = 0;
          tryNextMechanism(ctx);
        } else {
          failAuthentication(ctx, "Unknown or unexpected SASL server command: "
                + cmd + " with args: " + args);
        }
    }
  }

  private void tryNextMechanism(ChannelHandlerContext ctx) {
    if (serverSupportedMechanisms == null) {
      failAuthentication(ctx, "Server did not provide a list of supported mechanisms.");
      return;
    }

    disposeCurrentMechanism(); // Dispose previous one before trying next

    while (currentMechanismAttemptIndex < clientMechanismsPreference.size()) {
      SaslMechanism candidate = clientMechanismsPreference.get(currentMechanismAttemptIndex++);
      if (serverSupportedMechanisms.contains(candidate.getName())) {
        currentMechanism = candidate;
        LOGGER.info("Attempting SASL mechanism: {}", currentMechanism.getName());
        try {
          currentMechanism.init(ctx);
          currentMechanism.getInitialResponseAsync(ctx).addListener(future -> {
            if (!ctx.channel().isActive()) {
              return;
            }

            if (future.isSuccess()) {
              String initialResponseHex = (String) future.getNow();
              String commandToSend = "AUTH " + currentMechanism.getName();
              if (initialResponseHex != null && !initialResponseHex.isEmpty()) {
                commandToSend += " " + initialResponseHex;
              }
              sendSaslCommand(ctx, commandToSend);
              currentState = SaslState.NEGOTIATING;
            } else {
              LOGGER.error("SASL mechanism {} failed during initial response generation.",
                    currentMechanism.getName(), future.cause());
              tryNextMechanism(ctx); // Try next automatically
            }
          });

          return; // Exit while loop, an attempt has started (asynchronously)
        } catch (SaslMechanismException e) {
          LOGGER.warn("Failed to initialize SASL mechanism {}: {}", candidate.getName(), e.getMessage());
          // Loop continues to try next mechanism
        }
      }
    }

    failAuthentication(ctx, "No suitable SASL mechanism found. Client supports: " +
          clientMechanismsPreference.stream().map(SaslMechanism::getName).toList() +
          ", Server offered: " + serverSupportedMechanisms);
  }


  private ChannelFuture sendSaslCommand(ChannelHandlerContext ctx, String cmd) {
    return sendSaslCommand(ctx, cmd, null);
  }

  private ChannelFuture sendSaslCommand(ChannelHandlerContext ctx, String cmd, String args) {
    String fullCommand = cmd + (args != null ? " " + args : "") + CRLF;
    LOGGER.debug("SASL SEND: {}", fullCommand.trim());
    // Assuming StringEncoder("US_ASCII") is in pipeline before this handler
    // StringEncoder adds CRLF if configured, or we add it manually.
    // DBus SASL lines are terminated by \r\n. StringEncoder typically doesn't add it.
    // So we add it ourselves.
    return ctx.writeAndFlush(fullCommand); // StringEncoder will convert to ByteBuf and add CRLF
  }

  private void failAuthentication(ChannelHandlerContext ctx, String reason) {
    if (currentState == SaslState.FAILED) {
      return; // Already failed
    }

    LOGGER.error("SASL Authentication Failed: {}", reason);
    currentState = SaslState.FAILED;

    // Try to send CANCEL if we were negotiating with a mechanism
    if (currentMechanism != null && currentState != SaslState.AWAITING_SERVER_MECHS) {
      sendSaslCommand(ctx, "CANCEL")
            .addListener(f -> cleanupAndSignalCompletion(ctx, false));
    } else {
      cleanupAndSignalCompletion(ctx, false);
    }
  }

  private void cleanupAndSignalCompletion(ChannelHandlerContext ctx, boolean success) {
    disposeCurrentMechanism();
    ChannelPipeline pipeline = ctx.pipeline();
    final String[] saslHandlerNames = {SASL_LINE_DECODER_NAME, SASL_STRING_DECODER_NAME, SASL_STRING_ENCODER_NAME};
    for (String name : saslHandlerNames) {
      try {
        if (pipeline.get(name) != null) pipeline.remove(name);
      } catch (NoSuchElementException e) { /* ignore if already removed */ }
    }
    try {
      // Remove self
      ctx.pipeline().remove(this);
      LOGGER.debug("SaslAuthenticationHandler removed itself from pipeline.");
    } catch (NoSuchElementException e) { /* ignore */ }

    if (success) {
      LOGGER.debug("Firing SASL_AUTH_COMPLETE event.");
      ctx.fireUserEventTriggered(DbusChannelEvent.SASL_AUTH_COMPLETE);
    } else {
      LOGGER.debug("Firing SASL_AUTH_FAILED event.");
      ctx.fireUserEventTriggered(DbusChannelEvent.SASL_AUTH_FAILED);
      // Don't close channel here, let DbusPipelineConfigurer or ConnectionCompletionHandler decide
      // based on SASL_AUTH_FAILED event.
    }
  }

  private void disposeCurrentMechanism() {
    if (currentMechanism != null) {
      try {
        currentMechanism.dispose();
      } catch (Exception e) {
        LOGGER.warn("Error disposing SASL mechanism {}: {}", currentMechanism.getName(), e.getMessage());
      }
      currentMechanism = null;
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.warn("Channel became inactive during SASL authentication. Current state: {}", currentState);
    if (currentState != SaslState.AUTHENTICATED && currentState != SaslState.FAILED) {
      failAuthentication(ctx, "Channel became inactive.");
    }
    disposeCurrentMechanism();
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (currentState == SaslState.FAILED) { // Avoid double failure processing
      LOGGER.debug("Ignoring exception as SASL already in FAILED state: ", cause);
      return;
    }
    if (cause instanceof DecoderException && cause.getMessage() != null &&
          (cause.getMessage().contains("over " + MAX_SASL_LINE_LENGTH + " byte") ||
                cause.getMessage().contains("UTF-8"))) { // from StringDecoder for non-ASCII
      LOGGER.warn("SASL line too long or invalid characters," +
            " possibly not a SASL server or misconfiguration: {}", cause.getMessage());
    } else {
      LOGGER.error("Exception in SaslAuthenticationHandler. Current state: {}", currentState, cause);
    }
    failAuthentication(ctx, "Exception caught: " + cause.getMessage());
    // Do not call super.exceptionCaught if we handle it by failing and potentially closing.
    // If we want it to propagate for other handlers (e.g. a general error logger/closer at the end):
    // super.exceptionCaught(ctx, cause);
  }
}
