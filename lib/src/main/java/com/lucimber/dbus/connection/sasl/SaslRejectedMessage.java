package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * The {@code REJECTED} command is sent by the server to the client.
 *
 * <p>The {@code REJECTED} command indicates that the current authentication exchange has failed,
 * and further exchange of {@code DATA} is inappropriate. The client would normally try another mechanism,
 * or try providing different responses to challenges.
 *
 * <p>Optionally, the {@code REJECTED} command has a space-separated list of available
 * auth mechanisms as arguments. If a server ever provides a list of supported mechanisms,
 * it must provide the same list each time it sends a {@code REJECTED} message.
 * Clients are free to ignore all lists received after the first.
 */
public final class SaslRejectedMessage implements SaslMessage {

  private final String commandValue;

  public SaslRejectedMessage() {
    commandValue = null;
  }

  public SaslRejectedMessage(final String commandValue) {
    this.commandValue = commandValue;
  }

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.SERVER_REJECTED;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.ofNullable(commandValue);
  }

  @Override
  public String toString() {
    return "SaslRejectedMessage{commandValue='" + commandValue + "'}";
  }
}
