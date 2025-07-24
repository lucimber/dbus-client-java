# D-Bus Client Java

[![Build Status](https://github.com/lucimber/dbus-client-java/workflows/Continuous%20Integration/badge.svg)](https://github.com/lucimber/dbus-client-java/actions)
[![CodeQL](https://github.com/lucimber/dbus-client-java/workflows/Code%20Analysis/badge.svg)](https://github.com/lucimber/dbus-client-java/actions)
[![Coverage](https://img.shields.io/endpoint?url=https://lucimber.github.io/dbus-client-java/coverage-badge.json)](https://lucimber.github.io/dbus-client-java/latest/coverage/)
[![Documentation](https://img.shields.io/badge/docs-GitHub%20Pages-blue.svg)](https://lucimber.github.io/dbus-client-java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

A high-performance, asynchronous D-Bus client library for Java applications. Built on [Netty](https://netty.io) with a sophisticated dual-pipeline architecture that supports **safe blocking operations** in handlers.

## Key Features

- **Asynchronous & Thread-Safe**: Non-blocking I/O with safe blocking operations in handlers
- **Multiple Transports**: Unix domain sockets and TCP/IP connections
- **SASL Authentication**: EXTERNAL and DBUS_COOKIE_SHA1 mechanisms
- **Complete Type System**: All D-Bus types with proper marshalling/unmarshalling
- **Cross-Platform**: Containerized integration tests for consistent behavior
- **Production Ready**: Comprehensive testing, monitoring, and error handling

## Documentation

Complete API documentation is available on GitHub Pages with version support:

- **[Full Documentation](https://lucimber.github.io/dbus-client-java/)** - Comprehensive guides and API reference
- **[Latest API](https://lucimber.github.io/dbus-client-java/latest/)** - Current development documentation
- **[Version History](https://lucimber.github.io/dbus-client-java/)** - Documentation for all releases

## Quick Start

### Installation

```xml
<!-- Maven -->
<dependency>
    <groupId>com.lucimber</groupId>
    <artifactId>lucimber-dbus-client</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```

```gradle
// Gradle
implementation 'com.lucimber:lucimber-dbus-client:2.0-SNAPSHOT'
```

### Basic Usage

```java
// Connect to D-Bus
Connection connection = NettyConnection.newSystemBusConnection();
connection.connect().toCompletableFuture().get();

// Send a method call
OutboundMethodCall call = OutboundMethodCall.Builder
    .create()
    .withSerial(connection.getNextSerial())
    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
    .withMember(DBusString.valueOf("ListNames"))
    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
    .withReplyExpected(true)
    .build();

CompletableFuture<InboundMessage> response = connection.sendRequest(call);

// Close connection
connection.close();
```

### Safe Blocking Operations

**✅ Handlers run on dedicated thread pools - blocking operations are completely safe!**

```java
public class MyHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // ✅ SAFE - Database calls, REST APIs, file I/O all OK here!
        String result = databaseCall(msg);  // Blocking - totally fine!
        ctx.propagateInboundMessage(processedMessage(msg, result));
    }
}
```

## Testing

### Linux
```bash
# Build and test directly
./gradlew build

# Run integration tests (containerized)
./gradlew integrationTest
```

### macOS / Windows
```bash
# Start D-Bus in Docker
docker-compose -f docker/docker-compose.yml up -d

# Run tests against Docker D-Bus
./gradlew test

# Or run tests inside container
docker-compose -f docker/docker-compose.yml run --rm test-runner
```

See [Platform Setup Guide](docs/guides/platform-setup.md) for detailed cross-platform instructions.

## Documentation

- **[Developer Guide](docs/guides/developer-guide.md)** - Comprehensive usage guide
- **[Platform Setup](docs/guides/platform-setup.md)** - Cross-platform development setup
- **[Client-Server Patterns](docs/guides/client-server-patterns.md)** - Using ServiceProxy and StandardInterfaceHandler
- **[Standard Interfaces](docs/standard-interfaces.md)** - Using D-Bus standard interfaces
- **[D-Bus Compatibility](docs/dbus-compatibility.md)** - Specification version support
- **[Contributing Guidelines](CONTRIBUTING.md)** - How to contribute
- **[Security Policy](SECURITY.md)** - Vulnerability reporting

## Architecture

This library implements a dual-pipeline architecture with strict thread isolation:

- **Transport Layer**: Netty-based async I/O for D-Bus protocol handling
- **Application Layer**: Dedicated thread pools for user code
- **RealityCheckpoint**: The bridge ensuring safe threading and message routing

This design enables handlers to safely perform blocking operations without affecting the underlying transport performance.

## D-Bus Compatibility

- **Protocol Version**: 1 (fully compatible)
- **Specification Support**: 0.41+ with selective newer features
- **Tested Against**: Standard D-Bus daemon and systemd's sd-bus

See [D-Bus Compatibility](docs/dbus-compatibility.md) for detailed version support.

## Status

**Current**: v2.0-SNAPSHOT - Core features stable, API may change based on feedback

- ✅ **Stable Core**: D-Bus client operations, SASL authentication, message handling
- ✅ **Production Architecture**: Thread-safe with comprehensive error handling
- ✅ **Comprehensive Testing**: Unit, integration, and performance tests
- ✅ **CI/CD Ready**: Automated testing, coverage, and security scanning

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md).

For security issues, see our [Security Policy](SECURITY.md).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

Copyright 2021-2025 Lucimber UG