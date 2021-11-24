package com.lucimber.dbus.connection.sasl;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@code DATA} command may come from either client or server,
 * and simply contains a hex-encoded block of data to be
 * interpreted according to the SASL mechanism in use.
 * If sent by the client, the server replies with {@code DATA}, {@code OK} or {@code REJECTED}.
 */
public final class SaslDataMessage implements SaslMessage {

  private final String commandValue;

  public SaslDataMessage(final String commandValue) {
    this.commandValue = Objects.requireNonNull(commandValue);
  }

  @Override
  public SaslCommandName getCommandName() {
    return SaslCommandName.SHARED_DATA;
  }

  @Override
  public Optional<String> getCommandValue() {
    return Optional.of(commandValue);
  }

  @Override
  public String toString() {
    return "SaslDataMessage{commandValue='" + commandValue + "'}";
  }
}
