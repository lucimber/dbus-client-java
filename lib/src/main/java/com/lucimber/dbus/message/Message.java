package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;

import java.util.List;
import java.util.Optional;

/**
 * This interface declares the common properties of the different D-Bus message types.
 * A concrete D-Bus message must implement this interface.
 */
public interface Message {

    /**
     * Gets the serial number of this D-Bus message.
     *
     * @return an {@link UInt32}
     */
    UInt32 getSerial();

    /**
     * Sets the serial number of this D-Bus message.
     * Must not be {@code null}.
     *
     * @param serial an {@link UInt32}
     */
    void setSerial(UInt32 serial);

    /**
     * Gets the payload of this D-Bus message.
     *
     * @return a {@link List} of {@link DBusType}s.
     */
    List<DBusType> getPayload();

    /**
     * Sets the payload of this D-Bus message.
     * The payload can only be set after a signature has been set before.
     *
     * @param payload a {@link List} of {@link DBusType}s.
     * @see Signature
     */
    void setPayload(List<? extends DBusType> payload);

    /**
     * Gets the signature of the payload.
     *
     * @return an {@link Optional} of {@link Signature}
     */
    Optional<Signature> getSignature();

    /**
     * Sets the signature of the payload.
     * The payload can only be set after a signature has been set before.
     *
     * @param signature a {@link Signature}
     */
    void setSignature(Signature signature);
}
