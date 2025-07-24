/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a variant from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusVariant
 */
public final class VariantDecoder implements Decoder<ByteBuffer, DBusVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantDecoder.class);

    @Override
    public DecoderResult<DBusVariant> decode(ByteBuffer buffer, int offset)
            throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");

        try {
            int consumedBytes = 0;

            // Decode the signature
            Decoder<ByteBuffer, DBusSignature> signatureDecoder = new SignatureDecoder();
            DecoderResult<DBusSignature> sigResult = signatureDecoder.decode(buffer, offset);
            DBusSignature signature = sigResult.getValue();
            consumedBytes += sigResult.getConsumedBytes();

            if (signature.getQuantity() != 1) {
                throw new DecoderException("Signature must be a single complete type.");
            }

            // Decode the value with that signature
            int valueOffset = offset + consumedBytes;
            DecoderResult<? extends DBusType> valueResult =
                    DecoderUtils.decode(signature, buffer, valueOffset);
            consumedBytes += valueResult.getConsumedBytes();

            DBusVariant variant = DBusVariant.valueOf(valueResult.getValue());
            DecoderResult<DBusVariant> result = new DecoderResultImpl<>(consumedBytes, variant);

            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "VARIANT: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
                    variant,
                    offset,
                    0,
                    consumedBytes);

            return result;
        } catch (Exception e) {
            throw new DecoderException("Could not decode VARIANT.", e);
        }
    }
}
