/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.netty.DBusChannelEvent;
import com.lucimber.dbus.netty.WriteOperationListener;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaslAuthenticationHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaslAuthenticationHandler.class);
    private final List<SaslMechanism> clientMechanismsPreference;
    private SaslState currentState = SaslState.IDLE;
    private SaslMechanism currentMechanism;
    private List<String> serverSupportedMechanisms;
    private int currentMechanismAttemptIndex = 0;

    public SaslAuthenticationHandler(List<SaslMechanism> preferredClientMechanisms) {
        Objects.requireNonNull(preferredClientMechanisms, "Client mechanisms list cannot be null.");
        this.clientMechanismsPreference =
                preferredClientMechanisms.isEmpty()
                        ? List.of(
                                new ExternalSaslMechanism(),
                                new CookieSaslMechanism(),
                                new AnonymousSaslMechanism())
                        : new ArrayList<>(preferredClientMechanisms);
    }

    public SaslAuthenticationHandler() {
        this(Collections.emptyList());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // Handle reconnection events
        if (evt == DBusChannelEvent.RECONNECTION_STARTING) {
            reset();
            ctx.fireUserEventTriggered(evt);
            return;
        }

        if (evt == DBusChannelEvent.SASL_NUL_BYTE_SENT && currentState == SaslState.IDLE) {
            LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE, "SASL_NUL_BYTE_SENT event received.");
            SaslMessage authMsg = new SaslMessage(SaslCommandName.AUTH, null);
            ctx.writeAndFlush(authMsg).addListener(new WriteOperationListener<>(LOGGER));
            currentState = SaslState.AWAITING_SERVER_MECHS;
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof SaslMessage saslMessage) {
            switch (currentState) {
                case AWAITING_SERVER_MECHS, NEGOTIATING ->
                        handleSaslServerResponse(ctx, saslMessage);
                default -> {
                    LOGGER.warn(
                            LoggerUtils.SASL,
                            "Received command '{}' in unexpected state: {}.",
                            saslMessage.getCommandName(),
                            currentState);
                    if (EnumSet.of(SaslState.AUTHENTICATED, SaslState.FAILED)
                            .contains(currentState)) {
                        LOGGER.warn(
                                LoggerUtils.SASL,
                                "Ignoring command '{}' as state is already {}.",
                                saslMessage.getCommandName(),
                                currentState);
                    } else {
                        failAuthentication(
                                ctx,
                                "Unexpected command '"
                                        + saslMessage.getCommandName()
                                        + "' in state "
                                        + currentState);
                    }
                }
            }
        } else {
            LOGGER.warn(
                    LoggerUtils.SASL,
                    "Received unexpected non-SASL message type during SASL: {}",
                    msg.getClass().getName());
            if (currentState == SaslState.AUTHENTICATED) {
                LOGGER.debug(
                        LoggerUtils.SASL,
                        "Passing non-string message up, assuming post-SASL and pre-DBus-pipeline data.");
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
                LOGGER.info(LoggerUtils.SASL, "Server send OK. Sending BEGIN.");
                LOGGER.debug(LoggerUtils.SASL, "Server GUID: {}", args);
                SaslMessage beginMsg = new SaslMessage(SaslCommandName.BEGIN, null);
                ctx.writeAndFlush(beginMsg)
                        .addListener(
                                new WriteOperationListener<>(
                                        LOGGER,
                                        future -> {
                                            if (future.isSuccess()) {
                                                LOGGER.debug(
                                                        LoggerUtils.SASL,
                                                        "BEGIN command sent successfully.");
                                                currentState = SaslState.AUTHENTICATED;
                                                cleanupAndSignalCompletion(ctx, true);
                                            } else {
                                                LOGGER.error(
                                                        LoggerUtils.SASL,
                                                        "Failed to send BEGIN: {}",
                                                        future.cause().getMessage());
                                                failAuthentication(
                                                        ctx,
                                                        "Failed to send BEGIN: "
                                                                + future.cause().getMessage());
                                            }
                                        }));
            }
            case REJECTED -> {
                serverSupportedMechanisms = Arrays.asList(args.split(" "));
                LOGGER.warn(
                        LoggerUtils.SASL,
                        "Mechanism rejected. Server supports: {}",
                        serverSupportedMechanisms);
                disposeCurrentMechanism();
                tryNextMechanism(ctx);
            }
            case DATA -> {
                if (currentMechanism == null || currentState != SaslState.NEGOTIATING) {
                    failAuthentication(
                            ctx, "Unexpected DATA command without mechanism or wrong state.");
                    return;
                }
                currentMechanism
                        .processChallengeAsync(ctx, args)
                        .addListener(
                                future -> {
                                    if (!ctx.channel().isActive()) {
                                        return;
                                    }
                                    if (future.isSuccess()) {
                                        var responseHex = (String) future.getNow();
                                        if (responseHex != null) {
                                            SaslMessage dataMsg =
                                                    new SaslMessage(
                                                            SaslCommandName.DATA, responseHex);
                                            ctx.writeAndFlush(dataMsg)
                                                    .addListener(
                                                            new WriteOperationListener<>(LOGGER));
                                        } else {
                                            LOGGER.debug(
                                                    LoggerUtils.SASL,
                                                    "Mechanism {} complete, awaiting server response.",
                                                    currentMechanism.getName());
                                        }
                                    } else {
                                        LOGGER.error(
                                                LoggerUtils.SASL,
                                                "Failed to process challenge with mechanism {}",
                                                currentMechanism.getName(),
                                                future.cause());
                                        SaslMessage cancelMsg =
                                                new SaslMessage(SaslCommandName.CANCEL, null);
                                        ctx.writeAndFlush(cancelMsg)
                                                .addListener(new WriteOperationListener<>(LOGGER));
                                    }
                                });
            }
            case ERROR -> {
                LOGGER.error(LoggerUtils.SASL, "Server send ERROR: {}", args);
                SaslMessage cancelMsg = new SaslMessage(SaslCommandName.CANCEL, null);
                ctx.writeAndFlush(cancelMsg).addListener(new WriteOperationListener<>(LOGGER));
            }
            case AGREE_UNIX_FD ->
                    LOGGER.info(LoggerUtils.SASL, "Server agreed to UNIX FD passing.");
            default -> {
                if (currentState == SaslState.AWAITING_SERVER_MECHS
                        && command.name().matches("[A-Z0-9_]+([-A-Z0-9_]*[A-Z0-9_]+)?")) {
                    serverSupportedMechanisms = Arrays.asList(msg.toString().split(" "));
                    LOGGER.debug(
                            LoggerUtils.SASL, "Server mechanisms: {}", serverSupportedMechanisms);
                    currentMechanismAttemptIndex = 0;
                    tryNextMechanism(ctx);
                } else {
                    failAuthentication(
                            ctx, "Unexpected command: " + command + " with args: " + args);
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
                LOGGER.info(LoggerUtils.SASL, "Trying mechanism: {}", candidate.getName());
                try {
                    currentMechanism.init(ctx);
                    currentMechanism
                            .getInitialResponseAsync(ctx)
                            .addListener(
                                    future -> {
                                        if (!ctx.channel().isActive()) {
                                            return;
                                        }
                                        if (future.isSuccess()) {
                                            String initialResponse = (String) future.getNow();
                                            String value =
                                                    initialResponse != null
                                                                    && !initialResponse.isEmpty()
                                                            ? initialResponse
                                                            : null;
                                            String commandArgs = candidate.getName();
                                            if (value != null) {
                                                commandArgs += " " + value;
                                            }
                                            SaslMessage authMsg =
                                                    new SaslMessage(
                                                            SaslCommandName.AUTH, commandArgs);
                                            ctx.writeAndFlush(authMsg)
                                                    .addListener(
                                                            new WriteOperationListener<>(LOGGER));
                                            currentState = SaslState.NEGOTIATING;
                                        } else {
                                            LOGGER.error(
                                                    LoggerUtils.SASL,
                                                    "Failed to get initial response for {}: {}",
                                                    candidate.getName(),
                                                    future.cause().getMessage());
                                            tryNextMechanism(ctx);
                                        }
                                    });
                } catch (SaslMechanismException e) {
                    LOGGER.warn(
                            LoggerUtils.SASL,
                            "Initialization failed for {}: {}",
                            candidate.getName(),
                            e.getMessage());
                    tryNextMechanism(ctx);
                }
            }
        } else {
            failAuthentication(ctx, "No compatible SASL mechanism found.");
        }
    }

    private void failAuthentication(ChannelHandlerContext ctx, String reason) {
        if (currentState == SaslState.FAILED) {
            return;
        }
        LOGGER.error(LoggerUtils.SASL, "Authentication failed: {}", reason);
        currentState = SaslState.FAILED;
        if (currentMechanism != null) {
            SaslMessage responseMsg = new SaslMessage(SaslCommandName.CANCEL, null);
            ctx.writeAndFlush(responseMsg)
                    .addListener(
                            new WriteOperationListener<>(
                                    LOGGER, f -> cleanupAndSignalCompletion(ctx, false)));
        } else {
            cleanupAndSignalCompletion(ctx, false);
        }
    }

    private void cleanupAndSignalCompletion(ChannelHandlerContext ctx, boolean success) {
        disposeCurrentMechanism();

        // Fire the event to notify other handlers before removing self
        if (success) {
            LOGGER.info(LoggerUtils.SASL, "Authentication completed successfully.");
            ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        } else {
            LOGGER.error(LoggerUtils.SASL, "Authentication failed.");
            ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_FAILED);
        }

        // Remove this handler from the pipeline as SASL phase is complete
        ctx.pipeline().remove(this);
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Removed SASL authentication handler from pipeline as SASL phase is complete.");
    }

    private void disposeCurrentMechanism() {
        if (currentMechanism != null) {
            try {
                currentMechanism.dispose();
            } catch (Exception e) {
                LOGGER.warn(
                        LoggerUtils.SASL,
                        "Error disposing mechanism {}: {}",
                        currentMechanism.getName(),
                        e.getMessage());
            }
            currentMechanism = null;
        }
    }

    /**
     * Resets the SASL handler to its initial state for reconnection. This method is called when the
     * connection needs to be re-established.
     */
    public void reset() {
        LOGGER.debug(LoggerUtils.SASL, "Resetting SASL handler for reconnection");

        // Reset state
        currentState = SaslState.IDLE;
        currentMechanismAttemptIndex = 0;
        serverSupportedMechanisms = null;

        // Dispose current mechanism
        disposeCurrentMechanism();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.warn(
                LoggerUtils.SASL,
                "Channel became inactive during authentication. Current state: {}",
                currentState);

        if (!EnumSet.of(SaslState.AUTHENTICATED, SaslState.FAILED).contains(currentState)) {
            failAuthentication(ctx, "Channel became inactive.");
        }
        disposeCurrentMechanism();

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (currentState == SaslState.FAILED) {
            LOGGER.debug(
                    LoggerUtils.SASL,
                    "Ignoring exception as SASL already in FAILED state: ",
                    cause);
            return;
        }
        LOGGER.error(
                LoggerUtils.SASL,
                "Exception in SaslAuthenticationHandler. State: {}",
                currentState,
                cause);
        failAuthentication(ctx, "Exception caught: " + cause.getMessage());
    }

    private enum SaslState {
        IDLE,
        AWAITING_SERVER_MECHS,
        NEGOTIATING,
        AWAITING_BEGIN_CONFIRMATION,
        AUTHENTICATED,
        FAILED
    }
}
