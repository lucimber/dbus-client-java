/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder which encodes a signature to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusSignature
 */
public final class SignatureEncoder implements Encoder<DBusSignature, ByteBuffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureEncoder.class);

    private static final int NUL_TERMINATOR_LENGTH = 1;

    @Override
    public EncoderResult<ByteBuffer> encode(DBusSignature signature, int offset)
            throws EncoderException {
        Objects.requireNonNull(signature, "signature must not be null");

        try {
            String value = signature.toString();
            byte[] signatureBytes = value.getBytes(StandardCharsets.UTF_8);
            int length = signatureBytes.length;

            // Encode the length as a single byte
            DBusByte size = DBusByte.valueOf((byte) length);
            Encoder<DBusByte, ByteBuffer> byteEncoder = new ByteEncoder();
            EncoderResult<ByteBuffer> lengthResult = byteEncoder.encode(size, offset);
            ByteBuffer lengthBuffer = lengthResult.getBuffer();

            // Allocate the complete buffer
            int totalSize = lengthResult.getProducedBytes() + length + NUL_TERMINATOR_LENGTH;
            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            buffer.put(lengthBuffer);
            buffer.put(signatureBytes);
            buffer.put((byte) 0); // NUL terminator
            buffer.flip();

            EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "SIGNATURE: {}; Offset: {}; Padding: {}; Produced bytes: {};",
                    signature,
                    offset,
                    0,
                    totalSize);

            return result;
        } catch (Exception ex) {
            throw new EncoderException("Could not encode SIGNATURE.", ex);
        }
    }
}
