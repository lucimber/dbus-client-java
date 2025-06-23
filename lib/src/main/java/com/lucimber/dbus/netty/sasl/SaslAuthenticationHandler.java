/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.netty.DBusChannelEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class SaslAuthenticationHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaslAuthenticationHandler.class);

  private enum SaslState {
    IDLE,
    AWAITING_SERVER_MECHS,
    NEGOTIATING,
    AWAITING_BEGIN_CONFIRMATION,
    AUTHENTICATED,
    FAILED
  }

  private SaslState currentState = SaslState.IDLE;
  private final List<SaslMechanism> clientMechanismsPreference;
  private SaslMechanism currentMechanism;
  private List<String> serverSupportedMechanisms;
  private int currentMechanismAttemptIndex = 0;

  public SaslAuthenticationHandler(List<SaslMechanism> preferredClientMechanisms) {
    Objects.requireNonNull(preferredClientMechanisms, "Client mechanisms list cannot be null.");
    this.clientMechanismsPreference = preferredClientMechanisms.isEmpty()
          ? List.of(new ExternalSaslMechanism(), new CookieSaslMechanism(), new AnonymousSaslMechanism())
          : new ArrayList<>(preferredClientMechanisms);
  }

  public SaslAuthenticationHandler() {
    this(Collections.emptyList());
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == DBusChannelEvent.SASL_NUL_BYTE_SENT && currentState == SaslState.IDLE) {
      LOGGER.debug("SASL_NUL_BYTE_SENT event received. Adding SASL string codecs and initiating AUTH.");
      SaslMessage authMsg = new SaslMessage(SaslCommandName.AUTH, null);
      ctx.writeAndFlush(authMsg);
      currentState = SaslState.AWAITING_SERVER_MECHS;
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof SaslMessage saslMessage) {
      LOGGER.debug("SASL RECV: {}", saslMessage);
      switch (currentState) {
        case AWAITING_SERVER_MECHS, NEGOTIATING -> handleSaslServerResponse(ctx, saslMessage);
        default -> {
          LOGGER.warn("Received SASL command '{}' in unexpected state: {}.", saslMessage.getCommandName(), currentState);
          if (EnumSet.of(SaslState.AUTHENTICATED, SaslState.FAILED).contains(currentState)) {
            LOGGER.warn("Ignoring SASL command '{}' as SASL state is already {}.", saslMessage.getCommandName(), currentState);
          } else {
            failAuthentication(ctx, "Unexpected SASL command '" + saslMessage.getCommandName() + "' in state " + currentState);
          }
        }
      }
    } else {
      LOGGER.warn("Received unexpected non-SASL message type during SASL: {}", msg.getClass().getName());
      if (currentState == SaslState.AUTHENTICATED) {
        LOGGER.debug("Passing non-string message up, assuming post-SASL and pre-DBus-pipeline data.");
        ctx.fireChannelRead(msg);
      } else {
        failAuthentication(ctx, "Received non-string data during active SASL exchange.");
      }
    }
  }

  private void handleSaslServerResponse(ChannelHandlerContext ctx, SaslMessage msg) {
    SaslCommandName command = msg.getCommandName();
    String args = msg.getCommandArgs().orElse("");
    switch (command) {
      case OK -> {
        LOGGER.info("SASL server OK. GUID: {}. Sending BEGIN.", args);
        SaslMessage beginMsg = new SaslMessage(SaslCommandName.BEGIN, null);
        ctx.writeAndFlush(beginMsg).addListener(future -> {
          if (future.isSuccess()) {
            LOGGER.debug("BEGIN command sent successfully.");
            currentState = SaslState.AUTHENTICATED;
            cleanupAndSignalCompletion(ctx, true);
          } else {
            LOGGER.error("Failed to send BEGIN: {}", future.cause().getMessage());
            failAuthentication(ctx, "Failed to send BEGIN: " + future.cause().getMessage());
          }
        });
      }
      case REJECTED -> {
        serverSupportedMechanisms = Arrays.asList(args.split(" "));
        LOGGER.warn("SASL mechanism rejected. Server supports: {}", serverSupportedMechanisms);
        disposeCurrentMechanism();
        tryNextMechanism(ctx);
      }
      case DATA -> {
        if (currentMechanism == null || currentState != SaslState.NEGOTIATING) {
          failAuthentication(ctx, "Unexpected DATA command without mechanism or wrong state.");
          return;
        }
        currentMechanism.processChallengeAsync(ctx, args).addListener(future -> {
          if (!ctx.channel().isActive()) return;
          if (future.isSuccess()) {
            var responseHex = (String) future.getNow();
            if (responseHex != null) {
              SaslMessage dataMsg = new SaslMessage(SaslCommandName.DATA, responseHex);
              ctx.writeAndFlush(dataMsg);
            } else {
              LOGGER.debug("Mechanism {} complete, awaiting server response.", currentMechanism.getName());
            }
          } else {
            LOGGER.error("Failed to process challenge with mechanism {}", currentMechanism.getName(), future.cause());
            SaslMessage cancelMsg = new SaslMessage(SaslCommandName.CANCEL, null);
            ctx.writeAndFlush(cancelMsg);
          }
        });
      }
      case ERROR -> {
        LOGGER.error("SASL server ERROR: {}", args);
        SaslMessage cancelMsg = new SaslMessage(SaslCommandName.CANCEL, null);
        ctx.writeAndFlush(cancelMsg);
      }
      case AGREE_UNIX_FD -> LOGGER.info("Server agreed to UNIX FD passing.");
      default -> {
        if (currentState == SaslState.AWAITING_SERVER_MECHS &&
              command.name().matches("[A-Z0-9_]+([-A-Z0-9_]*[A-Z0-9_]+)?")) {
          serverSupportedMechanisms = Arrays.asList(msg.toString().split(" "));
          LOGGER.debug("Server mechanisms: {}", serverSupportedMechanisms);
          currentMechanismAttemptIndex = 0;
          tryNextMechanism(ctx);
        } else {
          failAuthentication(ctx, "Unexpected command: " + command + " with args: " + args);
        }
      }
    }
  }

  private void tryNextMechanism(ChannelHandlerContext ctx) {
    if (serverSupportedMechanisms == null) {
      failAuthentication(ctx, "No server mechanisms provided.");
      return;
    }
    disposeCurrentMechanism();
    if (currentMechanismAttemptIndex < clientMechanismsPreference.size()) {
      var candidate = clientMechanismsPreference.get(currentMechanismAttemptIndex++);
      if (serverSupportedMechanisms.contains(candidate.getName())) {
        currentMechanism = candidate;
        LOGGER.info("Trying SASL mechanism: {}", candidate.getName());
        try {
          currentMechanism.init(ctx);
          currentMechanism.getInitialResponseAsync(ctx).addListener(future -> {
            if (!ctx.channel().isActive()) return;
            if (future.isSuccess()) {
              String initialResponse = (String) future.getNow();
              String value = initialResponse != null && !initialResponse.isEmpty() ? initialResponse : null;
              SaslMessage authMsg = new SaslMessage(SaslCommandName.AUTH, value);
              ctx.writeAndFlush(authMsg);
              currentState = SaslState.NEGOTIATING;
            } else {
              LOGGER.error("Failed to get initial response for {}: {}", candidate.getName(), future.cause().getMessage());
              tryNextMechanism(ctx);
            }
          });
        } catch (SaslMechanismException e) {
          LOGGER.warn("Initialization failed for {}: {}", candidate.getName(), e.getMessage());
        }
      }
    } else {
      failAuthentication(ctx, "No compatible SASL mechanism found.");
    }
  }

  private void failAuthentication(ChannelHandlerContext ctx, String reason) {
    if (currentState == SaslState.FAILED) return;
    LOGGER.error("SASL Authentication Failed: {}", reason);
    currentState = SaslState.FAILED;
    if (currentMechanism != null) {
      SaslMessage responseMsg = new SaslMessage(SaslCommandName.CANCEL, null);
      ctx.writeAndFlush(responseMsg).addListener(f -> cleanupAndSignalCompletion(ctx, false));
    } else {
      cleanupAndSignalCompletion(ctx, false);
    }
  }

  private void cleanupAndSignalCompletion(ChannelHandlerContext ctx, boolean success) {
    disposeCurrentMechanism();

    try {
      ctx.pipeline().remove(this);
      LOGGER.debug("SaslAuthenticationHandler removed itself from pipeline.");
    } catch (NoSuchElementException ignored) {
    }

    ctx.fireUserEventTriggered(success ? DBusChannelEvent.SASL_AUTH_COMPLETE : DBusChannelEvent.SASL_AUTH_FAILED);
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
    if (!EnumSet.of(SaslState.AUTHENTICATED, SaslState.FAILED).contains(currentState)) {
      failAuthentication(ctx, "Channel became inactive.");
    }
    disposeCurrentMechanism();
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (currentState == SaslState.FAILED) {
      LOGGER.debug("Ignoring exception as SASL already in FAILED state: ", cause);
      return;
    }
    LOGGER.error("Exception in SaslAuthenticationHandler. State: {}", currentState, cause);
    failAuthentication(ctx, "Exception caught: " + cause.getMessage());
  }
}
