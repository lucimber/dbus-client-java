/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageBatcherTest {

    private MessageBatcher batcher;
    private ChannelHandlerContext mockContext;
    private EventLoop mockEventLoop;
    private ScheduledFuture<?> mockFuture;

    @BeforeEach
    void setUp() {
        batcher = new MessageBatcher(4, 1024, Duration.ofMillis(10));
        mockContext = mock(ChannelHandlerContext.class);
        mockEventLoop = mock(EventLoop.class);
        mockFuture = mock(ScheduledFuture.class);

        when(mockContext.executor()).thenReturn(mockEventLoop);
        when(mockEventLoop.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn((ScheduledFuture) mockFuture);
    }

    @Test
    void testAddMessageBelowThreshold() {
        OutboundMessage message = createTestMessage();

        boolean batched = batcher.addMessage(mockContext, message, 100);

        assertTrue(batched);
        verify(mockEventLoop).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testLargeMessageBypassesBatching() {
        OutboundMessage message = createTestMessage();

        // Message too large for batching
        boolean batched = batcher.addMessage(mockContext, message, 600);

        assertFalse(batched);
        verify(mockEventLoop, never())
                .schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testBatchSizeLimit() {
        List<OutboundMessage> messages = new ArrayList<>();

        // Add messages up to batch size limit (4)
        for (int i = 0; i < 4; i++) {
            OutboundMessage msg = createTestMessage();
            messages.add(msg);
            batcher.addMessage(mockContext, msg, 100);
        }

        // 5th message should trigger flush
        OutboundMessage msg5 = createTestMessage();
        batcher.addMessage(mockContext, msg5, 100);

        // Should have flushed the first batch
        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);
        assertEquals(1, flushed.size()); // Only the 5th message in new batch
    }

    @Test
    void testBatchBytesLimit() {
        // Add messages that exceed byte limit
        batcher.addMessage(mockContext, createTestMessage(), 500);
        batcher.addMessage(mockContext, createTestMessage(), 500);

        // This should trigger flush due to byte limit
        batcher.addMessage(mockContext, createTestMessage(), 500);

        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);
        assertEquals(1, flushed.size()); // Only the last message in new batch
    }

    @Test
    void testFlushEmptyBatch() {
        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);
        assertTrue(flushed.isEmpty());
    }

    @Test
    void testFlushWithMessages() {
        batcher.addMessage(mockContext, createTestMessage(), 100);
        batcher.addMessage(mockContext, createTestMessage(), 100);

        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);
        assertEquals(2, flushed.size());

        // Verify scheduled flush was cancelled
        verify(mockFuture).cancel(false);
    }

    @Test
    void testMetrics() {
        // Add some messages
        for (int i = 0; i < 5; i++) {
            batcher.addMessage(mockContext, createTestMessage(), 100);
        }

        // Flush batch (might have already been flushed due to size limit)
        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);

        String metrics = batcher.getMetrics();
        assertTrue(metrics.contains("Messages processed: 5"));
        // Could be 1 or 2 batches depending on auto-flush
        assertTrue(metrics.contains("Batches sent:"));
        assertTrue(metrics.contains("Bytes processed:"));
    }

    @Test
    void testResetMetrics() {
        batcher.addMessage(mockContext, createTestMessage(), 100);
        batcher.resetMetrics();

        String metrics = batcher.getMetrics();
        assertTrue(metrics.contains("Messages processed: 0"));
        assertTrue(metrics.contains("Batches sent: 0"));
    }

    @Test
    void testShutdown() {
        batcher.addMessage(mockContext, createTestMessage(), 100);
        batcher.shutdown();

        // Verify scheduled flush was cancelled
        verify(mockFuture).cancel(false);

        // Batch should be empty after shutdown
        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);
        assertTrue(flushed.isEmpty());
    }

    @Test
    void testConcurrentFlush() throws InterruptedException {
        // Add messages from multiple threads
        CompletableFuture<Void> future1 =
                CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < 10; i++) {
                                batcher.addMessage(mockContext, createTestMessage(), 50);
                            }
                        });

        CompletableFuture<Void> future2 =
                CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < 10; i++) {
                                batcher.addMessage(mockContext, createTestMessage(), 50);
                            }
                        });

        CompletableFuture.allOf(future1, future2).join();

        // Flush remaining messages
        List<OutboundMessage> flushed = batcher.flushBatch(mockContext);
        assertNotNull(flushed);

        String metrics = batcher.getMetrics();
        assertTrue(metrics.contains("Messages processed: 20"));
    }

    @Test
    void testAdaptiveBatching() {
        // Test with custom batcher that has shorter adaptive check interval
        MessageBatcher adaptiveBatcher = new MessageBatcher(8, 2048, Duration.ofMillis(5));

        // Simulate high message rate
        for (int i = 0; i < 100; i++) {
            adaptiveBatcher.addMessage(mockContext, createTestMessage(), 50);
        }

        String metrics = adaptiveBatcher.getMetrics();
        assertTrue(metrics.contains("Current batch target:"));
        // Target should have adapted based on message rate
    }

    private OutboundMessage createTestMessage() {
        return OutboundMethodCall.Builder.create()
                .withPath(DBusObjectPath.valueOf("/test"))
                .withMember(DBusString.valueOf("TestMethod"))
                .withSerial(DBusUInt32.valueOf(1))
                .build();
    }
}
