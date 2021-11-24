package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code AGREE_UNIX_FD} command is sent by the server to the client.
 *
 * <p>The {@code AGREE_UNIX_FD} command indicates that the server supports Unix file descriptor passing.
 * This command may only be sent after the connection is authenticated,
 * and the client sent {@code NEGOTIATE_UNIX_FD} to enable Unix file descriptor passing.
 * This command may only be sent on transports that support Unix file descriptor passing.
 *
 * <p>On receiving {@code AGREE_UNIX_FD} the client must respond with {@code BEGIN},
 * followed by its stream of messages, or by disconnecting.
 * The server must not accept additional commands using this protocol
 * after the {@code BEGIN} command has been received. Further communication will be a stream of
 * D-Bus messages (optionally encrypted, as negotiated) rather than this protocol.
 */
public final class SaslAgreeMessage implements SaslMessage {

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.SERVER_AGREE;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "SaslAgreeMessage{}";
  }
}
