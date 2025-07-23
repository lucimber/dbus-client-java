/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.util.LoggerUtils;

/**
 * An encoder which encodes a byte to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusByte
 */
public final class ByteEncoder implements Encoder<DBusByte, ByteBuffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByteEncoder.class);

    private static final int TYPE_SIZE = 1;

    @Override
    public EncoderResult<ByteBuffer> encode(DBusByte value, int offset) throws EncoderException {
        Objects.requireNonNull(value, "value must not be null");

        try {
            // BYTE has 1-byte alignment, so no padding is ever needed.
            ByteBuffer buffer = ByteBuffer.allocate(TYPE_SIZE);
            buffer.put(value.getDelegate());
            buffer.flip();

            EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(TYPE_SIZE, buffer);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "BYTE: {}; Offset: {}; Padding: {}; Produced bytes: {};",
                    value,
                    offset,
                    0,
                    result.getProducedBytes());

            return result;
        } catch (Exception ex) {
            throw new EncoderException("Could not encode BYTE.", ex);
        }
    }
}
