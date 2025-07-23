/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.lucimber.dbus.codec.encoder.ArrayEncoder;
import com.lucimber.dbus.codec.encoder.Encoder;
import com.lucimber.dbus.codec.encoder.EncoderResult;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrameDecoderTest {

    private static final byte BIG_ENDIAN_MAGIC = 0x42;
    private static final int HEADER_BOUNDARY = 8;
    private static final byte LITTLE_ENDIAN_MAGIC = 0x6C;

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void succeedWithError(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(BIG_ENDIAN_MAGIC);
        } else {
            buffer.writeByte(LITTLE_ENDIAN_MAGIC);
        }
        final byte msgTypeError = 0x03;
        buffer.writeByte(msgTypeError);
        final byte msgFlag = 0x00;
        buffer.writeByte(msgFlag);
        final byte version = 0x01;
        buffer.writeByte(version);
        // Body length
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        // Serial number
        final int serial = 2;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(serial);
        } else {
            buffer.writeIntLE(serial);
        }
        // Header fields
        final DBusSignature signature = DBusSignature.valueOf("a(yv)");
        final DBusArray<DBusStruct> array = new DBusArray<>(signature);
        final DBusSignature structSignature = signature.subContainer();
        final DBusString errorName = DBusString.valueOf("io.lucimber.Error.UnitTest");
        final DBusVariant errorNameVariant = DBusVariant.valueOf(errorName);
        final DBusStruct errorStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 4), errorNameVariant);
        array.add(errorStruct);
        final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
        final DBusStruct replySerialStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 5), replySerialVariant);
        array.add(replySerialStruct);
        final Encoder<DBusArray<DBusStruct>, ByteBuffer> headerEncoder =
                new ArrayEncoder<>(byteOrder, signature);
        final EncoderResult<ByteBuffer> headerResult = headerEncoder.encode(array, 12);
        buffer.writeBytes(headerResult.getBuffer());
        addAlignmentPaddingAfterHeaderIfNecessary(buffer);
        final FrameDecoder inboundHandler = new FrameDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
        assertTrue(channel.writeInbound(buffer));
        final Frame frame = channel.readInbound();
        assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
        assertTrue(frame.getFlags().isEmpty(), "Message flags");
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
        assertEquals(MessageType.ERROR, frame.getType(), "Message type");
        assertEquals(0, frame.getBody().remaining(), "Frame body");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void succeedWithMethodCall(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(BIG_ENDIAN_MAGIC);
        } else {
            buffer.writeByte(LITTLE_ENDIAN_MAGIC);
        }
        final byte msgTypeMethodCall = 0x01;
        buffer.writeByte(msgTypeMethodCall);
        final byte msgFlag = 0x00;
        buffer.writeByte(msgFlag);
        final byte version = 0x01;
        buffer.writeByte(version);
        // Body length
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        // Serial number
        final int serial = 1;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(serial);
        } else {
            buffer.writeIntLE(serial);
        }
        // Header fields
        final DBusSignature signature = DBusSignature.valueOf("a(yv)");
        final DBusArray<DBusStruct> structList = new DBusArray<>(signature);
        final DBusSignature structSignature = signature.subContainer();
        final DBusString methodName = DBusString.valueOf("MethodCall");
        final DBusVariant memberVariant = DBusVariant.valueOf(methodName);
        final DBusStruct memberStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 3), memberVariant);
        structList.add(memberStruct);
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
        final DBusVariant interfaceVariant = DBusVariant.valueOf(interfaceName);
        final DBusStruct interfaceStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 2), interfaceVariant);
        structList.add(interfaceStruct);
        final DBusObjectPath path = DBusObjectPath.valueOf("/");
        final DBusVariant pathVariant = DBusVariant.valueOf(path);
        final DBusStruct pathStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 1), pathVariant);
        structList.add(pathStruct);
        final Encoder<DBusArray<DBusStruct>, ByteBuffer> headerEncoder =
                new ArrayEncoder<>(byteOrder, signature);
        final EncoderResult<ByteBuffer> headerResult = headerEncoder.encode(structList, 12);
        buffer.writeBytes(headerResult.getBuffer());
        addAlignmentPaddingAfterHeaderIfNecessary(buffer);
        final FrameDecoder inboundHandler = new FrameDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
        assertTrue(channel.writeInbound(buffer));
        final Frame frame = channel.readInbound();
        assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
        assertTrue(frame.getFlags().isEmpty(), "Message flags");
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
        assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
        assertEquals(0, frame.getBody().remaining(), "Frame body");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void succeedWithMethodReturnMessage(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(BIG_ENDIAN_MAGIC);
        } else {
            buffer.writeByte(LITTLE_ENDIAN_MAGIC);
        }
        final byte msgTypeMethodReturn = 0x02;
        buffer.writeByte(msgTypeMethodReturn);
        final byte msgFlag = 0x00;
        buffer.writeByte(msgFlag);
        final byte version = 0x01;
        buffer.writeByte(version);
        // Body length
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        // Serial number
        final int serial = 2;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(serial);
        } else {
            buffer.writeIntLE(serial);
        }
        // Header fields
        final DBusSignature signature = DBusSignature.valueOf("a(yv)");
        final DBusArray<DBusStruct> array = new DBusArray<>(signature);
        final DBusSignature structSignature = signature.subContainer();
        final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
        final DBusStruct replySerialStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 5), replySerialVariant);
        array.add(replySerialStruct);
        final Encoder<DBusArray<DBusStruct>, ByteBuffer> headerEncoder =
                new ArrayEncoder<>(byteOrder, signature);
        final EncoderResult<ByteBuffer> headerResult = headerEncoder.encode(array, 12);
        buffer.writeBytes(headerResult.getBuffer());
        addAlignmentPaddingAfterHeaderIfNecessary(buffer);
        final FrameDecoder inboundHandler = new FrameDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
        assertTrue(channel.writeInbound(buffer));
        final Frame frame = channel.readInbound();
        assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
        assertTrue(frame.getFlags().isEmpty(), "Message flags");
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
        assertEquals(MessageType.METHOD_RETURN, frame.getType(), "Message type");
        assertEquals(0, frame.getBody().remaining(), "Frame body");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void succeedWithSignalMessage(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(BIG_ENDIAN_MAGIC);
        } else {
            buffer.writeByte(LITTLE_ENDIAN_MAGIC);
        }
        final byte msgTypeSignal = 0x04;
        buffer.writeByte(msgTypeSignal);
        final byte msgFlag = 0x00;
        buffer.writeByte(msgFlag);
        final byte version = 0x01;
        buffer.writeByte(version);
        // Body length
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        // Serial number
        final int serial = 2;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(serial);
        } else {
            buffer.writeIntLE(serial);
        }
        // Header fields
        final DBusSignature signature = DBusSignature.valueOf("a(yv)");
        final DBusArray<DBusStruct> array = new DBusArray<>(signature);
        final DBusSignature structSignature = signature.subContainer();
        final DBusString signalName = DBusString.valueOf("TestSignal");
        final DBusVariant memberVariant = DBusVariant.valueOf(signalName);
        final DBusStruct memberStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 3), memberVariant);
        array.add(memberStruct);
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
        final DBusVariant interfaceVariant = DBusVariant.valueOf(interfaceName);
        final DBusStruct interfaceStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 2), interfaceVariant);
        array.add(interfaceStruct);
        final DBusObjectPath path = DBusObjectPath.valueOf("/");
        final DBusVariant pathVariant = DBusVariant.valueOf(path);
        final DBusStruct pathStruct =
                new DBusStruct(structSignature, DBusByte.valueOf((byte) 1), pathVariant);
        array.add(pathStruct);
        final Encoder<DBusArray<DBusStruct>, ByteBuffer> headerEncoder =
                new ArrayEncoder<>(byteOrder, signature);
        final EncoderResult<ByteBuffer> headerResult = headerEncoder.encode(array, 12);
        buffer.writeBytes(headerResult.getBuffer());
        addAlignmentPaddingAfterHeaderIfNecessary(buffer);
        final FrameDecoder inboundHandler = new FrameDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
        assertTrue(channel.writeInbound(buffer));
        final Frame frame = channel.readInbound();
        assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
        assertTrue(frame.getFlags().isEmpty(), "Message flags");
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
        assertEquals(MessageType.SIGNAL, frame.getType(), "Message type");
        assertEquals(0, frame.getBody().remaining(), "Frame body");
    }

    private void addAlignmentPaddingAfterHeaderIfNecessary(final ByteBuf frame) {
        final int remainder = frame.readableBytes() % HEADER_BOUNDARY;
        if (remainder > 0) {
            final int padding = HEADER_BOUNDARY - remainder;
            frame.writeZero(padding);
        }
    }
}
