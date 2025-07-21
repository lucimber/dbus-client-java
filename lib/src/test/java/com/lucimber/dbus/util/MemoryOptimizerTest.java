/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MemoryOptimizer memory monitoring utility.
 */
class MemoryOptimizerTest {

  @BeforeEach
  void setUp() {
    MemoryOptimizer.resetStatistics();
  }

  @Test
  void testGetHeapUsageRatio() {
    double ratio = MemoryOptimizer.getHeapUsageRatio();
    
    assertTrue(ratio >= 0.0, "Heap usage ratio should be non-negative");
    assertTrue(ratio <= 1.0, "Heap usage ratio should not exceed 1.0");
  }

  @Test
  void testMemoryPressureDetection() {
    // These tests depend on current system memory state
    // We can at least verify they don't throw exceptions
    assertDoesNotThrow(() -> MemoryOptimizer.isUnderMemoryPressure());
    assertDoesNotThrow(() -> MemoryOptimizer.isInCriticalMemoryState());
    
    // Critical memory should be a subset of memory pressure
    if (MemoryOptimizer.isInCriticalMemoryState()) {
      assertTrue(MemoryOptimizer.isUnderMemoryPressure(), 
        "Critical memory state should imply memory pressure");
    }
  }

  @Test
  void testSuggestCollectionCapacity() {
    // Test zero and negative inputs
    assertEquals(0, MemoryOptimizer.suggestCollectionCapacity(0, 100));
    assertEquals(0, MemoryOptimizer.suggestCollectionCapacity(-5, 100));
    
    // Test normal case
    int capacity = MemoryOptimizer.suggestCollectionCapacity(10, 64);
    assertTrue(capacity >= 10, "Suggested capacity should be at least expected size");
    assertTrue(capacity <= 50, "Suggested capacity should be reasonable");
    
    // Test large collection
    int largeCapacity = MemoryOptimizer.suggestCollectionCapacity(1000, 1024);
    assertTrue(largeCapacity >= 1000, "Should handle large collections");
  }

  @Test
  void testAllocationTracking() {
    // Initial state
    assertEquals(0, MemoryOptimizer.getCacheHitRatio(), 0.001);
    
    // Record some allocations
    MemoryOptimizer.recordAllocation(1024);
    MemoryOptimizer.recordAllocation(2048);
    
    // Record cache statistics
    MemoryOptimizer.recordCacheHit();
    MemoryOptimizer.recordCacheHit();
    MemoryOptimizer.recordCacheMiss();
    
    // Check hit ratio
    double hitRatio = MemoryOptimizer.getCacheHitRatio();
    assertEquals(2.0/3.0, hitRatio, 0.001); // 2 hits out of 3 total
  }

  @Test
  void testMemoryStatistics() {
    String stats = MemoryOptimizer.getMemoryStatistics();
    
    assertNotNull(stats);
    assertTrue(stats.contains("Memory Statistics"));
    assertTrue(stats.contains("Heap:"));
    assertTrue(stats.contains("Cache Hit Ratio:"));
    assertTrue(stats.contains("Memory Pressure:"));
  }

  @Test
  void testShouldPerformCleanup() {
    // Test normal conditions
    assertFalse(MemoryOptimizer.shouldPerformCleanup(5, 10));
    assertTrue(MemoryOptimizer.shouldPerformCleanup(15, 10));
    
    // Test edge cases
    assertFalse(MemoryOptimizer.shouldPerformCleanup(0, 10));
    assertTrue(MemoryOptimizer.shouldPerformCleanup(10, 10));
  }

  @Test
  void testResetStatistics() {
    // Record some data
    MemoryOptimizer.recordAllocation(1024);
    MemoryOptimizer.recordCacheHit();
    MemoryOptimizer.recordCacheMiss();
    
    // Verify data exists
    assertTrue(MemoryOptimizer.getCacheHitRatio() > 0);
    
    // Reset and verify clean state
    MemoryOptimizer.resetStatistics();
    assertEquals(0, MemoryOptimizer.getCacheHitRatio(), 0.001);
  }

  @Test
  void testCacheHitRatioEdgeCases() {
    // No cache operations recorded
    assertEquals(0.0, MemoryOptimizer.getCacheHitRatio(), 0.001);
    
    // Only hits
    MemoryOptimizer.recordCacheHit();
    MemoryOptimizer.recordCacheHit();
    assertEquals(1.0, MemoryOptimizer.getCacheHitRatio(), 0.001);
    
    // Reset and only misses
    MemoryOptimizer.resetStatistics();
    MemoryOptimizer.recordCacheMiss();
    MemoryOptimizer.recordCacheMiss();
    assertEquals(0.0, MemoryOptimizer.getCacheHitRatio(), 0.001);
  }

  @Test
  void testCollectionCapacitySuggestionUnderPressure() {
    // This test is inherently system-dependent, but we can test the logic
    int normalCapacity = MemoryOptimizer.suggestCollectionCapacity(100, 64);
    
    // The capacity should be reasonable regardless of memory pressure
    assertTrue(normalCapacity > 0, "Should suggest positive capacity");
    assertTrue(normalCapacity <= 200, "Should not over-allocate excessively");
  }
}