/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread-safe ByteBuffer pool to reduce garbage collection pressure
 * by reusing ByteBuffer instances for encoding/decoding operations.
 * 
 * <p>This pool maintains separate queues for different buffer sizes,
 * automatically sizing up to the nearest power of two to improve
 * reuse efficiency. Buffers are cleaned before being returned to
 * ensure data integrity.</p>
 * 
 * <p>The pool has built-in size limits to prevent unbounded memory
 * growth under high load scenarios.</p>
 */
public final class ByteBufferPool {
  
  private static final int MIN_BUFFER_SIZE = 64; // 64 bytes minimum
  private static final int MAX_BUFFER_SIZE = 64 * 1024; // 64KB maximum
  private static final int MAX_BUFFERS_PER_SIZE = 16; // Limit per size class
  
  private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<ByteBuffer>> pools;
    
  /**
   * Creates a new ByteBuffer pool instance.
   */
  public ByteBufferPool() {
    this.pools = new ConcurrentHashMap<>();
  }
    
  /**
   * Acquires a ByteBuffer with at least the requested capacity.
   * The returned buffer will have its position at 0, limit at capacity,
   * and will be cleared of any previous data.
   *
   * @param capacity minimum required capacity
   * @param order byte order for the buffer
   * @return a ByteBuffer ready for use
   */
  public ByteBuffer acquire(int capacity, ByteOrder order) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be positive");
    }
    
    // For very large buffers, allocate directly without pooling
    if (capacity > MAX_BUFFER_SIZE) {
      return ByteBuffer.allocate(capacity).order(order);
    }
    
    // Round up to next power of 2 for better reuse
    int pooledSize = Math.max(MIN_BUFFER_SIZE, nextPowerOfTwo(capacity));
    
    ConcurrentLinkedQueue<ByteBuffer> pool = pools.computeIfAbsent(
        pooledSize, k -> new ConcurrentLinkedQueue<>());
    
    ByteBuffer buffer = pool.poll();
    if (buffer == null) {
      buffer = ByteBuffer.allocate(pooledSize);
    } else {
      // Clear the buffer for reuse
      buffer.clear();
    }
    
    return buffer.order(order);
  }
    
  /**
   * Returns a ByteBuffer to the pool for potential reuse.
   * The buffer should not be used after calling this method.
   *
   * @param buffer the buffer to return (may be null)
   */
  public void release(ByteBuffer buffer) {
    if (buffer == null) {
      return;
    }
    
    int capacity = buffer.capacity();
    
    // Only pool buffers within our size range
    if (capacity < MIN_BUFFER_SIZE || capacity > MAX_BUFFER_SIZE) {
      return; // Let GC handle it
    }
    
    ConcurrentLinkedQueue<ByteBuffer> pool = pools.get(capacity);
    if (pool != null && pool.size() < MAX_BUFFERS_PER_SIZE) {
      // Clear buffer before returning to pool
      buffer.clear();
      pool.offer(buffer);
    }
    // If pool is full or doesn't exist, let GC handle the buffer
  }
    
  /**
   * Gets statistics about the current pool state.
   * Useful for monitoring and debugging memory usage.
   *
   * @return a string representation of pool statistics
   */
  public String getStatistics() {
    StringBuilder sb = new StringBuilder("ByteBufferPool Statistics:\n");
    int totalBuffers = 0;
    long totalMemory = 0;
    
    for (var entry : pools.entrySet()) {
      int size = entry.getKey();
      int count = entry.getValue().size();
      totalBuffers += count;
      totalMemory += (long) size * count;
      
      sb.append(String.format("  Size %d bytes: %d buffers\n", size, count));
    }
    
    sb.append(String.format("Total: %d buffers, %d KB pooled memory\n", 
        totalBuffers, totalMemory / 1024));
    
    return sb.toString();
  }
    
  /**
   * Clears all pooled buffers, releasing memory back to the system.
   * Useful for cleanup or testing scenarios.
   */
  public void clear() {
    pools.clear();
  }
    
  /**
   * Calculates the next power of 2 greater than or equal to the input.
   * Used for efficient buffer size pooling.
   */
  private static int nextPowerOfTwo(int value) {
    if (value <= 0) {
      return 1;
    }
    
    // If already a power of 2, return as-is
    if ((value & (value - 1)) == 0) {
      return value;
    }
    
    // Find the next power of 2
    int power = 1;
    while (power < value) {
      power <<= 1;
    }
    return power;
  }
}