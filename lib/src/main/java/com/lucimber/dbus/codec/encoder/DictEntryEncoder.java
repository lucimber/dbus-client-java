/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusDictEntry;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;

/**
 * An encoder which encodes a key-value pair to the D-Bus marshalling format using ByteBuffer.
 *
 * @param <KeyT> The data type of the key.
 * @param <ValueT> The data type of the value.
 * @see Encoder
 * @see DBusDictEntry
 */
public final class DictEntryEncoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Encoder<DBusDictEntry<KeyT, ValueT>, ByteBuffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DictEntryEncoder.class);

    private final ByteOrder order;
    private final DBusSignature signature;

    /**
     * Constructs a new instance with mandatory parameters.
     *
     * @param order The byte order of the produced bytes.
     * @param signature The signature of the dictionary entry.
     */
    public DictEntryEncoder(ByteOrder order, DBusSignature signature) {
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
    }

    @Override
    public EncoderResult<ByteBuffer> encode(DBusDictEntry<KeyT, ValueT> entry, int offset)
            throws EncoderException {
        Objects.requireNonNull(entry, "entry must not be null");
        try {
            int producedBytes = 0;
            int padding =
                    EncoderUtils.calculateAlignmentPadding(Type.DICT_ENTRY.getAlignment(), offset);
            producedBytes += padding;

            // Encode key
            KeyT key = entry.getKey();
            int keyOffset = offset + producedBytes;
            EncoderResult<ByteBuffer> keyResult = EncoderUtils.encode(key, keyOffset, order);
            final ByteBuffer keyBuffer = keyResult.getBuffer();
            producedBytes += keyResult.getProducedBytes();

            // Encode value
            ValueT value = entry.getValue();
            int valueOffset = offset + producedBytes;
            EncoderResult<ByteBuffer> valueResult = EncoderUtils.encode(value, valueOffset, order);
            final ByteBuffer valueBuffer = valueResult.getBuffer();
            producedBytes += valueResult.getProducedBytes();

            // Assemble final buffer
            ByteBuffer buffer = ByteBuffer.allocate(producedBytes).order(order);
            for (int i = 0; i < padding; i++) {
                buffer.put((byte) 0);
            }
            buffer.put(keyBuffer);
            buffer.put(valueBuffer);
            buffer.flip();

            EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(producedBytes, buffer);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "DICT_ENTRY: {}; Offset: {}; Padding: {}; Produced bytes: {};",
                    signature,
                    offset,
                    padding,
                    result.getProducedBytes());

            return result;
        } catch (Exception ex) {
            throw new EncoderException("Could not encode DICT_ENTRY.", ex);
        }
    }
}
