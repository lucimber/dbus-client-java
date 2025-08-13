/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * A singleton manager for ByteBuffer pooling with performance metrics.
 *
 * <p>This manager provides a global instance of ByteBufferPool and tracks performance metrics to
 * help identify optimization opportunities.
 */
public final class ByteBufferPoolManager {

    private static final ByteBufferPoolManager INSTANCE = new ByteBufferPoolManager();

    private final ByteBufferPool pool;
    private final LongAdder acquireCount;
    private final LongAdder releaseCount;
    private final LongAdder poolHitCount;
    private final LongAdder poolMissCount;
    private final AtomicLong totalBytesAllocated;

    private ByteBufferPoolManager() {
        this.pool = new ByteBufferPool();
        this.acquireCount = new LongAdder();
        this.releaseCount = new LongAdder();
        this.poolHitCount = new LongAdder();
        this.poolMissCount = new LongAdder();
        this.totalBytesAllocated = new AtomicLong();
    }

    /**
     * Gets the singleton instance of ByteBufferPoolManager.
     *
     * @return the singleton instance
     */
    public static ByteBufferPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Acquires a ByteBuffer with at least the requested capacity.
     *
     * @param capacity minimum required capacity
     * @param order byte order for the buffer
     * @return a ByteBuffer ready for use
     */
    public ByteBuffer acquire(int capacity, ByteOrder order) {
        acquireCount.increment();

        // Track whether we get a pooled buffer or allocate a new one
        long beforeAllocated = totalBytesAllocated.get();
        ByteBuffer buffer = pool.acquire(capacity, order);
        long afterAllocated = totalBytesAllocated.get();

        if (afterAllocated > beforeAllocated) {
            poolMissCount.increment();
            totalBytesAllocated.addAndGet(buffer.capacity());
        } else {
            poolHitCount.increment();
        }

        return buffer;
    }

    /**
     * Returns a ByteBuffer to the pool for potential reuse.
     *
     * @param buffer the buffer to return (may be null)
     */
    public void release(ByteBuffer buffer) {
        if (buffer != null) {
            releaseCount.increment();
            pool.release(buffer);
        }
    }

    /**
     * Gets performance metrics for the buffer pool.
     *
     * @return a formatted string with performance statistics
     */
    public String getPerformanceMetrics() {
        long acquires = acquireCount.sum();
        long releases = releaseCount.sum();
        long hits = poolHitCount.sum();
        long misses = poolMissCount.sum();
        double hitRate = acquires > 0 ? (double) hits / acquires * 100 : 0;

        StringBuilder sb = new StringBuilder("ByteBufferPool Performance Metrics:\n");
        sb.append(String.format("  Acquires: %d\n", acquires));
        sb.append(String.format("  Releases: %d\n", releases));
        sb.append(String.format("  Pool Hits: %d (%.1f%%)\n", hits, hitRate));
        sb.append(String.format("  Pool Misses: %d\n", misses));
        sb.append(String.format("  Total Allocated: %d KB\n", totalBytesAllocated.get() / 1024));
        sb.append(String.format("  Buffers in Flight: %d\n", acquires - releases));
        sb.append("\n");
        sb.append(pool.getStatistics());

        return sb.toString();
    }

    /** Resets all performance metrics. Useful for testing or monitoring specific periods. */
    public void resetMetrics() {
        acquireCount.reset();
        releaseCount.reset();
        poolHitCount.reset();
        poolMissCount.reset();
        totalBytesAllocated.set(0);
    }

    /** Clears the pool and resets metrics. */
    public void clear() {
        pool.clear();
        resetMetrics();
    }
}
