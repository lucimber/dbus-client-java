/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class NettyConnectionHandleTest {

    @Mock
    private Channel channel;

    @Mock
    private EventLoopGroup eventLoopGroup;

    @Mock
    private ConnectionConfig config;

    @Mock
    private RealityCheckpoint realityCheckpoint;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private Attribute<DBusString> busNameAttribute;

    @Mock
    private Attribute<AtomicLong> serialAttribute;

    private NettyConnectionHandle handle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(config.getCloseTimeout()).thenReturn(Duration.ofSeconds(5));
        when(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME)).thenReturn(busNameAttribute);
        when(channel.attr(DBusChannelAttribute.SERIAL_COUNTER)).thenReturn(serialAttribute);
        
        handle = new NettyConnectionHandle(channel, eventLoopGroup, config, realityCheckpoint);
    }

    @Test
    void testConstructor() {
        assertNotNull(handle);
    }

    @Test
    void testIsActiveWhenChannelIsActive() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        assertTrue(handle.isActive());
    }

    @Test
    void testIsActiveWhenChannelIsInactive() {
        when(channel.isActive()).thenReturn(false);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        assertFalse(handle.isActive());
    }

    @Test
    void testIsActiveWhenChannelIsNull() {
        handle = new NettyConnectionHandle(null, eventLoopGroup, config, realityCheckpoint);
        
        assertFalse(handle.isActive());
    }

    @Test
    void testIsActiveWhenBusNameIsNull() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(null);
        
        assertFalse(handle.isActive());
    }

    @Test
    void testIsActiveWhenClosing() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        // Start close operation
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        handle.close();
        
        // Should not be active anymore even if channel is still active
        assertFalse(handle.isActive());
    }

    @Test
    void testSendSuccessful() throws Exception {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        CompletionStage<Void> result = handle.send(message);
        
        // Simulate successful write
        verify(channelFuture).addListener(any());
        when(channelFuture.isSuccess()).thenReturn(true);
        
        // Trigger the listener
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
    }

    @Test
    void testSendFailure() throws Exception {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        CompletionStage<Void> result = handle.send(message);
        
        // Simulate failed write
        RuntimeException testException = new RuntimeException("Write failed");
        when(channelFuture.isSuccess()).thenReturn(false);
        when(channelFuture.cause()).thenReturn(testException);
        
        // Trigger the listener
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
    }

    @Test
    void testSendWhenNotActive() {
        when(channel.isActive()).thenReturn(false);
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        CompletionStage<Void> result = handle.send(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Connection is not active", ex.getCause().getMessage());
    }

    @Test
    void testSendRequestSuccessful() throws Exception {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        // Mock RealityCheckpoint behavior
        Future<InboundMessage> replyFuture = ImmediateEventExecutor.INSTANCE.newSucceededFuture(
                InboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(2))
                        .withSender(DBusString.valueOf("test.sender"))
                        .withObjectPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .withReplyExpected(false)
                        .build()
        );
        
        Future<Future<InboundMessage>> writeFuture = ImmediateEventExecutor.INSTANCE.newSucceededFuture(replyFuture);
        when(realityCheckpoint.writeMessage(message)).thenReturn(writeFuture);
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
        
        InboundMessage response = result.toCompletableFuture().get();
        assertNotNull(response);
    }

    @Test
    void testSendRequestWhenNotActive() {
        when(channel.isActive()).thenReturn(false);
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Connection is not active", ex.getCause().getMessage());
    }

    @Test
    void testSendRequestWithNullRealityCheckpoint() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        handle = new NettyConnectionHandle(channel, eventLoopGroup, config, null);
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("RealityCheckpoint not available", ex.getCause().getMessage());
    }

    @Test
    void testSendRequestWriteFailure() throws Exception {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        RuntimeException testException = new RuntimeException("Write failed");
        Future<Future<InboundMessage>> writeFuture = ImmediateEventExecutor.INSTANCE.newFailedFuture(testException);
        when(realityCheckpoint.writeMessage(message)).thenReturn(writeFuture);
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
    }

    @Test
    void testSendRequestReplyFailure() throws Exception {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        RuntimeException testException = new RuntimeException("Reply failed");
        Future<InboundMessage> replyFuture = ImmediateEventExecutor.INSTANCE.newFailedFuture(testException);
        Future<Future<InboundMessage>> writeFuture = ImmediateEventExecutor.INSTANCE.newSucceededFuture(replyFuture);
        when(realityCheckpoint.writeMessage(message)).thenReturn(writeFuture);
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
    }

    @Test
    void testSendRequestException() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        OutboundMessage message = OutboundMethodCall.Builder.create()
                .withSerial(DBusUInt32.valueOf(1))
                .withPath(DBusObjectPath.valueOf("/test"))
                .withInterface(DBusString.valueOf("test.Interface"))
                .withMember(DBusString.valueOf("TestMethod"))
                .build();
        
        RuntimeException testException = new RuntimeException("Unexpected error");
        when(realityCheckpoint.writeMessage(message)).thenThrow(testException);
        
        CompletionStage<InboundMessage> result = handle.sendRequest(message);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
    }

    @Test
    void testGetNextSerial() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        
        AtomicLong counter = new AtomicLong(42);
        when(serialAttribute.get()).thenReturn(counter);
        
        DBusUInt32 serial = handle.getNextSerial();
        
        assertNotNull(serial);
        assertEquals(42, serial.getDelegate());
        assertEquals(43, counter.get());
    }

    @Test
    void testGetNextSerialWhenNotActive() {
        when(channel.isActive()).thenReturn(false);
        
        assertThrows(IllegalStateException.class, () -> handle.getNextSerial());
    }

    @Test
    void testGetNextSerialWithNullCounter() {
        when(channel.isActive()).thenReturn(true);
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.name"));
        when(serialAttribute.get()).thenReturn(null);
        
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> handle.getNextSerial());
        assertEquals("Serial counter not initialized on channel", ex.getMessage());
    }

    @Test
    void testCloseSuccessful() throws Exception {
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        when(eventLoopGroup.isShuttingDown()).thenReturn(false);
        
        DefaultEventExecutor executor = new DefaultEventExecutor();
        when(eventLoopGroup.shutdownGracefully(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(executor.newSucceededFuture(null));
        
        CompletionStage<Void> result = handle.close();
        
        // Simulate successful channel close
        when(channelFuture.isSuccess()).thenReturn(true);
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        // Wait for async completion
        Thread.sleep(100);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
        
        executor.shutdownGracefully();
    }

    @Test
    void testCloseWithChannelFailure() throws Exception {
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        when(eventLoopGroup.isShuttingDown()).thenReturn(false);
        
        DefaultEventExecutor executor = new DefaultEventExecutor();
        when(eventLoopGroup.shutdownGracefully(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(executor.newSucceededFuture(null));
        
        CompletionStage<Void> result = handle.close();
        
        // Simulate channel close failure
        RuntimeException testException = new RuntimeException("Channel close failed");
        when(channelFuture.isSuccess()).thenReturn(false);
        when(channelFuture.cause()).thenReturn(testException);
        
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        // Wait for async completion
        Thread.sleep(100);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
        
        executor.shutdownGracefully();
    }

    @Test
    void testCloseWithEventLoopGroupFailure() throws Exception {
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        when(eventLoopGroup.isShuttingDown()).thenReturn(false);
        
        RuntimeException testException = new RuntimeException("EventLoopGroup shutdown failed");
        DefaultEventExecutor executor = new DefaultEventExecutor();
        when(eventLoopGroup.shutdownGracefully(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(executor.newFailedFuture(testException));
        
        CompletionStage<Void> result = handle.close();
        
        // Simulate successful channel close
        when(channelFuture.isSuccess()).thenReturn(true);
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        // Wait for async completion
        Thread.sleep(100);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
        
        executor.shutdownGracefully();
    }

    @Test
    void testCloseWithAlreadyShuttingDownEventLoopGroup() throws Exception {
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        when(eventLoopGroup.isShuttingDown()).thenReturn(true);
        
        CompletionStage<Void> result = handle.close();
        
        // Simulate successful channel close
        when(channelFuture.isSuccess()).thenReturn(true);
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
        
        verify(eventLoopGroup, never()).shutdownGracefully(anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testCloseWithNullChannel() {
        handle = new NettyConnectionHandle(null, eventLoopGroup, config, realityCheckpoint);
        
        CompletionStage<Void> result = handle.close();
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
    }

    @Test
    void testCloseWithNullEventLoopGroup() throws Exception {
        handle = new NettyConnectionHandle(channel, null, config, realityCheckpoint);
        
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        
        CompletionStage<Void> result = handle.close();
        
        // Simulate successful channel close
        when(channelFuture.isSuccess()).thenReturn(true);
        ArgumentCaptor<GenericFutureListener<ChannelFuture>> captor = 
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(channelFuture).addListener(captor.capture());
        GenericFutureListener<ChannelFuture> listener = captor.getValue();
        listener.operationComplete(channelFuture);
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertFalse(result.toCompletableFuture().isCompletedExceptionally());
    }

    @Test
    void testConcurrentClose() {
        when(channel.close()).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        
        CompletionStage<Void> result1 = handle.close();
        CompletionStage<Void> result2 = handle.close();
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result2.toCompletableFuture().isDone());
        assertFalse(result2.toCompletableFuture().isCompletedExceptionally());
        
        verify(channel, times(1)).close();
    }

    @Test
    void testCloseException() {
        RuntimeException testException = new RuntimeException("Close exception");
        when(channel.close()).thenThrow(testException);
        
        CompletionStage<Void> result = handle.close();
        
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
        assertTrue(result.toCompletableFuture().isCompletedExceptionally());
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
                () -> result.toCompletableFuture().get());
        assertEquals(testException, ex.getCause());
    }

    @Test
    void testGetAssignedBusName() {
        when(busNameAttribute.get()).thenReturn(DBusString.valueOf("test.bus.name"));
        
        String busName = handle.getAssignedBusName();
        
        assertEquals("test.bus.name", busName);
    }

    @Test
    void testGetAssignedBusNameWithNullAttribute() {
        when(busNameAttribute.get()).thenReturn(null);
        
        String busName = handle.getAssignedBusName();
        
        assertNull(busName);
    }

    @Test
    void testGetAssignedBusNameWithNullChannel() {
        handle = new NettyConnectionHandle(null, eventLoopGroup, config, realityCheckpoint);
        
        String busName = handle.getAssignedBusName();
        
        assertNull(busName);
    }
}