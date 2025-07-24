/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder which encodes an object path to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusObjectPath
 */
public final class ObjectPathEncoder implements Encoder<DBusObjectPath, ByteBuffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectPathEncoder.class);

    private static final int NUL_TERMINATOR_LENGTH = 1;
    private final ByteOrder order;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param order The byte order of the produced bytes.
     */
    public ObjectPathEncoder(ByteOrder order) {
        this.order = Objects.requireNonNull(order, "order must not be null");
    }

    @Override
    public EncoderResult<ByteBuffer> encode(DBusObjectPath value, int offset)
            throws EncoderException {
        Objects.requireNonNull(value, "value must not be null");
        try {
            // Calculate padding
            int padding =
                    EncoderUtils.calculateAlignmentPadding(Type.OBJECT_PATH.getAlignment(), offset);

            // UTF-8 bytes of the object path
            byte[] bytes = value.getDelegate().getBytes(StandardCharsets.UTF_8);
            DBusUInt32 length = DBusUInt32.valueOf(bytes.length);

            // Encode length using UInt32Encoder
            Encoder<DBusUInt32, ByteBuffer> lengthEncoder = new UInt32Encoder(order);
            EncoderResult<ByteBuffer> lengthResult = lengthEncoder.encode(length, offset + padding);
            ByteBuffer lengthBuffer = lengthResult.getBuffer();

            // Calculate total size and allocate final buffer
            int totalSize =
                    padding
                            + lengthResult.getProducedBytes()
                            + bytes.length
                            + NUL_TERMINATOR_LENGTH;
            ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);

            // Apply padding
            for (int i = 0; i < padding; i++) {
                buffer.put((byte) 0);
            }

            // Write encoded components
            buffer.put(lengthBuffer);
            buffer.put(bytes);
            buffer.put((byte) 0); // NUL terminator
            buffer.flip();

            EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "OBJECT_PATH: {}; Offset: {}; Padding: {}; Produced bytes: {};",
                    value,
                    offset,
                    padding,
                    totalSize);

            return result;
        } catch (Exception ex) {
            throw new EncoderException("Could not encode OBJECT_PATH.", ex);
        }
    }
}
