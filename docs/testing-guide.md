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

**Reliable Cross-Platform Testing (Recommended):**
```bash
# Option 1: Simple test script (fastest)
./test-container.sh

# Option 1a: With verbose output to see detailed test results
./test-container.sh --verbose

# Option 2: Gradle task (skips PMD/Checkstyle/JaCoCo for speed)
./gradlew integrationTestContainer

# Option 2a: With verbose output to see detailed test results
./gradlew integrationTestContainer -PshowOutput

# Option 3: Manual Docker run
docker build -f Dockerfile.test -t dbus-integration-test .
docker run --rm dbus-integration-test
```

**Host-Based Testing (May Fail on Non-Linux):**
```bash
# Basic execution
./gradlew integrationTest

# With verbose output to see detailed test results
./gradlew integrationTest -PshowOutput
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

Integration tests use Docker to provide isolated D-Bus environments with two approaches:

#### Container-Based Testing (Recommended)
Tests run inside a Linux container with native D-Bus daemon:

```java
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class ConnectionIntegrationTest extends DBusIntegrationTestBase {
    // Tests run inside Linux container with full D-Bus functionality
}
```

**Features:**
- ✅ **Cross-platform reliability** - works on macOS, Linux, Windows
- 🔒 **Full SASL authentication** - EXTERNAL + DBUS_COOKIE_SHA1
- 🌐 **Both connection types** - Unix socket + TCP
- 🐳 **Native Linux environment** - eliminates cross-platform SASL issues
- ⚡ **Consistent performance** - same environment every time

#### Host-Based Testing (Legacy)
Tests run on host connecting to containerized D-Bus daemon:

```java
// Uses Testcontainers to manage D-Bus daemon container
// Host JVM connects to container via TCP
```

**Limitations:**
- ⚠️ **May fail on macOS** due to SASL EXTERNAL authentication issues
- ⚠️ **May fail on Windows** due to Unix socket limitations
- ✅ **Usually works on Linux** with proper D-Bus installation

#### Technical Implementation

**Container Architecture:**
```dockerfile
# Multi-stage build
FROM gradle:8.14.2-jdk17 AS builder
# ... compile Java code

FROM ubuntu:22.04
# ... install D-Bus + JDK
# ... configure SASL authentication
# ... run tests inside container
```

**D-Bus Configuration:**
- SASL mechanisms: EXTERNAL, DBUS_COOKIE_SHA1
- Both transport types: Unix socket `/tmp/dbus-test-socket`, TCP `127.0.0.1:12345`
- Proper authentication setup with D-Bus keyrings
- Policy configuration for comprehensive testing

**Connection Detection:**
```java
protected static boolean shouldPreferUnixSocket() {
    if (isRunningInContainer()) {
        return true; // Always prefer Unix socket in container
    }
    return false; // Use TCP for cross-platform compatibility
}
```

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

## Cross-Platform Testing

### Platform-Specific Considerations

#### macOS
- **Container-based**: ✅ Always works
- **Host-based**: ❌ Often fails due to SASL EXTERNAL authentication
- **Recommendation**: Use `./test-container.sh` or `./gradlew integrationTestContainer`

#### Linux
- **Container-based**: ✅ Always works
- **Host-based**: ✅ Usually works with proper D-Bus installation
- **Recommendation**: Either approach works, container-based is more reliable

#### Windows
- **Container-based**: ✅ Works with Docker Desktop
- **Host-based**: ❌ May fail due to Unix socket limitations
- **Recommendation**: Use `./test-container.sh` or `./gradlew integrationTestContainer`

### Prerequisites

**For Container-Based Testing:**
- Docker installed and running
- Docker daemon accessible from command line
- Internet connection for base image downloads (first run only)

**For Host-Based Testing:**
- Docker installed and running (for Testcontainers)
- D-Bus daemon available on host system (Linux only)

### Troubleshooting Integration Tests

**Docker Issues:**
```bash
# Check if Docker is running
docker version

# Check if Docker daemon is accessible
docker ps

# Test Docker with simple container
docker run hello-world
```

**Container Build Issues:**
```bash
# Clean rebuild container
docker rmi dbus-integration-test
./gradlew integrationTestContainer
```

**Host-Based Test Failures:**
```bash
# Check D-Bus installation (Linux)
dbus-daemon --version

# Check if D-Bus is running
ps aux | grep dbus

# Use container-based tests instead
./test-container.sh
```

**Network Issues:**
```bash
# Check if Docker can access internet
docker pull ubuntu:22.04
```

## Running Tests

### Complete Test Suite

```bash
# Run all test types (use container-based integration tests)
./gradlew test integrationTestContainer performanceTest chaosTest

# Generate coverage report
./gradlew jacocoTestReport
```

### Individual Test Categories

```bash
# Unit tests only (includes static analysis)
./gradlew test

# Integration tests - container-based (recommended, fast)
./test-container.sh
./gradlew integrationTestContainer

# Integration tests with verbose output
./test-container.sh --verbose
./gradlew integrationTestContainer -PshowOutput

# Integration tests - host-based (legacy, may fail, fast)
./gradlew integrationTest
./gradlew integrationTest -PshowOutput

# Performance benchmarks (fast - skips static analysis)
./gradlew performanceTest

