/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code BEGIN} command is sent by the client to the server. The server does not reply.
 *
 * <p>The {@code BEGIN} command acknowledges that the client has received an {@code OK} command from the server
 * and completed any feature negotiation that it wishes to do,
 * and declares that the stream of messages is about to begin.
 *
 * <p>The first octet received by the server after the \r\n of the {@code BEGIN} command from the client
 * must be the first octet of the authenticated/encrypted stream of D-Bus messages.
 *
 * <p>Unlike all other commands, the server does not reply to the {@code BEGIN} command with
 * an authentication command of its own. After the \r\n of the reply to the command before {@code BEGIN},
 * the next octet received by the client must be the first octet of the authenticated/encrypted stream
 * of D-Bus messages.
 */
public final class SaslBeginMessage implements SaslMessage {

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.CLIENT_BEGIN;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "SaslBeginMessage{}";
  }
}
