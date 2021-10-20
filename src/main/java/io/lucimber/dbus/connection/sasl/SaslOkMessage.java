package io.lucimber.dbus.connection.sasl;

import java.util.Objects;
import java.util.Optional;

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
