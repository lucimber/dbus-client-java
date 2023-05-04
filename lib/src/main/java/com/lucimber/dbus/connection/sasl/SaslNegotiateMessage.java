/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code NEGOTIATE_UNIX_FD} command is sent by the client to the server.
 * The server replies with {@code AGREE_UNIX_FD} or {@code ERROR}.
 *
 * <p>The {@code NEGOTIATE_UNIX_FD} command indicates that the client supports Unix file descriptor passing.
 * This command may only be sent after the connection is authenticated,
 * i.e. after {@code OK} was received by the client.
 * This command may only be sent on transports that support Unix file descriptor passing.
 *
 * <p>On receiving {@code NEGOTIATE_UNIX_FD} the server must respond with
 * either {@code AGREE_UNIX_FD} or {@code ERROR}.
 * It shall respond the former if the transport chosen supports Unix file descriptor passing
 * and the server supports this feature.
 * It shall respond the latter if the transport does not support Unix file descriptor passing,
 * the server does not support this feature, or the server decides not to enable file descriptor passing
 * due to security or other reasons.
 */
public final class SaslNegotiateMessage implements SaslMessage {

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.CLIENT_NEGOTIATE;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "SaslNegotiateMessage{}";
  }
}
