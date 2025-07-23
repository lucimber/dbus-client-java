/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;

/**
 * A decoder which unmarshals an unsigned integer from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusUInt32
 */
public final class UInt32Decoder implements Decoder<ByteBuffer, DBusUInt32> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UInt32Decoder.class);
    private static final int TYPE_BYTES = 4;

    @Override
    public DecoderResult<DBusUInt32> decode(ByteBuffer buffer, int offset) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            int consumedBytes = 0;

            int padding = DecoderUtils.skipPadding(buffer, offset, Type.UINT32);
            consumedBytes += padding;

            int rawValue = buffer.getInt();
            consumedBytes += TYPE_BYTES;

            DBusUInt32 value = DBusUInt32.valueOf(rawValue);
            DecoderResult<DBusUInt32> result = new DecoderResultImpl<>(consumedBytes, value);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "UINT32: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
                    value,
                    offset,
                    padding,
                    consumedBytes);

            return result;
        } catch (Throwable t) {
            throw new DecoderException("Could not decode UINT32.", t);
        }
    }
}
