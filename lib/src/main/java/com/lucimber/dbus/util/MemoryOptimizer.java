/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for memory optimization and monitoring in D-Bus Client Java.
 *
 * <p>This class provides tools for:
 *
 * <ul>
 *   <li>Monitoring memory usage and detecting memory pressure
 *   <li>Providing guidance on when to trigger cleanup operations
 *   <li>Collecting statistics about object allocation patterns
 *   <li>Suggesting optimal collection sizes
 * </ul>
 *
 * <p>This is primarily intended for internal use by the library to make intelligent decisions about
 * memory management, such as when to clean up cached objects or resize collections.
 */
public final class MemoryOptimizer {

    private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();

    // Thresholds for memory pressure detection
    private static final double HIGH_MEMORY_USAGE_THRESHOLD = 0.85; // 85%
    private static final double CRITICAL_MEMORY_USAGE_THRESHOLD = 0.95; // 95%

    // Statistics tracking
    private static final AtomicLong totalAllocations = new AtomicLong(0);
    private static final AtomicLong totalBytesAllocated = new AtomicLong(0);
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);

    private MemoryOptimizer() {
        // Utility class - no instantiation
    }

    /**
     * Checks if the system is currently under memory pressure. This can be used to trigger cleanup
     * operations or reduce caching.
     *
     * @return true if memory usage is above the high threshold
     */
    public static boolean isUnderMemoryPressure() {
        return getHeapUsageRatio() > HIGH_MEMORY_USAGE_THRESHOLD;
    }

    /**
     * Checks if the system is in critical memory state. This should trigger immediate cleanup and
     * reduce allocations.
     *
     * @return true if memory usage is above the critical threshold
     */
    public static boolean isInCriticalMemoryState() {
        return getHeapUsageRatio() > CRITICAL_MEMORY_USAGE_THRESHOLD;
    }

    /**
     * Gets the current heap memory usage as a ratio (0.0 to 1.0).
     *
     * @return current heap usage ratio
     */
    public static double getHeapUsageRatio() {
        MemoryUsage heapUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();

        // If max is undefined (-1), use committed memory
        if (max < 0) {
            max = heapUsage.getCommitted();
        }

        return max > 0 ? (double) used / max : 0.0;
    }

    /**
     * Suggests an optimal initial capacity for collections based on memory pressure and expected
     * size.
     *
     * @param expectedSize expected number of elements
     * @param elementSize approximate size per element in bytes
     * @return suggested initial capacity
     */
    public static int suggestCollectionCapacity(int expectedSize, int elementSize) {
        if (expectedSize <= 0) {
            return 0;
        }

        // Under memory pressure, be more conservative
        if (isUnderMemoryPressure()) {
            // Start smaller and let it grow
            return Math.min(expectedSize, 8);
        }

        // Normal memory conditions - size appropriately with some buffer
        long totalBytes = (long) expectedSize * elementSize;

        // For small collections, use exact size
        if (totalBytes < 1024) { // Less than 1KB
            return expectedSize;
        }

        // For larger collections, add 25% buffer to reduce resizing
        return Math.max(expectedSize, (int) (expectedSize * 1.25));
    }

    /**
     * Records an allocation for statistics tracking.
     *
     * @param bytes number of bytes allocated
     */
    public static void recordAllocation(long bytes) {
        totalAllocations.incrementAndGet();
        totalBytesAllocated.addAndGet(bytes);
    }

    /** Records a cache hit for statistics tracking. */
    public static void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /** Records a cache miss for statistics tracking. */
    public static void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Gets the current cache hit ratio.
     *
     * @return cache hit ratio (0.0 to 1.0)
     */
    public static double getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;

        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Returns current memory usage statistics as a formatted string. Useful for logging and
     * debugging.
     *
     * @return memory statistics string
     */
    public static String getMemoryStatistics() {
        MemoryUsage heapUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = MEMORY_MX_BEAN.getNonHeapMemoryUsage();

        return String.format(
                "Memory Statistics:\n"
                        + "  Heap: %.1f%% used (%d MB / %d MB)\n"
                        + "  Non-Heap: %d MB used\n"
                        + "  Total Allocations: %d (%d MB)\n"
                        + "  Cache Hit Ratio: %.2f%%\n"
                        + "  Memory Pressure: %s",
                getHeapUsageRatio() * 100,
                heapUsage.getUsed() / (1024 * 1024),
                heapUsage.getMax() / (1024 * 1024),
                nonHeapUsage.getUsed() / (1024 * 1024),
                totalAllocations.get(),
                totalBytesAllocated.get() / (1024 * 1024),
                getCacheHitRatio() * 100,
                isUnderMemoryPressure() ? "HIGH" : "NORMAL");
    }

    /**
     * Suggests whether a cleanup operation should be performed based on current memory conditions
     * and operation frequency.
     *
     * @param operationCount number of operations since last cleanup
     * @param cleanupThreshold normal threshold for cleanup
     * @return true if cleanup is recommended
     */
    public static boolean shouldPerformCleanup(long operationCount, long cleanupThreshold) {
        // Always cleanup if in critical memory state
        if (isInCriticalMemoryState()) {
            return operationCount > 0;
        }

        // Under memory pressure, cleanup more frequently
        if (isUnderMemoryPressure()) {
            return operationCount > (cleanupThreshold / 2);
        }

        // Normal memory conditions
        return operationCount >= cleanupThreshold;
    }

    /** Resets all internal statistics counters. Useful for testing or periodic stats reporting. */
    public static void resetStatistics() {
        totalAllocations.set(0);
        totalBytesAllocated.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
}
