package io.lucimber.dbus.connection.sasl;

import java.util.Objects;
import java.util.Optional;

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
