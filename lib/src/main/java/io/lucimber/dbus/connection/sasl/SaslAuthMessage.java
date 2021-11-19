package io.lucimber.dbus.connection.sasl;

import java.util.Optional;

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
