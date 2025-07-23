/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a boolean from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusBoolean
 */
public final class BooleanDecoder implements Decoder<ByteBuffer, DBusBoolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BooleanDecoder.class);

    @Override
    public DecoderResult<DBusBoolean> decode(ByteBuffer buffer, int offset)
            throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");

        try {
            int consumedBytes = 0;

            // Skip padding based on alignment
            int padding = DecoderUtils.skipPadding(buffer, offset, Type.BOOLEAN);
            consumedBytes += padding;

            // Decode the UInt32 value that encodes the boolean
            UInt32Decoder decoder = new UInt32Decoder();
            DecoderResult<DBusUInt32> rawResult = decoder.decode(buffer, offset + padding);
            consumedBytes += rawResult.getConsumedBytes();

            int value = rawResult.getValue().getDelegate();
            DBusBoolean decodedValue;
            if (Integer.compareUnsigned(value, 0) == 0) {
                decodedValue = DBusBoolean.valueOf(false);
            } else if (Integer.compareUnsigned(value, 1) == 0) {
                decodedValue = DBusBoolean.valueOf(true);
            } else {
                throw new Exception("Marshalled UINT32 is not a valid BOOLEAN value.");
            }

            DecoderResult<DBusBoolean> result =
                    new DecoderResultImpl<>(consumedBytes, decodedValue);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "BOOLEAN: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
                    decodedValue,
                    offset,
                    padding,
                    consumedBytes);

            return result;
        } catch (Throwable t) {
            throw new DecoderException("Could not decode BOOLEAN.", t);
        }
    }
}
