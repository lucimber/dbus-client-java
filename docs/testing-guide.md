# D-Bus Client Testing Guide

The D-Bus client library includes a comprehensive testing suite designed to ensure reliability, performance, and resilience across different scenarios.

## Test Structure

### Test Categories

#### 1. **Unit Tests** (Default)
- **Location**: `lib/src/test/java/**/*Test.java`
- **Coverage**: 67% instruction coverage, 48% branch coverage
- **Purpose**: Test individual components in isolation
- **Run with**: `./gradlew test`

```bash
./gradlew test
```

#### 2. **Integration Tests** 🐳
- **Location**: `lib/src/test/java/**/integration/**`
- **Tag**: `@Tag("integration")`
- **Purpose**: Test real D-Bus scenarios using Docker containers
- **Requirements**: Docker installed and running
- **Run with**: `./gradlew integrationTest`

```bash
./gradlew integrationTest
```

#### 3. **Performance Tests** ⚡
- **Location**: `lib/src/test/java/**/performance/**`
- **Tag**: `@Tag("performance")`
- **Purpose**: Benchmark throughput, latency, and resource usage
- **Memory**: Allocated 2GB heap for performance testing
- **Run with**: `./gradlew performanceTest`

```bash
./gradlew performanceTest
```

#### 4. **Chaos Tests** 🌪️
- **Location**: `lib/src/test/java/**/chaos/**`
- **Tag**: `@Tag("chaos")`
- **Purpose**: Test resilience under adverse conditions
- **Run with**: `./gradlew chaosTest`

```bash
./gradlew chaosTest
```

## Test Infrastructure

### Docker-Based Integration Testing

Integration tests use Docker to provide isolated D-Bus environments:

```java
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class ConnectionIntegrationTest extends DBusIntegrationTestBase {
    // Tests real D-Bus connections using containerized D-Bus daemon
}
```

**Features:**
- Automatic Docker container management
- Cross-platform compatibility
- Isolated test environments
- TCP and Unix socket support

### Performance Benchmarking

Performance tests measure key metrics:

```java
@Tag("performance")
@DisabledIf("shouldSkipPerformanceTests")
public class DBusPerformanceBenchmark {
    // Benchmarks latency, throughput, memory usage
}
```

**Measured Metrics:**
- Method call latency (avg, min, max, p95)
- Throughput (operations/second)
- Concurrent connection performance
- Memory usage per operation

### Chaos Engineering

Chaos tests simulate failure scenarios:

```java
@Tag("chaos")
@DisabledIf("shouldSkipChaosTests")
public class DBusChaosTest {
    // Tests resilience under network partitions, resource exhaustion, etc.
}
```

**Failure Scenarios:**
- Network partitions
- Rapid connection cycling
- Resource exhaustion
- Concurrent stress testing
- Slow response handling

## Coverage Analysis

### Current Coverage Report

| Package | Instruction Coverage | Branch Coverage | Strong Areas |
|---------|---------------------|-----------------|--------------|
| **Encoders** | 87% | 84% | ✅ Excellent |
| **Decoders** | 83% | 74% | ✅ Very Good |
| **Connection** | 78% | 51% | ✅ Good |
| **Messages** | 76% | 55% | ⚠️ Good |
| **Types** | 68% | 55% | ⚠️ Good |
| **Netty** | 62% | 35% | ⚠️ Needs Improvement |
| **SASL** | 32% | 29% | ❌ Needs Work |
| **Exceptions** | 0% | N/A | ❌ No Coverage |
| **Utilities** | 14% | 10% | ❌ Needs Work |

### Areas for Improvement

1. **Exception Handling** (0% coverage)
   - Exception classes completely untested
   - Need error scenario testing

2. **SASL Authentication** (32% coverage)
   - Limited authentication testing
   - Need more authentication mechanism tests

3. **Utilities** (14% coverage)
   - Helper classes under-tested
   - Need comprehensive utility testing

## Test Configuration

### Gradle Configuration

```kotlin
// Unit tests (exclude integration/performance/chaos)
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration", "performance", "chaos")
    }
}

// Integration tests
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    systemProperty("testcontainers.reuse.enable", "true")
}

// Performance tests
tasks.register<Test>("performanceTest") {
    useJUnitPlatform {
        includeTags("performance")
    }
    maxHeapSize = "2g"
}

// Chaos tests
tasks.register<Test>("chaosTest") {
    useJUnitPlatform {
        includeTags("chaos")
    }
}
```

### Test Dependencies

```kotlin
// Core testing
testImplementation("org.junit.jupiter:junit-jupiter-api")
testImplementation("org.mockito:mockito-core:5.8.0")

// Integration testing
testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.awaitility:awaitility:4.2.2")
```

## Running Tests

### Complete Test Suite

```bash
# Run all test types
./gradlew test integrationTest performanceTest chaosTest

# Generate coverage report
./gradlew jacocoTestReport
```

### Individual Test Categories

```bash
# Unit tests only (default)
./gradlew test

# Integration tests (requires Docker)
./gradlew integrationTest

# Performance benchmarks
./gradlew performanceTest

# Chaos engineering tests
./gradlew chaosTest
```

### Skipping Test Categories

Tests can be skipped using environment variables:

```bash
# Skip integration tests
export SKIP_INTEGRATION_TESTS=true
./gradlew integrationTest

# Skip performance tests
export SKIP_PERFORMANCE_TESTS=true
./gradlew performanceTest

# Skip chaos tests
export SKIP_CHAOS_TESTS=true
./gradlew chaosTest
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Test Suite

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Unit Tests
        run: ./gradlew test

  integration-tests:
    runs-on: ubuntu-latest
    services:
      docker:
        image: docker:latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Integration Tests
        run: ./gradlew integrationTest

  performance-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'release'
    steps:
      - uses: actions/checkout@v3
      - name: Run Performance Tests
        run: ./gradlew performanceTest
```

## Best Practices

### Writing Tests

1. **Use Descriptive Names**
   ```java
   @Test
   void testConnectionResilenceToNetworkPartition() {
       // Clear test intent
   }
   ```

2. **Follow AAA Pattern**
   ```java
   @Test
   void testMethodCallLatency() {
       // Arrange
       Connection connection = createTestConnection();
       
       // Act
       long startTime = System.nanoTime();
       connection.sendRequest(methodCall);
       long endTime = System.nanoTime();
       
       // Assert
       assertTrue(latency < maxExpectedLatency);
   }
   ```

3. **Clean Up Resources**
   ```java
   @AfterEach
   void tearDown() {
       if (connection != null) {
           connection.close();
       }
   }
   ```

### Test Environment

1. **Docker Requirements**
   - Docker must be installed for integration tests
   - Tests will be skipped if Docker unavailable

2. **Resource Management**
   - Performance tests allocate additional memory
   - Chaos tests may consume significant resources

3. **Timeouts**
   - Integration tests: 30 second default timeout
   - Performance tests: 5 minute benchmark timeout
   - Chaos tests: 2 minute chaos timeout

## Troubleshooting

### Common Issues

**Docker not available:**
```
Integration tests will be skipped if Docker is not installed or running
```

**Performance test failures:**
```
Increase heap size: ./gradlew performanceTest -Xmx4g
```

**Chaos test instability:**
```
Chaos tests may be flaky by design - rerun to verify
```

### Debug Mode

Enable debug logging for test troubleshooting:

```bash
./gradlew test --debug-jvm
```

This comprehensive testing suite ensures the D-Bus client library is reliable, performant, and resilient under various conditions.