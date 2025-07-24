/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler;
import com.lucimber.dbus.netty.sasl.SaslCodec;
import com.lucimber.dbus.netty.sasl.SaslInitiationHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DBusHandlerConfigurationTest {

    @Mock private RealityCheckpoint mockRealityCheckpoint;

    private EmbeddedChannel channel;
    private Promise<Void> connectPromise;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        channel = new EmbeddedChannel();
        connectPromise = new DefaultPromise<>(channel.eventLoop());
    }

    @Test
    void testInitializePipelineWithAllHandlers() {
        // When: initializing the pipeline
        DBusHandlerConfiguration.initializePipeline(
                channel.pipeline(), connectPromise, mockRealityCheckpoint);

        // Then: all handlers should be added in the correct order
        ChannelPipeline pipeline = channel.pipeline();

        // SASL handlers
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_INITIATION_HANDLER));
        assertTrue(
                pipeline.get(DBusHandlerNames.SASL_INITIATION_HANDLER)
                        instanceof SaslInitiationHandler);

        assertNotNull(pipeline.get(DBusHandlerNames.SASL_CODEC));
        assertTrue(pipeline.get(DBusHandlerNames.SASL_CODEC) instanceof SaslCodec);

        assertNotNull(pipeline.get(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER));
        assertTrue(
                pipeline.get(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER)
                        instanceof SaslAuthenticationHandler);

        // D-Bus protocol handlers
        assertNotNull(pipeline.get(DBusHandlerNames.NETTY_BYTE_LOGGER));
        assertTrue(pipeline.get(DBusHandlerNames.NETTY_BYTE_LOGGER) instanceof LoggingHandler);

        assertNotNull(pipeline.get(DBusHandlerNames.FRAME_ENCODER));
        assertTrue(pipeline.get(DBusHandlerNames.FRAME_ENCODER) instanceof FrameEncoder);

        assertNotNull(pipeline.get(DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER));
        assertTrue(
                pipeline.get(DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER)
                        instanceof OutboundMessageEncoder);

        assertNotNull(pipeline.get(DBusHandlerNames.FRAME_DECODER));
        assertTrue(pipeline.get(DBusHandlerNames.FRAME_DECODER) instanceof FrameDecoder);

        assertNotNull(pipeline.get(DBusHandlerNames.INBOUND_MESSAGE_DECODER));
        assertTrue(
                pipeline.get(DBusHandlerNames.INBOUND_MESSAGE_DECODER)
                        instanceof InboundMessageDecoder);

        assertNotNull(pipeline.get(DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER));
        assertTrue(
                pipeline.get(DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER)
                        instanceof DBusMandatoryNameHandler);

        assertNotNull(pipeline.get(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER));
        assertTrue(
                pipeline.get(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER)
                        instanceof ConnectionCompletionHandler);

        // Reconnection manager
        assertNotNull(pipeline.get("ReconnectionHandlerManager"));
        assertTrue(
                pipeline.get("ReconnectionHandlerManager") instanceof ReconnectionHandlerManager);

        // Application logic handler
        assertNotNull(pipeline.get("RealityCheckpoint"));
        assertSame(mockRealityCheckpoint, pipeline.get("RealityCheckpoint"));
    }

    @Test
    void testInitializePipelineWithoutAppLogicHandler() {
        // When: initializing the pipeline without app logic handler
        DBusHandlerConfiguration.initializePipeline(channel.pipeline(), connectPromise, null);

        // Then: all handlers except RealityCheckpoint should be added
        ChannelPipeline pipeline = channel.pipeline();

        // Verify essential handlers are present
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_INITIATION_HANDLER));
        assertNotNull(pipeline.get(DBusHandlerNames.FRAME_ENCODER));
        assertNotNull(pipeline.get(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER));

        // RealityCheckpoint should not be present
        assertNull(pipeline.get("RealityCheckpoint"));
    }

    @Test
    void testGetSaslHandlers() {
        // When: getting SASL handlers
        Map<String, Supplier<ChannelHandler>> saslHandlers =
                DBusHandlerConfiguration.getSaslHandlers();

        // Then: should return exactly 3 SASL handlers
        assertEquals(3, saslHandlers.size());

        // Verify handler names
        assertTrue(saslHandlers.containsKey(DBusHandlerNames.SASL_INITIATION_HANDLER));
        assertTrue(saslHandlers.containsKey(DBusHandlerNames.SASL_CODEC));
        assertTrue(saslHandlers.containsKey(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER));

        // Verify suppliers create correct handler types
        ChannelHandler initiationHandler =
                saslHandlers.get(DBusHandlerNames.SASL_INITIATION_HANDLER).get();
        assertTrue(initiationHandler instanceof SaslInitiationHandler);

        ChannelHandler codecHandler = saslHandlers.get(DBusHandlerNames.SASL_CODEC).get();
        assertTrue(codecHandler instanceof SaslCodec);

        ChannelHandler authHandler =
                saslHandlers.get(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER).get();
        assertTrue(authHandler instanceof SaslAuthenticationHandler);
    }

    @Test
    void testGetHandlerOrder() {
        // When: getting handler order
        String[] order = DBusHandlerConfiguration.getHandlerOrder();

        // Then: should return handlers in the correct order
        assertNotNull(order);
        assertTrue(order.length > 0);

        // Verify SASL handlers come first
        assertEquals(DBusHandlerNames.SASL_INITIATION_HANDLER, order[0]);
        assertEquals(DBusHandlerNames.SASL_CODEC, order[1]);
        assertEquals(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER, order[2]);

        // Verify D-Bus handlers follow
        List<String> handlerList = List.of(order);
        assertTrue(handlerList.contains(DBusHandlerNames.NETTY_BYTE_LOGGER));
        assertTrue(handlerList.contains(DBusHandlerNames.FRAME_ENCODER));
        assertTrue(handlerList.contains(DBusHandlerNames.FRAME_DECODER));
        assertTrue(handlerList.contains(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER));
    }

    @Test
    void testFindFirstDbusHandler() {
        // Given: a pipeline with handlers
        DBusHandlerConfiguration.initializePipeline(
                channel.pipeline(), connectPromise, mockRealityCheckpoint);

        // When: finding the first D-Bus handler
        String firstDbusHandler = DBusHandlerConfiguration.findFirstDbusHandler(channel.pipeline());

        // Then: should return the byte logger (first non-SASL handler)
        assertEquals(DBusHandlerNames.NETTY_BYTE_LOGGER, firstDbusHandler);
    }

    @Test
    void testFindFirstDbusHandlerWithoutSaslHandlers() {
        // Given: a pipeline without SASL handlers
        channel.pipeline().addLast(DBusHandlerNames.FRAME_ENCODER, new FrameEncoder());
        channel.pipeline().addLast(DBusHandlerNames.FRAME_DECODER, new FrameDecoder());

        // When: finding the first D-Bus handler
        String firstDbusHandler = DBusHandlerConfiguration.findFirstDbusHandler(channel.pipeline());

        // Then: should return the first handler
        assertEquals(DBusHandlerNames.FRAME_ENCODER, firstDbusHandler);
    }

    @Test
    void testFindFirstDbusHandlerEmptyPipeline() {
        // Given: an empty pipeline (except for head and tail)
        // When: finding the first D-Bus handler
        String firstDbusHandler = DBusHandlerConfiguration.findFirstDbusHandler(channel.pipeline());

        // Then: should return null
        assertNull(firstDbusHandler);
    }

    @Test
    void testHandlerOrderConsistency() {
        // When: initializing the pipeline
        DBusHandlerConfiguration.initializePipeline(
                channel.pipeline(), connectPromise, mockRealityCheckpoint);

        // Then: verify essential handlers are present in the correct order
        ChannelPipeline pipeline = channel.pipeline();

        // Verify SASL handlers are present (note: SaslCodec adds SaslMessageDecoder/Encoder)
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_INITIATION_HANDLER));
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_CODEC));
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_MESSAGE_DECODER)); // Added by SaslCodec
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_MESSAGE_ENCODER)); // Added by SaslCodec
        assertNotNull(pipeline.get(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER));

        // Verify D-Bus handlers are present
        assertNotNull(pipeline.get(DBusHandlerNames.NETTY_BYTE_LOGGER));
        assertNotNull(pipeline.get(DBusHandlerNames.FRAME_ENCODER));
        assertNotNull(pipeline.get(DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER));
        assertNotNull(pipeline.get(DBusHandlerNames.FRAME_DECODER));
        assertNotNull(pipeline.get(DBusHandlerNames.INBOUND_MESSAGE_DECODER));
        assertNotNull(pipeline.get(DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER));
        assertNotNull(pipeline.get(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER));

        // Verify the SASL handlers come before D-Bus handlers
        List<String> handlerNames = new ArrayList<>();
        pipeline.forEach(entry -> handlerNames.add(entry.getKey()));

        int saslInitPos = handlerNames.indexOf(DBusHandlerNames.SASL_INITIATION_HANDLER);
        int byteLoggerPos = handlerNames.indexOf(DBusHandlerNames.NETTY_BYTE_LOGGER);
        assertTrue(saslInitPos < byteLoggerPos, "SASL handlers should come before D-Bus handlers");
    }

    @Test
    void testSaslHandlerSuppliers() {
        // When: getting SASL handlers
        Map<String, Supplier<ChannelHandler>> saslHandlers =
                DBusHandlerConfiguration.getSaslHandlers();

        // Then: each supplier should create a new instance
        for (Map.Entry<String, Supplier<ChannelHandler>> entry : saslHandlers.entrySet()) {
            ChannelHandler handler1 = entry.getValue().get();
            ChannelHandler handler2 = entry.getValue().get();

            assertNotNull(handler1);
            assertNotNull(handler2);
            assertNotSame(handler1, handler2, "Supplier should create new instances");
        }
    }
}
