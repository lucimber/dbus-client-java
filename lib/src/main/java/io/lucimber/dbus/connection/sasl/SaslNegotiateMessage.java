package io.lucimber.dbus.connection.sasl;

import java.util.Optional;

public final class SaslNegotiateMessage implements SaslMessage {

    @Override
    public SaslCommandName getCommandName() {
        return SaslCommandName.CLIENT_NEGOTIATE;
    }

    @Override
    public Optional<String> getCommandValue() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "SaslNegotiateMessage{}";
    }
}
