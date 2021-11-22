package com.lucimber.dbus.connection.sasl;

import java.util.Optional;

public final class SaslAgreeMessage implements SaslMessage {

    @Override
    public SaslCommandName getCommandName() {
        return SaslCommandName.SERVER_AGREE;
    }

    @Override
    public Optional<String> getCommandValue() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "SaslAgreeMessage{}";
    }
}
