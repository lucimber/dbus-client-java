package io.lucimber.dbus.connection.sasl;

import java.util.Objects;

public final class SaslExternalAuthConfig implements SaslAuthConfig {

    private final String identity;

    public SaslExternalAuthConfig(final String identity) {
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public SaslAuthMechanism getAuthMechanism() {
        return SaslAuthMechanism.EXTERNAL;
    }

    public String getIdentity() {
        return identity;
    }
}
