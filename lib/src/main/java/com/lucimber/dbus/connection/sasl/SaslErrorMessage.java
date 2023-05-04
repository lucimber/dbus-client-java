/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code ERROR} command can be sent in either direction.
 * If sent by the client, the server replies with {@code REJECTED}.
 *
 * <p>The {@code ERROR} command indicates that either server or client did not know a command,
 * does not accept the given command in the current context,
 * or did not understand the arguments to the command.
 * This allows the protocol to be extended;
 * a client or server can send a command present
 * or permitted only in new protocol versions,
 * and if an {@code ERROR} is received instead of an appropriate response,
 * fall back to using some other technique.
 *
 * <p>If an {@code ERROR} is sent, the server or client that sent the error must continue
 * as if the command causing the {@code ERROR} had never been received.
 * However, the server or client receiving the error should try something other than
 * whatever caused the error; if only canceling/rejecting the authentication.
 *
 * <p>If the D-Bus protocol changes incompatibly at some future time,
 * applications implementing the new protocol would probably be able to check
 * for support of the new protocol by sending a new command
 * and receiving an {@code ERROR} from applications that don't understand it.
 * Thus the {@code ERROR} feature of the auth protocol is an escape hatch that lets us
 * negotiate extensions or changes to the D-Bus protocol in the future.
 */
public final class SaslErrorMessage implements SaslMessage {

  private final String commandValue;

  public SaslErrorMessage() {
    commandValue = null;
  }

  public SaslErrorMessage(final String commandValue) {
    this.commandValue = commandValue;
  }

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.SHARED_ERROR;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.ofNullable(commandValue);
  }

  @Override
  public String toString() {
    return "SaslErrorMessage{commandValue='" + commandValue + "'}";
  }
}
