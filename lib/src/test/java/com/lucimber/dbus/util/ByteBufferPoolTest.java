/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ByteBufferPool memory optimization utility. */
class ByteBufferPoolTest {

    private ByteBufferPool pool;

    @BeforeEach
    void setUp() {
        pool = new ByteBufferPool();
    }

    @Test
    void testAcquireBasicBuffer() {
        ByteBuffer buffer = pool.acquire(100, ByteOrder.BIG_ENDIAN);

        assertNotNull(buffer);
        assertTrue(buffer.capacity() >= 100);
        assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    void testAcquireMinimumSize() {
        ByteBuffer buffer = pool.acquire(1, ByteOrder.LITTLE_ENDIAN);

        assertNotNull(buffer);
        assertTrue(buffer.capacity() >= 64); // MIN_BUFFER_SIZE
        assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());
    }

    @Test
    void testAcquireLargeBuffer() {
        // Request buffer larger than MAX_BUFFER_SIZE (64KB)
        ByteBuffer buffer = pool.acquire(128 * 1024, ByteOrder.BIG_ENDIAN);

        assertNotNull(buffer);
        assertEquals(128 * 1024, buffer.capacity());
        assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
    }

    @Test
    void testBufferReuse() {
        // Acquire a buffer
        ByteBuffer buffer1 = pool.acquire(256, ByteOrder.BIG_ENDIAN);
        int originalCapacity = buffer1.capacity();

        // Put some data in it
        buffer1.putInt(0x12345678);

        // Release it back to pool
        pool.release(buffer1);

        // Acquire another buffer of same size
        ByteBuffer buffer2 = pool.acquire(256, ByteOrder.BIG_ENDIAN);

        // Should get the same buffer back (reused)
        assertEquals(originalCapacity, buffer2.capacity());
        assertEquals(0, buffer2.position()); // Should be cleared
        assertEquals(buffer2.capacity(), buffer2.limit());

        // Buffer should be cleared (position=0) but may contain old data
        // This is expected behavior - clearing sets position/limit but doesn't zero memory
    }

    @Test
    void testInvalidCapacity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    pool.acquire(0, ByteOrder.BIG_ENDIAN);
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    pool.acquire(-10, ByteOrder.BIG_ENDIAN);
                });
    }

    @Test
    void testReleaseNull() {
        // Should not throw exception
        assertDoesNotThrow(() -> pool.release(null));
    }

    @Test
    void testStatistics() {
        // Start with empty pool
        String initialStats = pool.getStatistics();
        assertTrue(initialStats.contains("Total: 0 buffers"));

        // Acquire and release some buffers
        ByteBuffer buffer1 = pool.acquire(128, ByteOrder.BIG_ENDIAN);
        ByteBuffer buffer2 = pool.acquire(256, ByteOrder.BIG_ENDIAN);

        pool.release(buffer1);
        pool.release(buffer2);

        String stats = pool.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("ByteBufferPool Statistics"));
    }

    @Test
    void testClear() {
        // Add some buffers to pool
        ByteBuffer buffer1 = pool.acquire(128, ByteOrder.BIG_ENDIAN);
        ByteBuffer buffer2 = pool.acquire(256, ByteOrder.BIG_ENDIAN);

        pool.release(buffer1);
        pool.release(buffer2);

        // Clear the pool
        pool.clear();

        String stats = pool.getStatistics();
        assertTrue(stats.contains("Total: 0 buffers"));
    }

    @Test
    void testPowerOfTwoSizing() {
        // Test that buffers are rounded up to power of 2
        ByteBuffer buffer100 = pool.acquire(100, ByteOrder.BIG_ENDIAN);
        ByteBuffer buffer200 = pool.acquire(200, ByteOrder.BIG_ENDIAN);

        // 100 should round to 128 (next power of 2)
        assertTrue(buffer100.capacity() >= 100);
        assertEquals(128, buffer100.capacity());

        // 200 should round to 256 (next power of 2)
        assertTrue(buffer200.capacity() >= 200);
        assertEquals(256, buffer200.capacity());
    }

    @Test
    void testDifferentByteOrders() {
        ByteBuffer bigEndian = pool.acquire(64, ByteOrder.BIG_ENDIAN);
        ByteBuffer littleEndian = pool.acquire(64, ByteOrder.LITTLE_ENDIAN);

        assertEquals(ByteOrder.BIG_ENDIAN, bigEndian.order());
        assertEquals(ByteOrder.LITTLE_ENDIAN, littleEndian.order());
    }
}
