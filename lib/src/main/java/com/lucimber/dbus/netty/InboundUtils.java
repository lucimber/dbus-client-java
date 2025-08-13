/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.codec.decoder.DecoderException;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageFlag;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.buffer.ByteBuf;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Utility class for common methods used for decoding and encoding messages. */
final class InboundUtils {

    private static final int MAX_MSG_LENGTH = 0x08000000;
    private static final int ZERO = 0x00000000;

    private InboundUtils() {
        // Utility class
    }

    static ByteOrder decodeByteOrder(final ByteBuf buffer) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        final byte B = 0x42;
        final byte l = 0x6C;
        final byte byteOrder = buffer.readByte();
        if (byteOrder == B) {
            return ByteOrder.BIG_ENDIAN;
        } else if (byteOrder == l) {
            return ByteOrder.LITTLE_ENDIAN;
        } else {
            throw new DecoderException("unknown byte order");
        }
    }

    static MessageType decodeType(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        byte ub = buffer.readByte();
        return MessageType.fromCode(ub);
    }

    static Set<MessageFlag> decodeFlags(final ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        final byte flagsByte = buffer.readByte();
        final Set<MessageFlag> flags = new HashSet<>();
        final byte replyFlag = 0x01;
        final byte startFlag = 0x02;
        final byte authFlag = 0x04;
        if ((flagsByte & replyFlag) == replyFlag) {
            flags.add(MessageFlag.NO_REPLY_EXPECTED);
        }
        if ((flagsByte & startFlag) == startFlag) {
            flags.add(MessageFlag.NO_AUTO_START);
        }
        if ((flagsByte & authFlag) == authFlag) {
            flags.add(MessageFlag.ALLOW_INTERACTIVE_AUTHORIZATION);
        }
        return flags;
    }

    static Map<HeaderField, DBusVariant> mapHeaderFields(List<DBusStruct> headerFields)
            throws DecoderException {
        Objects.requireNonNull(headerFields, "headerFields must not be null");
        Map<HeaderField, DBusVariant> map = new HashMap<>();

        for (DBusStruct struct : headerFields) {
            if (struct == null) {
                throw new DecoderException("Header field struct cannot be null");
            }

            List<DBusType> structList = struct.getDelegate();
            if (structList == null || structList.size() < 2) {
                throw new DecoderException("Header field struct must contain at least 2 elements");
            }

            // Validate and extract header field code
            DBusType firstElement = structList.get(0);
            if (!(firstElement instanceof DBusByte)) {
                throw new DecoderException(
                        "Header field code must be a DBusByte, got: "
                                + (firstElement != null
                                        ? firstElement.getClass().getSimpleName()
                                        : "null"));
            }

            DBusByte dbusByte = (DBusByte) firstElement;
            HeaderField headerField = HeaderField.fromCode(dbusByte.getDelegate());
            if (headerField == null) {
                throw new DecoderException("Unknown header field code: " + dbusByte.getDelegate());
            }

            // Validate and extract header field variant
            DBusType secondElement = structList.get(1);
            if (!(secondElement instanceof DBusVariant)) {
                throw new DecoderException(
                        "Header field value must be a DBusVariant, got: "
                                + (secondElement != null
                                        ? secondElement.getClass().getSimpleName()
                                        : "null"));
            }

            DBusVariant variant = (DBusVariant) secondElement;
            map.put(headerField, variant);
        }

        return map;
    }

    static boolean isMessageTooLong(final int headerLength, final int bodyLength) {
        // Validate input parameters to prevent integer overflow attacks
        if (headerLength < 0) {
            throw new IllegalArgumentException("Header length cannot be negative: " + headerLength);
        }
        if (bodyLength < 0) {
            throw new IllegalArgumentException("Body length cannot be negative: " + bodyLength);
        }

        final int signature = 0x0C;
        final int headerSignature = 0x08;
        final int headerAlignment = 0x08;
        final int headerRemainder = Integer.remainderUnsigned(headerLength, headerAlignment);
        final int headerPadding = headerAlignment - headerRemainder;

        // Check for potential integer overflow before performing arithmetic
        int messageLength = signature + headerSignature;
        if (Integer.MAX_VALUE - messageLength < headerLength) {
            return true; // Would overflow, so message is too long
        }
        messageLength += headerLength;

        if (Integer.compareUnsigned(headerRemainder, ZERO) > 0) {
            if (Integer.MAX_VALUE - messageLength < headerPadding) {
                return true; // Would overflow, so message is too long
            }
            messageLength += headerPadding;
        }

        if (Integer.MAX_VALUE - messageLength < bodyLength) {
            return true; // Would overflow, so message is too long
        }
        messageLength += bodyLength;

        return Integer.compareUnsigned(messageLength, MAX_MSG_LENGTH) > 0;
    }
}
