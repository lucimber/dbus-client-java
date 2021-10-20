package io.lucimber.dbus.connection.sasl;

import java.util.Optional;

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
