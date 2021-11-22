package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.type.DBusType;

/**
 * The DecoderResult class yields the decoded D-Bus data type and the number of decoded bytes.
 * The decoded D-Bus data type is an instance of {@link DBusType}.
 * The number of consumed bytes is necessary for the calculation of the alignment padding.
 *
 * @param <ValueT> data type
 */
public interface DecoderResult<ValueT extends DBusType> {
    /**
     * Gets the number of bytes, that have been consumed by a decoder
     * while producing this result.
     *
     * @return An integer as the number of consumed bytes.
     */
    int getConsumedBytes();

    /**
     * Sets the number of bytes, that have been consumed by a decoder
     * while producing this result.
     *
     * @param consumedBytes an {@link Integer}
     */
    void setConsumedBytes(int consumedBytes);

    /**
     * Gets the decoded value.
     *
     * @return the value.
     */
    ValueT getValue();
}
