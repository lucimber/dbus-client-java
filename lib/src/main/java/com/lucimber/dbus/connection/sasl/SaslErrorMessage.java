package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

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
