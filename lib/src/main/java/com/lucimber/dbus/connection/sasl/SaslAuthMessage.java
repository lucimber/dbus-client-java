/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code AUTH} command is sent by the client to the server.
 * The server replies with {@code DATA}, {@code OK} or {@code REJECTED}.
 *
 * <p>If an {@code AUTH} command has no arguments, it is a request to list available mechanisms.
 * The server must respond with a {@code REJECTED} command listing the mechanisms it understands, or with an error.
 *
 * <p>If an {@code AUTH} command specifies a mechanism, and the server supports said mechanism,
 * the server should begin exchanging SASL challenge-response data with the client using {@code DATA} commands.
 *
 * <p>If the server does not support the mechanism given in the {@code AUTH} command,
 * it must send either a {@code REJECTED} command listing the mechanisms it does support, or an error.
 *
 * <p>If the [initial-response] argument is provided, it is intended for use with mechanisms
 * that have no initial challenge (or an empty initial challenge),
 * as if it were the argument to an initial {@code DATA} command.
 * If the selected mechanism has an initial challenge and [initial-response] was provided,
 * the server should reject authentication by sending {@code REJECTED}.
 *
 * <p>If authentication succeeds after exchanging {@code DATA} commands,
 * an {@code OK} command must be sent to the client.
 */
public final class SaslAuthMessage implements SaslMessage {

  private final String commandValue;

  public SaslAuthMessage() {
    commandValue = null;
  }

  public SaslAuthMessage(final String commandValue) {
    this.commandValue = commandValue;
  }

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.CLIENT_AUTH;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.ofNullable(commandValue);
  }

  @Override
  public String toString() {
    return "SaslAuthMessage{commandValue='" + commandValue + "'}";
  }
}
