package io.lucimber.dbus.connection.sasl;

public final class SaslAnonymousAuthConfig implements SaslAuthConfig {
    @Override
    public SaslAuthMechanism getAuthMechanism() {
        return SaslAuthMechanism.ANONYMOUS;
    }

    @Override
    public String toString() {
        return "SaslAnonymousAuthConfig{}";
    }
}
