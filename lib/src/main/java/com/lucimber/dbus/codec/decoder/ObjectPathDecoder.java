/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals an object path from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusObjectPath
 */
public final class ObjectPathDecoder implements Decoder<ByteBuffer, DBusObjectPath> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectPathDecoder.class);

    @Override
    public DecoderResult<DBusObjectPath> decode(ByteBuffer buffer, int offset)
            throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            int consumedBytes = 0;

            int padding = DecoderUtils.skipPadding(buffer, offset, Type.OBJECT_PATH);
            consumedBytes += padding;

            int stringOffset = offset + consumedBytes;
            Decoder<ByteBuffer, DBusString> stringDecoder = new StringDecoder();
            DecoderResult<DBusString> stringResult = stringDecoder.decode(buffer, stringOffset);
            consumedBytes += stringResult.getConsumedBytes();

            DBusObjectPath path = DBusObjectPath.valueOf(stringResult.getValue().getDelegate());
            DecoderResult<DBusObjectPath> result = new DecoderResultImpl<>(consumedBytes, path);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "OBJECT_PATH: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
                    path,
                    offset,
                    padding,
                    consumedBytes);

            return result;
        } catch (Exception e) {
            throw new DecoderException("Could not decode OBJECT_PATH.", e);
        }
    }
}
