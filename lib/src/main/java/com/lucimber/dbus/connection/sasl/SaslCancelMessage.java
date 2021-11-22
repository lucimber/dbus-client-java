package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

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
