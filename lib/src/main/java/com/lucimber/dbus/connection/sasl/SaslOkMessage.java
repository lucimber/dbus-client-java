/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@code OK} command is sent by the server to the client.
 *
 * <p>The {@code OK} command indicates that the client has been authenticated.
 * The client may now proceed with negotiating Unix file descriptor passing.
 * To do that it shall send {@code NEGOTIATE_UNIX_FD} to the server.
 *
 * <p>Otherwise, the client must respond to the {@code OK} command by sending a {@code BEGIN} command,
 * followed by its stream of messages, or by disconnecting.
 * The server must not accept additional commands using this protocol
 * after the {@code BEGIN} command has been received.
 * Further communication will be a stream of D-Bus messages (optionally encrypted, as negotiated)
 * rather than this protocol.
 *
 * <p>If there is no negotiation, the first octet received by the client after the \r\n of
 * the {@code OK} command must be the first octet of the authenticated/encrypted stream of D-Bus messages.
 * If the client negotiates Unix file descriptor passing,
 * the first octet received by the client after the \r\n of the {@code AGREE_UNIX_FD}
 * or {@code ERROR} reply must be the first octet of the authenticated/encrypted stream.
 *
 * <p>The {@code OK} command has one argument, which is the GUID of the server.
 * See the section called “Server Addresses” for more on server GUIDs.
 */
public final class SaslOkMessage implements SaslMessage {

  private final String commandValue;

  public SaslOkMessage(final String commandValue) {
    this.commandValue = Objects.requireNonNull(commandValue);
  }

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.SERVER_OK;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.of(commandValue);
  }

  @Override
  public String toString() {
    return "SaslOkMessage{commandValue='" + commandValue + "'}";
  }
}
