/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals an integer from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusInt32
 */
public final class Int32Decoder implements Decoder<ByteBuffer, DBusInt32> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Int32Decoder.class);
    private static final int TYPE_BYTES = 4;

    @Override
    public DecoderResult<DBusInt32> decode(ByteBuffer buffer, int offset) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            int consumedBytes = 0;

            int padding = DecoderUtils.skipPadding(buffer, offset, Type.INT32);
            consumedBytes += padding;

            int rawValue = buffer.getInt();
            consumedBytes += TYPE_BYTES;

            DBusInt32 value = DBusInt32.valueOf(rawValue);
            DecoderResult<DBusInt32> result = new DecoderResultImpl<>(consumedBytes, value);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "INT32: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
                    value,
                    offset,
                    padding,
                    consumedBytes);

            return result;
        } catch (Throwable t) {
            throw new DecoderException("Could not decode INT32.", t);
        }
    }
}
