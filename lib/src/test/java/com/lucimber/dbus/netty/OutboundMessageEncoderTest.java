/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.EncoderException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

final class OutboundMessageEncoderTest {

    private OutboundMessageEncoder encoder;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        MDC.clear();
        encoder = new OutboundMessageEncoder();
        channel = new EmbeddedChannel(encoder);
    }

    @Test
    void testEncodeMethodCall() {
        DBusString sender = DBusString.valueOf("io.lucimber.test.sender");
        DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        DBusObjectPath path = DBusObjectPath.valueOf("/io/lucimber/test");
        DBusString interfaceName = DBusString.valueOf("io.lucimber.TestInterface");
        DBusString member = DBusString.valueOf("TestMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(123);

        List<DBusType> payload = Arrays.asList(DBusString.valueOf("arg1"), DBusUInt32.valueOf(42));
        DBusSignature signature = DBusSignature.valueOf("su");

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withDestination(destination)
                        .withPath(path)
                        .withInterface(interfaceName)
                        .withMember(member)
                        .withBody(signature, payload)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder());
        assertEquals(MessageType.METHOD_CALL, frame.getType());
        assertEquals(serial, frame.getSerial());
        assertEquals(1, frame.getProtocolVersion());
        assertTrue(frame.getBody().hasRemaining());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(path, headerFields.get(HeaderField.PATH).getDelegate());
        assertEquals(interfaceName, headerFields.get(HeaderField.INTERFACE).getDelegate());
        assertEquals(member, headerFields.get(HeaderField.MEMBER).getDelegate());
        assertEquals(destination, headerFields.get(HeaderField.DESTINATION).getDelegate());
        assertEquals(signature, headerFields.get(HeaderField.SIGNATURE).getDelegate());
    }

    @Test
    void testEncodeMethodCallWithoutInterface() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString member = DBusString.valueOf("TestMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(1);

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.METHOD_CALL, frame.getType());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(path, headerFields.get(HeaderField.PATH).getDelegate());
        assertEquals(member, headerFields.get(HeaderField.MEMBER).getDelegate());
        assertNull(headerFields.get(HeaderField.INTERFACE));
        assertNull(headerFields.get(HeaderField.DESTINATION));
        assertNull(headerFields.get(HeaderField.SIGNATURE));
    }

    @Test
    void testEncodeMethodReturn() {
        DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        DBusUInt32 serial = DBusUInt32.valueOf(456);
        DBusUInt32 replySerial = DBusUInt32.valueOf(123);

        List<DBusType> payload = Arrays.asList(DBusString.valueOf("result"));
        DBusSignature signature = DBusSignature.valueOf("s");

        OutboundMethodReturn methodReturn =
                OutboundMethodReturn.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withDestination(destination)
                        .withBody(signature, payload)
                        .build();

        assertTrue(channel.writeOutbound(methodReturn));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.METHOD_RETURN, frame.getType());
        assertEquals(serial, frame.getSerial());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(replySerial, headerFields.get(HeaderField.REPLY_SERIAL).getDelegate());
        assertEquals(destination, headerFields.get(HeaderField.DESTINATION).getDelegate());
        assertEquals(signature, headerFields.get(HeaderField.SIGNATURE).getDelegate());
    }

    @Test
    void testEncodeMethodReturnWithoutBody() {
        DBusUInt32 serial = DBusUInt32.valueOf(456);
        DBusUInt32 replySerial = DBusUInt32.valueOf(123);

        OutboundMethodReturn methodReturn =
                OutboundMethodReturn.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .build();

        assertTrue(channel.writeOutbound(methodReturn));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.METHOD_RETURN, frame.getType());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(replySerial, headerFields.get(HeaderField.REPLY_SERIAL).getDelegate());
        assertNull(headerFields.get(HeaderField.DESTINATION));
        assertNull(headerFields.get(HeaderField.SIGNATURE));
        assertFalse(frame.getBody().hasRemaining());
    }

    @Test
    void testEncodeError() {
        DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        DBusUInt32 serial = DBusUInt32.valueOf(789);
        DBusUInt32 replySerial = DBusUInt32.valueOf(123);
        DBusString errorName = DBusString.valueOf("io.lucimber.TestError");

        List<DBusType> payload = Arrays.asList(DBusString.valueOf("Error message"));
        DBusSignature signature = DBusSignature.valueOf("s");

        OutboundError error =
                OutboundError.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withDestination(destination)
                        .withErrorName(errorName)
                        .withBody(signature, payload)
                        .build();

        assertTrue(channel.writeOutbound(error));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.ERROR, frame.getType());
        assertEquals(serial, frame.getSerial());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(errorName, headerFields.get(HeaderField.ERROR_NAME).getDelegate());
        assertEquals(replySerial, headerFields.get(HeaderField.REPLY_SERIAL).getDelegate());
        assertEquals(destination, headerFields.get(HeaderField.DESTINATION).getDelegate());
        assertEquals(signature, headerFields.get(HeaderField.SIGNATURE).getDelegate());
    }

    @Test
    void testEncodeErrorWithoutBody() {
        DBusUInt32 serial = DBusUInt32.valueOf(789);
        DBusUInt32 replySerial = DBusUInt32.valueOf(123);
        DBusString errorName = DBusString.valueOf("io.lucimber.TestError");

        OutboundError error =
                OutboundError.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withErrorName(errorName)
                        .build();

        assertTrue(channel.writeOutbound(error));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.ERROR, frame.getType());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(errorName, headerFields.get(HeaderField.ERROR_NAME).getDelegate());
        assertEquals(replySerial, headerFields.get(HeaderField.REPLY_SERIAL).getDelegate());
        assertNull(headerFields.get(HeaderField.DESTINATION));
        assertNull(headerFields.get(HeaderField.SIGNATURE));
    }

    @Test
    void testEncodeSignal() {
        DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        DBusObjectPath path = DBusObjectPath.valueOf("/io/lucimber/test");
        DBusString interfaceName = DBusString.valueOf("io.lucimber.TestInterface");
        DBusString member = DBusString.valueOf("TestSignal");
        DBusUInt32 serial = DBusUInt32.valueOf(321);

        List<DBusType> payload =
                Arrays.asList(DBusString.valueOf("signal_data"), DBusUInt32.valueOf(999));
        DBusSignature signature = DBusSignature.valueOf("su");

        OutboundSignal signal =
                OutboundSignal.Builder.create()
                        .withSerial(serial)
                        .withDestination(destination)
                        .withObjectPath(path)
                        .withInterface(interfaceName)
                        .withMember(member)
                        .withBody(signature, payload)
                        .build();

        assertTrue(channel.writeOutbound(signal));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.SIGNAL, frame.getType());
        assertEquals(serial, frame.getSerial());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(path, headerFields.get(HeaderField.PATH).getDelegate());
        assertEquals(interfaceName, headerFields.get(HeaderField.INTERFACE).getDelegate());
        assertEquals(member, headerFields.get(HeaderField.MEMBER).getDelegate());
        assertEquals(destination, headerFields.get(HeaderField.DESTINATION).getDelegate());
        assertEquals(signature, headerFields.get(HeaderField.SIGNATURE).getDelegate());
    }

    @Test
    void testEncodeSignalWithoutDestination() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString interfaceName = DBusString.valueOf("io.lucimber.TestInterface");
        DBusString member = DBusString.valueOf("TestSignal");
        DBusUInt32 serial = DBusUInt32.valueOf(321);

        OutboundSignal signal =
                OutboundSignal.Builder.create()
                        .withSerial(serial)
                        .withObjectPath(path)
                        .withInterface(interfaceName)
                        .withMember(member)
                        .build();

        assertTrue(channel.writeOutbound(signal));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals(MessageType.SIGNAL, frame.getType());

        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        assertEquals(path, headerFields.get(HeaderField.PATH).getDelegate());
        assertEquals(interfaceName, headerFields.get(HeaderField.INTERFACE).getDelegate());
        assertEquals(member, headerFields.get(HeaderField.MEMBER).getDelegate());
        assertNull(headerFields.get(HeaderField.DESTINATION));
        assertNull(headerFields.get(HeaderField.SIGNATURE));
    }

    @Test
    void testEncodeEmptyBody() {
        DBusUInt32 serial = DBusUInt32.valueOf(1);
        DBusUInt32 replySerial = DBusUInt32.valueOf(2);

        OutboundMethodReturn methodReturn =
                OutboundMethodReturn.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .build();

        assertTrue(channel.writeOutbound(methodReturn));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertFalse(frame.getBody().hasRemaining());
        assertEquals(0, frame.getBody().capacity());
    }

    @Test
    void testValidatePayloadMismatch() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString member = DBusString.valueOf("TestMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(1);

        // Payload has string but signature expects uint32
        List<DBusType> payload = Arrays.asList(DBusString.valueOf("wrong_type"));
        DBusSignature signature = DBusSignature.valueOf("u");

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withBody(signature, payload)
                        .build();

        assertThrows(
                EncoderException.class,
                () -> {
                    channel.writeOutbound(methodCall);
                });
    }

    @Test
    void testValidatePayloadCountMismatch() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString member = DBusString.valueOf("TestMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(1);

        // Payload has 1 item but signature expects 2
        List<DBusType> payload = Arrays.asList(DBusString.valueOf("only_one"));
        DBusSignature signature = DBusSignature.valueOf("su");

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withBody(signature, payload)
                        .build();

        assertThrows(
                EncoderException.class,
                () -> {
                    channel.writeOutbound(methodCall);
                });
    }

    @Test
    void testFrameProperties() {
        DBusUInt32 serial = DBusUInt32.valueOf(100);
        DBusUInt32 replySerial = DBusUInt32.valueOf(50);

        OutboundMethodReturn methodReturn =
                OutboundMethodReturn.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .build();

        assertTrue(channel.writeOutbound(methodReturn));
        Frame frame = channel.readOutbound();

        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder());
        assertEquals(1, frame.getProtocolVersion());
        assertEquals(serial, frame.getSerial());
        assertTrue(frame.getFlags().isEmpty());
    }

    @Test
    void testComplexPayload() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString member = DBusString.valueOf("ComplexMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(1);

        List<DBusType> payload =
                Arrays.asList(
                        DBusString.valueOf("string_arg"),
                        DBusUInt32.valueOf(42),
                        DBusString.valueOf("another_string"));
        DBusSignature signature = DBusSignature.valueOf("sus");

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withBody(signature, payload)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        Frame frame = channel.readOutbound();

        assertNotNull(frame);
        assertTrue(frame.getBody().hasRemaining());
        assertEquals(signature, frame.getHeaderFields().get(HeaderField.SIGNATURE).getDelegate());
    }

    @Test
    void testMethodCallNoReplyFlag() {
        DBusObjectPath path = DBusObjectPath.valueOf("/test");
        DBusString member = DBusString.valueOf("NoReplyMethod");
        DBusUInt32 serial = DBusUInt32.valueOf(1);

        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withReplyExpected(false)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        Frame frame = channel.readOutbound();

        assertEquals(MessageType.METHOD_CALL, frame.getType());
        // Note: Flags are set by FrameEncoder, not OutboundMessageEncoder
        assertTrue(frame.getFlags().isEmpty());
    }

    @Test
    void testAllMessageTypes() {
        // Test that all message types are handled correctly
        List<Class<?>> messageTypes =
                Arrays.asList(
                        OutboundMethodCall.class,
                        OutboundMethodReturn.class,
                        OutboundError.class,
                        OutboundSignal.class);

        for (Class<?> messageType : messageTypes) {
            EmbeddedChannel testChannel = new EmbeddedChannel(new OutboundMessageEncoder());

            if (messageType == OutboundMethodCall.class) {
                OutboundMethodCall msg =
                        OutboundMethodCall.Builder.create()
                                .withSerial(DBusUInt32.valueOf(1))
                                .withPath(DBusObjectPath.valueOf("/test"))
                                .withMember(DBusString.valueOf("test"))
                                .build();
                assertTrue(testChannel.writeOutbound(msg));
                Frame frame = testChannel.readOutbound();
                assertEquals(MessageType.METHOD_CALL, frame.getType());
            } else if (messageType == OutboundMethodReturn.class) {
                OutboundMethodReturn msg =
                        OutboundMethodReturn.Builder.create()
                                .withSerial(DBusUInt32.valueOf(1))
                                .withReplySerial(DBusUInt32.valueOf(2))
                                .build();
                assertTrue(testChannel.writeOutbound(msg));
                Frame frame = testChannel.readOutbound();
                assertEquals(MessageType.METHOD_RETURN, frame.getType());
            } else if (messageType == OutboundError.class) {
                OutboundError msg =
                        OutboundError.Builder.create()
                                .withSerial(DBusUInt32.valueOf(1))
                                .withReplySerial(DBusUInt32.valueOf(2))
                                .withErrorName(DBusString.valueOf("test.error"))
                                .build();
                assertTrue(testChannel.writeOutbound(msg));
                Frame frame = testChannel.readOutbound();
                assertEquals(MessageType.ERROR, frame.getType());
            } else if (messageType == OutboundSignal.class) {
                OutboundSignal msg =
                        OutboundSignal.Builder.create()
                                .withSerial(DBusUInt32.valueOf(1))
                                .withObjectPath(DBusObjectPath.valueOf("/test"))
                                .withInterface(DBusString.valueOf("test.interface"))
                                .withMember(DBusString.valueOf("TestSignal"))
                                .build();
                assertTrue(testChannel.writeOutbound(msg));
                Frame frame = testChannel.readOutbound();
                assertEquals(MessageType.SIGNAL, frame.getType());
            }
        }
    }
}
