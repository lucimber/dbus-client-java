package io.lucimber.dbus.connection.sasl;

import java.util.Optional;

public final class SaslBeginMessage implements SaslMessage {

    @Override
    public SaslCommandName getCommandName() {
        return SaslCommandName.CLIENT_BEGIN;
    }

    @Override
    public Optional<String> getCommandValue() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "SaslBeginMessage{}";
    }
}
