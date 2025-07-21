/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * Utility classes and helper functions for D-Bus operations and internal framework use.
 * 
 * <p>This package contains utility classes that provide common functionality
 * used throughout the D-Bus client library. These utilities handle tasks like
 * byte manipulation, string processing, validation, and other cross-cutting concerns.
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> Most utilities in this package are used internally.
 * However, {@link LoggerUtils} can be helpful for debugging, and {@link ByteBufferPoolManager}
 * is useful if you need to optimize memory usage in high-throughput scenarios.</p>
 * 
 * <h2>Key Utilities</h2>
 * 
 * <h3>Byte Operations</h3>
 * <p>Utilities for handling D-Bus binary data and byte array operations:
 * 
 * <pre>{@code
 * // Byte buffer utilities
 * ByteBuffer buffer = ByteBufferUtils.allocateDirect(1024);
 * ByteBufferUtils.writeBigEndianInt32(buffer, 42);
 * int value = ByteBufferUtils.readBigEndianInt32(buffer);
 * 
 * // Byte array manipulation
 * byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04};
 * String hex = ByteUtils.toHexString(data);
 * byte[] restored = ByteUtils.fromHexString(hex);
 * }</pre>
 * 
 * <h3>String Processing</h3>
 * <p>String utilities for D-Bus protocol requirements:
 * 
 * <pre>{@code
 * // String validation
 * boolean isValidObjectPath = StringUtils.isValidObjectPath("/org/example/Object");
 * boolean isValidInterface = StringUtils.isValidInterfaceName("org.example.Interface");
 * boolean isValidBusName = StringUtils.isValidBusName("org.example.Service");
 * 
 * // String normalization
 * String normalized = StringUtils.normalizeObjectPath("/org/example//Object/");
 * String sanitized = StringUtils.sanitizeString(userInput);
 * }</pre>
 * 
 * <h3>Validation Utilities</h3>
 * <p>Validation helpers for D-Bus protocol compliance:
 * 
 * <pre>{@code
 * // Argument validation
 * ValidationUtils.requireNonNull(argument, "Argument cannot be null");
 * ValidationUtils.requireNonEmpty(collection, "Collection cannot be empty");
 * ValidationUtils.requireValidRange(value, 0, 100, "Value must be between 0 and 100");
 * 
 * // D-Bus specific validation
 * ValidationUtils.validateObjectPath(path);
 * ValidationUtils.validateInterfaceName(interfaceName);
 * ValidationUtils.validateSignature(signature);
 * }</pre>
 * 
 * <h3>Concurrent Utilities</h3>
 * <p>Thread-safe utilities for concurrent operations:
 * 
 * <pre>{@code
 * // Thread-safe collections
 * ConcurrentMap<String, Object> cache = ConcurrentUtils.newConcurrentHashMap();
 * Set<String> syncSet = ConcurrentUtils.newConcurrentHashSet();
 * 
 * // Future utilities
 * CompletableFuture<String> future = FutureUtils.completedFuture("result");
 * CompletableFuture<Void> timeout = FutureUtils.withTimeout(
 *     operation, Duration.ofSeconds(30));
 * }</pre>
 * 
 * <h2>Internal Framework Utilities</h2>
 * 
 * <h3>Logging Support</h3>
 * <p>Structured logging utilities for consistent log formatting:
 * 
 * <pre>{@code
 * // Structured logging
 * Logger logger = LoggerFactory.getLogger(MyClass.class);
 * LogUtils.debug(logger, "Processing message", 
 *     "type", messageType, 
 *     "serial", serial,
 *     "size", messageSize);
 * 
 * // Performance logging
 * try (TimingContext timing = LogUtils.startTiming("operation")) {
 *     performOperation();
 * }
 * }</pre>
 * 
 * <h3>Configuration Helpers</h3>
 * <p>Configuration parsing and validation utilities:
 * 
 * <pre>{@code
 * // Environment variable parsing
 * String dbusAddress = ConfigUtils.getEnvVar("DBUS_SESSION_BUS_ADDRESS");
 * int timeout = ConfigUtils.getEnvInt("DBUS_TIMEOUT", 30);
 * boolean enabled = ConfigUtils.getEnvBoolean("DBUS_DEBUG", false);
 * 
 * // Configuration validation
 * ConfigUtils.validateConfiguration(config);
 * Properties props = ConfigUtils.loadProperties("dbus.properties");
 * }</pre>
 * 
 * <h3>Resource Management</h3>
 * <p>Utilities for managing system resources:
 * 
 * <pre>{@code
 * // Resource cleanup
 * try (CloseableResource resource = ResourceUtils.acquire()) {
 *     // Use resource
 * } // Automatically cleaned up
 * 
 * // Memory management
 * long memoryUsage = ResourceUtils.getMemoryUsage();
 * ResourceUtils.runGC();
 * boolean lowMemory = ResourceUtils.isLowMemory();
 * }</pre>
 * 
 * <h2>Data Structure Utilities</h2>
 * 
 * <h3>Collection Helpers</h3>
 * <p>Utilities for working with collections and data structures:
 * 
 * <pre>{@code
 * // Immutable collections
 * List<String> immutableList = CollectionUtils.immutableList("a", "b", "c");
 * Set<Integer> immutableSet = CollectionUtils.immutableSet(1, 2, 3);
 * Map<String, String> immutableMap = CollectionUtils.immutableMap(
 *     "key1", "value1",
 *     "key2", "value2");
 * 
 * // Collection operations
 * List<String> filtered = CollectionUtils.filter(list, String::isEmpty);
 * List<Integer> mapped = CollectionUtils.map(list, String::length);
 * Optional<String> found = CollectionUtils.find(list, "target"::equals);
 * }</pre>
 * 
 * <h3>Caching Utilities</h3>
 * <p>Simple caching mechanisms for performance optimization:
 * 
 * <pre>{@code
 * // LRU cache
 * Cache<String, Object> cache = CacheUtils.createLRUCache(1000);
 * cache.put("key", computeExpensiveValue());
 * Object value = cache.get("key");
 * 
 * // Time-based cache
 * Cache<String, Object> timeCache = CacheUtils.createTimeCache(
 *     Duration.ofMinutes(5));
 * }</pre>
 * 
 * <h2>Protocol Utilities</h2>
 * 
 * <h3>Marshalling Helpers</h3>
 * <p>Utilities for D-Bus message marshalling and unmarshalling:
 * 
 * <pre>{@code
 * // Alignment utilities
 * int alignedPosition = AlignmentUtils.alignTo(position, 8);
 * int padding = AlignmentUtils.calculatePadding(position, 4);
 * 
 * // Signature parsing
 * SignatureParser parser = new SignatureParser("a{sv}");
 * SignatureType type = parser.parseNext();
 * boolean isContainer = SignatureUtils.isContainer(type);
 * }</pre>
 * 
 * <h3>Endianness Handling</h3>
 * <p>Utilities for handling byte order in D-Bus messages:
 * 
 * <pre>{@code
 * // Endianness conversion
 * ByteOrder order = EndiannessUtils.getSystemByteOrder();
 * int swapped = EndiannessUtils.swapInt32(value);
 * short swapped16 = EndiannessUtils.swapInt16(value);
 * 
 * // D-Bus specific endianness
 * boolean isLittleEndian = EndiannessUtils.isDBusLittleEndian(flags);
 * }</pre>
 * 
 * <h2>Testing Utilities</h2>
 * 
 * <h3>Test Helpers</h3>
 * <p>Utilities for testing D-Bus applications:
 * 
 * <pre>{@code
 * // Test data generation
 * DBusString testString = TestUtils.randomDBusString(100);
 * DBusArray<DBusInt32> testArray = TestUtils.randomDBusArray(10, DBusInt32.class);
 * 
 * // Mock utilities
 * Connection mockConnection = TestUtils.createMockConnection();
 * InboundSignal mockSignal = TestUtils.createMockSignal("org.test.Signal");
 * 
 * // Assertion helpers
 * TestUtils.assertValidObjectPath(path);
 * TestUtils.assertValidSignature(signature);
 * TestUtils.assertMessageEquals(expected, actual);
 * }</pre>
 * 
 * <h2>Performance Utilities</h2>
 * 
 * <h3>Profiling Support</h3>
 * <p>Utilities for performance monitoring and profiling:
 * 
 * <pre>{@code
 * // Performance measurement
 * Stopwatch stopwatch = PerformanceUtils.startStopwatch();
 * performOperation();
 * Duration elapsed = stopwatch.elapsed();
 * 
 * // Memory profiling
 * MemoryProfiler profiler = PerformanceUtils.startMemoryProfiler();
 * performMemoryIntensiveOperation();
 * MemoryUsage usage = profiler.getUsage();
 * }</pre>
 * 
 * <h2>Best Practices</h2>
 * 
 * <ul>
 * <li><strong>Null Safety:</strong> All utility methods handle null inputs gracefully</li>
 * <li><strong>Immutability:</strong> Utility classes are stateless and thread-safe</li>
 * <li><strong>Performance:</strong> Utilities are optimized for common use cases</li>
 * <li><strong>Validation:</strong> Input validation is performed where appropriate</li>
 * <li><strong>Documentation:</strong> All utilities are thoroughly documented</li>
 * </ul>
 * 
 * <h2>Internal Use</h2>
 * 
 * <p>While this package is primarily for internal framework use, some utilities
 * may be useful for application developers. However, these utilities are not
 * part of the public API and may change between versions.
 * 
 * @see com.lucimber.dbus.type
 * @see com.lucimber.dbus.message
 * @see com.lucimber.dbus.connection
 * @since 1.0
 */
package com.lucimber.dbus.util;
