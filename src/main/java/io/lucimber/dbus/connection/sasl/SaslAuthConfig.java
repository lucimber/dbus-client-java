package io.lucimber.dbus.connection.sasl;

/**
 * This interface declares the common properties of the different SASL configuration types.
 * A concrete SASL configuration must implement this interface.
 */
public interface SaslAuthConfig {
    /**
     * Gets the authentication mechanism.
     *
     * @return the mechanism as a {@link SaslAuthMechanism}
     */
    SaslAuthMechanism getAuthMechanism();
}
