/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import com.lucimber.dbus.message.OutboundMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message batcher for improved throughput by grouping multiple small messages.
 *
 * <p>This batcher collects outbound messages and sends them in batches to reduce system call
 * overhead and improve network utilization. It implements adaptive batching based on message rate
 * and size.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Time-based batching with configurable delay
 *   <li>Size-based batching with configurable limits
 *   <li>Adaptive batch sizing based on throughput
 *   <li>Performance metrics tracking
 * </ul>
 */
public class MessageBatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBatcher.class);

    // Configuration constants
    private static final int DEFAULT_MAX_BATCH_SIZE = 16;
    private static final int DEFAULT_MAX_BATCH_BYTES = 16 * 1024; // 16KB
    private static final long DEFAULT_BATCH_DELAY_MS = 1; // 1ms
    private static final int MIN_BATCH_SIZE = 1;
    private static final int MAX_BATCH_SIZE = 64;

    private final int maxBatchSize;
    private final int maxBatchBytes;
    private final long batchDelayMs;

    // Current batch state
    private final List<OutboundMessage> currentBatch;
    private final AtomicInteger currentBatchBytes;
    private final AtomicBoolean flushScheduled;
    private ScheduledFuture<?> flushFuture;

    // Performance metrics
    private final AtomicLong messagesProcessed;
    private final AtomicLong batchesSent;
    private final AtomicLong bytesProcessed;
    private final AtomicInteger currentBatchSizeTarget;

    // Adaptive batching state
    private long lastAdaptiveCheck;
    private long lastMessageCount;
    private static final long ADAPTIVE_CHECK_INTERVAL_MS = 1000; // 1 second

    /** Creates a new message batcher with default configuration. */
    public MessageBatcher() {
        this(
                DEFAULT_MAX_BATCH_SIZE,
                DEFAULT_MAX_BATCH_BYTES,
                Duration.ofMillis(DEFAULT_BATCH_DELAY_MS));
    }

    /**
     * Creates a new message batcher with custom configuration.
     *
     * @param maxBatchSize maximum messages per batch
     * @param maxBatchBytes maximum bytes per batch
     * @param batchDelay maximum delay before flushing a batch
     */
    public MessageBatcher(int maxBatchSize, int maxBatchBytes, Duration batchDelay) {
        this.maxBatchSize = Math.min(Math.max(maxBatchSize, MIN_BATCH_SIZE), MAX_BATCH_SIZE);
        this.maxBatchBytes = maxBatchBytes;
        this.batchDelayMs = batchDelay.toMillis();

        this.currentBatch = new ArrayList<>(this.maxBatchSize);
        this.currentBatchBytes = new AtomicInteger();
        this.flushScheduled = new AtomicBoolean();

        this.messagesProcessed = new AtomicLong();
        this.batchesSent = new AtomicLong();
        this.bytesProcessed = new AtomicLong();
        this.currentBatchSizeTarget =
                new AtomicInteger(this.maxBatchSize / 2); // Start conservative

        this.lastAdaptiveCheck = System.currentTimeMillis();
        this.lastMessageCount = 0;
    }

    /**
     * Adds a message to the current batch.
     *
     * @param ctx channel handler context
     * @param message the message to batch
     * @param estimatedSize estimated size of the message in bytes
     * @return true if the message was batched, false if it should be sent immediately
     */
    public synchronized boolean addMessage(
            ChannelHandlerContext ctx, OutboundMessage message, int estimatedSize) {
        // Large messages bypass batching
        if (estimatedSize > maxBatchBytes / 2) {
            LOGGER.trace(
                    "Message too large for batching ({}B), sending immediately", estimatedSize);
            return false;
        }

        // Check if adding this message would exceed limits
        boolean wouldExceedSize = currentBatch.size() >= currentBatchSizeTarget.get();
        boolean wouldExceedBytes = currentBatchBytes.get() + estimatedSize > maxBatchBytes;

        if (wouldExceedSize || wouldExceedBytes) {
            // Flush current batch and start new one
            flushBatch(ctx);
        }

        // Add message to batch
        currentBatch.add(message);
        currentBatchBytes.addAndGet(estimatedSize);
        messagesProcessed.incrementAndGet();
        bytesProcessed.addAndGet(estimatedSize);

        // Schedule flush if not already scheduled
        if (!flushScheduled.get() && flushScheduled.compareAndSet(false, true)) {
            flushFuture =
                    ctx.executor()
                            .schedule(
                                    () -> {
                                        synchronized (MessageBatcher.this) {
                                            flushBatch(ctx);
                                        }
                                    },
                                    batchDelayMs,
                                    TimeUnit.MILLISECONDS);
        }

        // Periodically adapt batch size based on throughput
        adaptBatchSize();

        return true;
    }

    /**
     * Flushes the current batch immediately.
     *
     * @param ctx channel handler context
     * @return list of messages to send, or empty list if batch is empty
     */
    public synchronized List<OutboundMessage> flushBatch(ChannelHandlerContext ctx) {
        if (currentBatch.isEmpty()) {
            return new ArrayList<>();
        }

        // Cancel scheduled flush
        if (flushFuture != null) {
            flushFuture.cancel(false);
            flushFuture = null;
        }
        flushScheduled.set(false);

        // Clear current batch first and prepare return value
        final List<OutboundMessage> batch = new ArrayList<>(currentBatch);
        currentBatch.clear();
        int batchBytes = currentBatchBytes.get();
        currentBatchBytes.set(0);

        // Update metrics
        batchesSent.incrementAndGet();

        LOGGER.trace("Flushing batch: {} messages, {} bytes", batch.size(), batchBytes);

        return batch;
    }

    /** Adapts the batch size based on current throughput. */
    private void adaptBatchSize() {
        long now = System.currentTimeMillis();
        if (now - lastAdaptiveCheck < ADAPTIVE_CHECK_INTERVAL_MS) {
            return;
        }

        long currentMessageCount = messagesProcessed.get();
        long messageRate =
                (currentMessageCount - lastMessageCount) * 1000 / ADAPTIVE_CHECK_INTERVAL_MS;

        int currentTarget = currentBatchSizeTarget.get();
        int newTarget = currentTarget;

        // High message rate: increase batch size
        if (messageRate > 1000 && currentTarget < maxBatchSize) {
            newTarget = Math.min(currentTarget * 2, maxBatchSize);
        } else if (messageRate < 100 && currentTarget > MIN_BATCH_SIZE) {
            // Low message rate: decrease batch size for lower latency
            newTarget = Math.max(currentTarget / 2, MIN_BATCH_SIZE);
        }

        if (newTarget != currentTarget) {
            currentBatchSizeTarget.set(newTarget);
            LOGGER.debug(
                    "Adapted batch size target: {} -> {} (message rate: {}/s)",
                    currentTarget,
                    newTarget,
                    messageRate);
        }

        lastAdaptiveCheck = now;
        lastMessageCount = currentMessageCount;
    }

    /**
     * Gets performance metrics for the batcher.
     *
     * @return formatted string with metrics
     */
    public String getMetrics() {
        long messages = messagesProcessed.get();
        long batches = batchesSent.get();
        long bytes = bytesProcessed.get();
        double avgBatchSize = batches > 0 ? (double) messages / batches : 0;
        double avgBatchBytes = batches > 0 ? (double) bytes / batches : 0;

        return String.format(
                "MessageBatcher Metrics:\n"
                        + "  Messages processed: %d\n"
                        + "  Batches sent: %d\n"
                        + "  Bytes processed: %d KB\n"
                        + "  Average batch size: %.1f messages\n"
                        + "  Average batch bytes: %.1f KB\n"
                        + "  Current batch target: %d\n"
                        + "  Current batch: %d messages, %d bytes",
                messages,
                batches,
                bytes / 1024,
                avgBatchSize,
                avgBatchBytes / 1024,
                currentBatchSizeTarget.get(),
                currentBatch.size(),
                currentBatchBytes.get());
    }

    /** Resets all metrics. */
    public void resetMetrics() {
        messagesProcessed.set(0);
        batchesSent.set(0);
        bytesProcessed.set(0);
        lastMessageCount = 0;
        lastAdaptiveCheck = System.currentTimeMillis();
    }

    /** Shuts down the batcher, canceling any pending flushes. */
    public synchronized void shutdown() {
        if (flushFuture != null) {
            flushFuture.cancel(false);
            flushFuture = null;
        }
        currentBatch.clear();
        currentBatchBytes.set(0);
        flushScheduled.set(false);
    }
}
