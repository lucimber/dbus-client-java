/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import java.util.NoSuchElementException;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lucimber.dbus.netty.DBusChannelEvent;
import com.lucimber.dbus.netty.DBusHandlerNames;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SaslCodecTest {

    @Mock private ChannelHandlerContext context;

    @Mock private ChannelPipeline pipeline;

    private SaslCodec codec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        codec = new SaslCodec();

        when(context.pipeline()).thenReturn(pipeline);
        when(context.name()).thenReturn("SaslCodec");
    }

    @Test
    void testHandlerAdded() {
        codec.handlerAdded(context);

        // Verify decoder and encoder are added
        verify(pipeline)
                .addBefore(
                        eq("SaslCodec"),
                        eq(DBusHandlerNames.SASL_MESSAGE_DECODER),
                        any(SaslMessageDecoder.class));
        verify(pipeline)
                .addBefore(
                        eq("SaslCodec"),
                        eq(DBusHandlerNames.SASL_MESSAGE_ENCODER),
                        any(SaslMessageEncoder.class));
    }

    @Test
    void testHandlerAddedWithException() {
        // Setup pipeline to throw exception
        when(pipeline.addBefore(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Pipeline error"));

        assertThrows(RuntimeException.class, () -> codec.handlerAdded(context));
    }

    @Test
    void testHandlerRemoved() {
        codec.handlerRemoved(context);

        // Verify decoder and encoder are removed
        verify(pipeline).remove(DBusHandlerNames.SASL_MESSAGE_DECODER);
        verify(pipeline).remove(DBusHandlerNames.SASL_MESSAGE_ENCODER);
    }

    @Test
    void testHandlerRemovedWithNoSuchElementException() {
        // Setup pipeline to throw NoSuchElementException when removing decoder
        doThrow(new NoSuchElementException("Handler not found"))
                .when(pipeline)
                .remove(DBusHandlerNames.SASL_MESSAGE_DECODER);

        // Should not throw exception, just log warning
        assertDoesNotThrow(() -> codec.handlerRemoved(context));

        // Verify decoder removal was attempted
        verify(pipeline).remove(DBusHandlerNames.SASL_MESSAGE_DECODER);
    }

    @Test
    void testHandlerRemovedWithBothHandlersMissing() {
        // Setup pipeline to throw NoSuchElementException for both handlers
        doThrow(new NoSuchElementException("Decoder not found"))
                .when(pipeline)
                .remove(DBusHandlerNames.SASL_MESSAGE_DECODER);
        doThrow(new NoSuchElementException("Encoder not found"))
                .when(pipeline)
                .remove(DBusHandlerNames.SASL_MESSAGE_ENCODER);

        // Should not throw exception, just log warnings
        assertDoesNotThrow(() -> codec.handlerRemoved(context));
    }

    @Test
    void testUserEventTriggeredWithSaslAuthComplete() {
        codec.userEventTriggered(context, DBusChannelEvent.SASL_AUTH_COMPLETE);

        // Verify codec removes itself from pipeline
        verify(pipeline).remove(codec);

        // Verify event is propagated
        verify(context).fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    }

    @Test
    void testUserEventTriggeredWithOtherEvent() {
        Object customEvent = new Object();
        codec.userEventTriggered(context, customEvent);

        // Verify codec does NOT remove itself
        verify(pipeline, never()).remove(any(ChannelHandler.class));

        // Verify event is propagated
        verify(context).fireUserEventTriggered(customEvent);
    }

    @Test
    void testUserEventTriggeredWithMultipleEvents() {
        // Test various events
        DBusChannelEvent[] events = {
            DBusChannelEvent.SASL_NUL_BYTE_SENT,
            DBusChannelEvent.SASL_AUTH_FAILED,
            DBusChannelEvent.RECONNECTION_STARTING
        };

        for (DBusChannelEvent event : events) {
            codec.userEventTriggered(context, event);

            // Verify event is propagated but codec is not removed
            verify(context).fireUserEventTriggered(event);
            verify(pipeline, never()).remove(any(ChannelHandler.class));
        }
    }

    @Test
    void testHandlerAddedAndRemovedLifecycle() {
        // Add handler
        codec.handlerAdded(context);

        // Verify handlers were added
        verify(pipeline)
                .addBefore(
                        eq("SaslCodec"),
                        eq(DBusHandlerNames.SASL_MESSAGE_DECODER),
                        any(SaslMessageDecoder.class));
        verify(pipeline)
                .addBefore(
                        eq("SaslCodec"),
                        eq(DBusHandlerNames.SASL_MESSAGE_ENCODER),
                        any(SaslMessageEncoder.class));

        // Remove handler
        codec.handlerRemoved(context);

        // Verify handlers were removed
        verify(pipeline).remove(DBusHandlerNames.SASL_MESSAGE_DECODER);
        verify(pipeline).remove(DBusHandlerNames.SASL_MESSAGE_ENCODER);
    }

    @Test
    void testHandlerAddedWithNullContextName() {
        when(context.name()).thenReturn(null);

        codec.handlerAdded(context);

        // Should still add handlers, using null as the before name
        verify(pipeline)
                .addBefore(
                        isNull(),
                        eq(DBusHandlerNames.SASL_MESSAGE_DECODER),
                        any(SaslMessageDecoder.class));
        verify(pipeline)
                .addBefore(
                        isNull(),
                        eq(DBusHandlerNames.SASL_MESSAGE_ENCODER),
                        any(SaslMessageEncoder.class));
    }
}
