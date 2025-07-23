/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class DBusMandatoryNameHandlerTest {

    private EmbeddedChannel channel;
    private static final DBusString SENDER = DBusString.valueOf("org.freedesktop.DBus");

    @BeforeEach
    void setUp() {
        MDC.clear();
        DBusMandatoryNameHandler handler = new DBusMandatoryNameHandler();
        channel = new EmbeddedChannel();
        channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(new AtomicLong(1));
        channel.pipeline().addLast(handler);
    }

    @Test
    void testHelloCallSentOnPipelineReady() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);

        Object outbound = channel.readOutbound();
        assertInstanceOf(com.lucimber.dbus.message.OutboundMethodCall.class, outbound);

        OutboundMethodCall helloCall = (OutboundMethodCall) outbound;
        assertEquals(DBusObjectPath.valueOf("/org/freedesktop/DBus"), helloCall.getObjectPath());
        assertEquals(DBusString.valueOf("Hello"), helloCall.getMember());
    }

    @Test
    void testHelloReplyTriggersNameAcquiredEvent() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();
        DBusUInt32 sentSerial = sent.getSerial();

        DBusString name = DBusString.valueOf(":1.101");
        InboundMethodReturn reply =
                InboundMethodReturn.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sentSerial)
                        .withSender(SENDER)
                        .withBody(DBusSignature.valueOf("s"), List.of(name))
                        .build();

        channel.writeInbound(reply);

        assertEquals(name, channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
        // Handler should be removed after completion
        assertTrue(
                channel.pipeline().toMap().values().stream()
                        .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
    }

    @Test
    void testHelloReplyWithNoPayloadTriggersFailure() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();
        DBusUInt32 sentSerial = sent.getSerial();

        InboundMethodReturn reply =
                InboundMethodReturn.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sentSerial)
                        .withSender(SENDER)
                        .build();

        channel.writeInbound(reply);

        // No bus name assigned, handler removed after failure
        assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
        assertTrue(
                channel.pipeline().toMap().values().stream()
                        .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
    }

    @Test
    void testHelloErrorTriggersFailure() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();
        DBusUInt32 sentSerial = sent.getSerial();

        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        InboundError error =
                InboundError.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sentSerial)
                        .withSender(SENDER)
                        .withErrorName(DBusString.valueOf("org.freedesktop.DBus.Error.Failed"))
                        .build();

        channel.writeInbound(error);

        assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED));
    }

    @Test
    void testChannelInactiveDuringAwaitingState() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        channel.finish(); // triggers channelInactive

        // Handler should be gone when channel becomes inactive and pipeline is torn down
        assertTrue(
                channel.pipeline().toMap().values().stream()
                        .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
    }

    @Test
    void testReconnectionStartingResetsHandler() {
        // Start the hello process
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        channel.readOutbound(); // consume hello call

        // Trigger reconnection event
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);

        // Should be able to start hello process again
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();
        assertNotNull(helloCall);
        assertEquals(DBusString.valueOf("Hello"), helloCall.getMember());
    }

    @Test
    void testIgnoresSaslAuthCompleteWhenNotIdle() {
        // First SASL auth complete
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall firstCall = channel.readOutbound();
        assertNotNull(firstCall);

        // Second SASL auth complete should be ignored (handler is now awaiting reply)
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall secondCall = channel.readOutbound();
        assertNull(secondCall); // No second call should be sent
    }

    @Test
    void testPassesThroughUnrelatedEvents() {
        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        // Fire unrelated events
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_FAILED);

        // Events should be passed through
        assertTrue(userEvents.contains(DBusChannelEvent.SASL_NUL_BYTE_SENT));
        assertTrue(userEvents.contains(DBusChannelEvent.SASL_AUTH_FAILED));
    }

    @Test
    void testPassesThroughUnrelatedMessages() {
        // Start hello process
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();

        // Create unrelated method return with different serial
        InboundMethodReturn unrelatedReply =
                InboundMethodReturn.Builder.create()
                        .withSerial(DBusUInt32.valueOf(999))
                        .withReplySerial(
                                DBusUInt32.valueOf(888)) // Different from hello call serial
                        .withSender(SENDER)
                        .withBody(
                                DBusSignature.valueOf("s"),
                                List.of(DBusString.valueOf("unrelated")))
                        .build();

        List<Object> receivedMessages = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                receivedMessages.add(msg);
                            }
                        });

        // Send unrelated message
        channel.writeInbound(unrelatedReply);

        // Message should be passed through
        assertEquals(1, receivedMessages.size());
        assertEquals(unrelatedReply, receivedMessages.get(0));
    }

    @Test
    void testSerialNumberGeneration() {
        AtomicLong counter = new AtomicLong(42);
        channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(counter);

        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();

        assertEquals(DBusUInt32.valueOf(42), helloCall.getSerial());
        assertEquals(43, counter.get()); // Should be incremented
    }

    @Test
    void testHelloCallParameters() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();

        assertNotNull(helloCall);
        assertEquals(DBusObjectPath.valueOf("/org/freedesktop/DBus"), helloCall.getObjectPath());
        assertEquals(DBusString.valueOf("Hello"), helloCall.getMember());
        assertEquals(DBusString.valueOf("org.freedesktop.DBus"), helloCall.getDestination().get());
        assertEquals(
                DBusString.valueOf("org.freedesktop.DBus"), helloCall.getInterfaceName().get());
        assertTrue(helloCall.isReplyExpected());
        assertTrue(helloCall.getPayload().isEmpty());
    }

    @Test
    void testExceptionCaughtInIdleState() {
        Exception testException = new RuntimeException("Test exception");

        // Should not cause issues when handler is idle
        assertDoesNotThrow(
                () -> {
                    channel.pipeline().fireExceptionCaught(testException);
                });
    }

    @Test
    void testExceptionCaughtWhileAwaitingReply() {
        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        // Start hello process
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        channel.readOutbound(); // consume hello call

        Exception testException = new RuntimeException("Test exception");
        channel.pipeline().fireExceptionCaught(testException);

        // Should trigger failure event and remove handler
        assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED));
        assertTrue(
                channel.pipeline().toMap().values().stream()
                        .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
    }

    @Test
    void testInvalidHelloReplyPayloadType() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();
        DBusUInt32 sentSerial = sent.getSerial();

        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        // Reply with wrong payload type (uint32 instead of string)
        InboundMethodReturn reply =
                InboundMethodReturn.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sentSerial)
                        .withSender(SENDER)
                        .withBody(DBusSignature.valueOf("u"), List.of(DBusUInt32.valueOf(123)))
                        .build();

        channel.writeInbound(reply);

        // Should trigger failure
        assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED));
        assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    }

    @Test
    void testHelloErrorWithPayload() {
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();
        DBusUInt32 sentSerial = sent.getSerial();

        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        // Error with payload
        InboundError error =
                InboundError.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sentSerial)
                        .withSender(SENDER)
                        .withErrorName(
                                DBusString.valueOf("org.freedesktop.DBus.Error.AccessDenied"))
                        .withBody(
                                DBusSignature.valueOf("s"),
                                List.of(DBusString.valueOf("Access denied")))
                        .build();

        channel.writeInbound(error);

        assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED));
        assertTrue(
                channel.pipeline().toMap().values().stream()
                        .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
    }

    @Test
    void testChannelInactiveInIdleState() {
        // Trigger channel inactive when handler is idle
        assertDoesNotThrow(
                () -> {
                    channel.close();
                });
    }

    @Test
    void testMultipleReconnectionEvents() {
        // Start hello process
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        channel.readOutbound();

        // Multiple reconnection events should work
        for (int i = 0; i < 3; i++) {
            channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
            channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
            OutboundMethodCall helloCall = channel.readOutbound();
            assertNotNull(helloCall);
        }
    }

    @Test
    void testSerialNumberWrapAround() {
        // Test serial number near 32-bit limit
        long largeSerialValue = Integer.MAX_VALUE + 100L;
        AtomicLong counter = new AtomicLong(largeSerialValue);
        channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(counter);

        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();

        // Should wrap around due to (int) cast
        int expectedSerial = (int) largeSerialValue;
        assertEquals(DBusUInt32.valueOf(expectedSerial), helloCall.getSerial());
    }

    @Test
    void testUnrelatedErrorPassedThrough() {
        // Start hello process
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall helloCall = channel.readOutbound();

        // Create unrelated error with different serial
        InboundError unrelatedError =
                InboundError.Builder.create()
                        .withSerial(DBusUInt32.valueOf(999))
                        .withReplySerial(
                                DBusUInt32.valueOf(888)) // Different from hello call serial
                        .withSender(SENDER)
                        .withErrorName(
                                DBusString.valueOf("org.freedesktop.DBus.Error.UnknownMethod"))
                        .build();

        List<Object> receivedMessages = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                receivedMessages.add(msg);
                            }
                        });

        // Send unrelated error
        channel.writeInbound(unrelatedError);

        // Error should be passed through
        assertEquals(1, receivedMessages.size());
        assertEquals(unrelatedError, receivedMessages.get(0));
    }

    @Test
    void testHelloSuccessEventPropagation() {
        List<Object> userEvents = new ArrayList<>();
        channel.pipeline()
                .addLast(
                        new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                userEvents.add(evt);
                            }
                        });

        // Complete hello process successfully
        channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
        OutboundMethodCall sent = channel.readOutbound();

        DBusString name = DBusString.valueOf(":1.42");
        InboundMethodReturn reply =
                InboundMethodReturn.Builder.create()
                        .withSerial(DBusUInt32.valueOf(0))
                        .withReplySerial(sent.getSerial())
                        .withSender(SENDER)
                        .withBody(DBusSignature.valueOf("s"), List.of(name))
                        .build();

        channel.writeInbound(reply);

        // Should fire success event
        assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUIRED));
        assertEquals(name, channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    }
}