# Chaos engineering tests (fast - skips static analysis)
./gradlew chaosTest
```

### Development Workflow

**During Development:**
```bash
# Quick feedback loop
./gradlew test

# Full integration verification
./test-container.sh

# Performance check
./gradlew performanceTest
```

**Before Commit:**
```bash
# Full verification suite
./gradlew check integrationTestContainer
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
      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Unit Tests
        run: ./gradlew test

  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Container-Based Integration Tests
        run: ./gradlew integrationTestContainer

  performance-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'release'
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Performance Tests
        run: ./gradlew performanceTest
```

### GitLab CI Example

```yaml
stages:
  - test
  - integration
  - performance

unit-tests:
  stage: test
  image: openjdk:17-jdk
  services:
    - docker:dind
  script:
    - ./gradlew test

integration-tests:
  stage: integration
  image: openjdk:17-jdk
  services:
    - docker:dind
  script:
    - ./gradlew integrationTestContainer

performance-tests:
  stage: performance
  image: openjdk:17-jdk
  services:
    - docker:dind
  script:
    - ./gradlew performanceTest
  only:
    - tags
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Unit Tests') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'lib/build/test-results/test/TEST-*.xml'
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh './gradlew integrationTestContainer'
            }
        }
        
        stage('Performance Tests') {
            when {
                anyOf {
                    branch 'main'
                    buildingTag()
                }
            }
            steps {
                sh './gradlew performanceTest'
            }
        }
    }
}
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

### Performance Considerations

**Container-Based Testing:**
- **First run**: 3-5 minutes (includes Docker image downloads)
- **Subsequent runs**: 1-2 minutes (uses Docker layer caching)
- **Docker build**: ~1 minute (Java compilation + container setup)
- **Test execution**: ~30-60 seconds (actual test runtime)

**Host-Based Testing:**
- **First run**: 1-2 minutes (Testcontainers setup)
- **Subsequent runs**: 30-60 seconds (when working)
- **Cross-platform failures**: May timeout after 30 seconds

**Optimization Tips:**
- Use `./test-container.sh` for fastest container-based testing
- Enable Docker layer caching in CI/CD
- Run integration tests in parallel with unit tests
- Use `--no-daemon` flag to avoid Gradle daemon overhead
- **All integration test tasks skip expensive static analysis** (PMD, Checkstyle, JaCoCo) for faster execution

## Troubleshooting

### Common Issues

**Docker not available:**
```
Integration tests will be skipped if Docker is not installed or running
Solution: Install Docker and ensure daemon is running
```

**Container build failures:**
```
Error: Failed to build Docker container
Solution: Check Docker permissions and disk space
```

**SASL authentication failures (host-based tests):**
```
Error: Authentication failed with EXTERNAL mechanism
Solution: Use container-based tests instead: ./test-container.sh
```

**Performance test failures:**
```
Error: OutOfMemoryError during performance tests
Solution: Increase heap size: ./gradlew performanceTest -Xmx4g
```

**Chaos test instability:**
```
Chaos tests may be flaky by design - rerun to verify
```

**Gradle task not found:**
```
Error: Task 'integrationTestContainer' not found
Solution: Run from project root directory
```

### Debug Mode

Enable debug logging for test troubleshooting:

```bash
# Debug unit tests
./gradlew test --debug-jvm

# Debug integration tests with verbose output
./gradlew integrationTestContainer --info

# Debug container execution
docker run -it --rm dbus-integration-test /bin/bash
```

### Verbose Test Output

To see detailed test output including D-Bus daemon logs and test execution details:

**Container-based tests:**
```bash
# Using test script
./test-container.sh --verbose

# Using Gradle task
./gradlew integrationTestContainer -PshowOutput
```

**Host-based tests:**
```bash
# Show detailed test output
./gradlew integrationTest -PshowOutput
```

**Test script options:**
```bash
# Show help
./test-container.sh --help

# Show verbose test output
./test-container.sh --verbose

# Show Docker build output
./test-container.sh --show-build

# Show both verbose and build output
./test-container.sh --verbose --show-build
```

### Test Scripts

**Simple Container Test Script (test-container.sh):**
```bash
#!/bin/bash
# Builds and runs D-Bus integration tests in container
# Provides clear output and handles errors gracefully
./test-container.sh
```

**Features:**
- ✅ Simple execution without Gradle overhead
- 🐳 Automatic Docker container management
- 📊 Clear success/failure reporting
- ⚡ Fastest way to run integration tests

## Summary

This comprehensive testing suite ensures the D-Bus client library is reliable, performant, and resilient under various conditions:

- **Unit Tests**: Fast feedback for individual components
- **Integration Tests**: Real D-Bus scenarios with cross-platform reliability
- **Performance Tests**: Benchmarks for latency, throughput, and resource usage
- **Chaos Tests**: Resilience under adverse conditions

**Key Benefits:**
- 🌐 **Cross-platform compatibility** through container-based testing
- 🔒 **Comprehensive SASL authentication** testing
- ⚡ **Fast feedback loops** with multiple testing options
- 📊 **Detailed coverage analysis** for continuous improvement
- 🐳 **Reliable CI/CD integration** with Docker-based testing