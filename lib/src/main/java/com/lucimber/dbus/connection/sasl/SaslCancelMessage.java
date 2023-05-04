/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code CANCEL} command is sent by the client to the server. The server replies with {@code REJECTED}.
 *
 * <p>At any time up to sending the {@code BEGIN} command, the client may send a {@code CANCEL} command.
 * On receiving the {@code CANCEL} command, the server must send a {@code REJECTED} command
 * and abort the current authentication exchange.
 */
public final class SaslCancelMessage implements SaslMessage {

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.CLIENT_CANCEL;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "SaslCancelMessage{}";
  }
}
