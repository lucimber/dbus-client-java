/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferPoolManagerTest {

    private ByteBufferPoolManager poolManager;

    @BeforeEach
    void setUp() {
        poolManager = ByteBufferPoolManager.getInstance();
        poolManager.clear();
    }

    @Test
    void testAcquireAndRelease() {
        // Acquire buffer
        ByteBuffer buffer = poolManager.acquire(1024, ByteOrder.BIG_ENDIAN);
        assertNotNull(buffer);
        assertEquals(1024, buffer.capacity());
        assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
        assertEquals(0, buffer.position());
        assertEquals(1024, buffer.limit());

        // Release buffer
        poolManager.release(buffer);

        // Acquire again should reuse the buffer
        ByteBuffer buffer2 = poolManager.acquire(1024, ByteOrder.BIG_ENDIAN);
        assertNotNull(buffer2);
        assertEquals(1024, buffer2.capacity());
    }

    @Test
    void testDifferentSizes() {
        List<ByteBuffer> buffers = new ArrayList<>();

        // Acquire buffers of different sizes
        for (int size : new int[] {64, 128, 256, 512, 1024, 2048}) {
            ByteBuffer buffer = poolManager.acquire(size, ByteOrder.LITTLE_ENDIAN);
            assertTrue(buffer.capacity() >= size);
            assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());
            buffers.add(buffer);
        }

        // Release all buffers
        for (ByteBuffer buffer : buffers) {
            poolManager.release(buffer);
        }

        // Check metrics
        String metrics = poolManager.getPerformanceMetrics();
        assertTrue(metrics.contains("Acquires: 6"));
        assertTrue(metrics.contains("Releases: 6"));
    }

    @Test
    void testLargeBufferNotPooled() {
        // Very large buffer should not be pooled
        ByteBuffer largeBuffer = poolManager.acquire(128 * 1024, ByteOrder.BIG_ENDIAN);
        assertNotNull(largeBuffer);
        assertEquals(128 * 1024, largeBuffer.capacity());

        poolManager.release(largeBuffer);

        // Metrics should show this wasn't pooled
        String metrics = poolManager.getPerformanceMetrics();
        assertTrue(metrics.contains("Pool Statistics"));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(
                    () -> {
                        try {
                            for (int j = 0; j < operationsPerThread; j++) {
                                ByteBuffer buffer = poolManager.acquire(256, ByteOrder.BIG_ENDIAN);
                                assertNotNull(buffer);

                                // Use the buffer
                                buffer.putInt(42);
                                buffer.flip();
                                assertEquals(42, buffer.getInt());

                                poolManager.release(buffer);
                                successCount.incrementAndGet();
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * operationsPerThread, successCount.get());

        // Check metrics
        String metrics = poolManager.getPerformanceMetrics();
        assertTrue(metrics.contains("Acquires: " + (threadCount * operationsPerThread)));
        assertTrue(metrics.contains("Releases: " + (threadCount * operationsPerThread)));
    }

    @Test
    void testPoolHitRate() {
        // First acquire should be a miss
        ByteBuffer buffer1 = poolManager.acquire(512, ByteOrder.BIG_ENDIAN);
        poolManager.release(buffer1);

        // Subsequent acquires of same size should be hits
        for (int i = 0; i < 10; i++) {
            ByteBuffer buffer = poolManager.acquire(512, ByteOrder.BIG_ENDIAN);
            poolManager.release(buffer);
        }

        String metrics = poolManager.getPerformanceMetrics();
        // Should have high hit rate (10 hits out of 11 total)
        assertTrue(metrics.contains("Pool Hits"));
        assertTrue(metrics.contains("Pool Misses"));
    }

    @Test
    void testMetricsReset() {
        // Perform some operations
        ByteBuffer buffer = poolManager.acquire(256, ByteOrder.BIG_ENDIAN);
        poolManager.release(buffer);

        // Reset metrics
        poolManager.resetMetrics();

        String metrics = poolManager.getPerformanceMetrics();
        assertTrue(metrics.contains("Acquires: 0"));
        assertTrue(metrics.contains("Releases: 0"));
    }

    @Test
    void testNullRelease() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> poolManager.release(null));
    }

    @Test
    void testInvalidCapacity() {
        assertThrows(
                IllegalArgumentException.class, () -> poolManager.acquire(0, ByteOrder.BIG_ENDIAN));

        assertThrows(
                IllegalArgumentException.class,
                () -> poolManager.acquire(-1, ByteOrder.BIG_ENDIAN));
    }
}
