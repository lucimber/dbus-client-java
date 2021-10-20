package io.lucimber.dbus.connection.sasl;

import java.util.Optional;

/**
 * This interface declares the common properties of the different SASL message types.
 * A concrete SASL message must implement this interface.
 */
public interface SaslMessage {
    /**
     * Gets the name of the SASL command.
     *
     * @return the name as a {@link String}
     */
    SaslCommandName getCommandName();

    /**
     * Gets the value of the SASL command.
     *
     * @return the value as an {@link Optional} of {@link String}
     */
    Optional<String> getCommandValue();
}
