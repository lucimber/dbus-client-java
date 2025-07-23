# Changelog

All notable changes to the D-Bus Client Java library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Migration guide from 1.x to 2.0 with comprehensive examples and troubleshooting

## [2.0.0] - 2025-01-23

### Added
- **Comprehensive Test Coverage**: Achieved 83% overall test coverage (improved from 68%)
  - Complete test suite for DummyConnection with 92% coverage
  - Full SASL authentication configuration testing (91% coverage)
  - 100% test coverage for ConnectionEvent and lifecycle management
  - Comprehensive exception package testing (31 exception classes)
- **Builder Pattern Support**: Inbound and outbound message construction with fluent API
- **Strategy Pattern Implementation**: Pluggable transport support for Unix sockets and TCP
- **Factory Pattern**: Streamlined encoder/decoder creation and management
- **Request-Response Correlation**: Built-in `sendRequest()` method with automatic correlation
- **DummyConnection**: Testing utility for unit testing without D-Bus daemon
- **Connection Health Monitoring**: Event-driven health checks and automatic reconnection
- **Performance Optimizations**: Memory benchmarks and throughput improvements
- **Code Quality Tools**: Spotless formatting, enhanced PMD and Checkstyle configurations
- **SPDX License Headers**: Comprehensive license compliance across all source files
- **Enhanced Documentation**: Migration guide, architecture docs, and comprehensive examples

### Changed
- **Maven Coordinates**: Updated from `dbus-client` to `lucimber-dbus-client` 
- **Package Structure**: Unified codec package (merged encoder/decoder packages)
- **Annotation Package**: Merged `impl` package into main annotation package
- **Connection API**: NettyConnection-based API replacing DBusConnection
- **Threading Model**: Dual-pipeline architecture with safe blocking operations
- **SASL Configuration**: Type-safe configuration classes replacing string-based config
- **Message Building**: All messages now use Builder pattern instead of constructors
- **Integration Test Stability**: Improved reliability and error handling
- **Error Messaging**: Enhanced error messages for failed D-Bus operations
- **Performance**: Optimized message encoding/decoding with reduced memory allocations

### Fixed
- **Checkstyle Violations**: Resolved all major code style issues across the codebase
- **PMD Violations**: Fixed code quality issues and suppressed intentional patterns
- **Integration Test Failures**: Resolved message correlation and timing issues
- **Javadoc Generation**: Fixed documentation build errors and improved API docs
- **Connection Timeout Handling**: Improved retry logic and timeout management
- **Docker Resource Issues**: Resolved CI/CD pipeline disk space and resource constraints
- **Message Serial Numbers**: Fixed missing serial numbers in test method calls
- **SASL Authentication**: Enhanced error handling for authentication failures

### Security
- **Enhanced SASL Testing**: Comprehensive testing for all authentication mechanisms
- **Improved Authentication Error Handling**: Better validation and error reporting
- **Message Integrity Validation**: Strengthened D-Bus message validation
- **Security Scanning**: Integrated security analysis in CI/CD pipeline

### Breaking Changes
- **Maven Coordinates**: `com.lucimber:dbus-client` → `com.lucimber:lucimber-dbus-client`
- **Connection Creation**: `DBusConnection.create()` → `NettyConnection.newSessionBusConnection()`
- **Package Imports**: `com.lucimber.dbus.encoder.*` → `com.lucimber.dbus.codec.encoder.*`
- **Message Construction**: Constructor-based → Builder pattern required
- **SASL Configuration**: String-based → Type-safe configuration classes

## [2.0-SNAPSHOT] - Previous Development

### Architecture
- **Dual-Pipeline System**: Sophisticated architecture with Public API and Netty transport layers
- **RealityCheckpoint**: Bridge component between pipeline layers for thread isolation
- **Thread Safety**: ApplicationTaskExecutor vs Netty EventLoop separation
- **Event Propagation**: Robust message flow through the system

### Core Features
- **Multiple Transport Options**: Unix Domain Sockets, TCP/IP connections
- **SASL Authentication**: EXTERNAL, DBUS_COOKIE_SHA1, ANONYMOUS mechanisms
- **Message Types**: Method calls, signals, errors, and replies
- **Asynchronous Operations**: CompletableFuture-based API
- **Spring Boot Integration**: Auto-configuration and dependency injection support
- **Health Monitoring**: Connection health checks and automatic reconnection

### Testing & Quality (Legacy)
- **Integration Testing**: Docker-based testing with real D-Bus daemon
- **Code Coverage**: 68% baseline coverage (improved to 83% in 2.0.0)
- **Code Quality**: Basic PMD, Checkstyle integration (enhanced in 2.0.0)
- **Performance Testing**: Initial memory usage and throughput benchmarks

### Documentation
- **Developer Guide**: Comprehensive learning path for new users
- **Architecture Documentation**: Detailed technical documentation
- **Examples**: Working code samples for common use cases
- **Integration Guides**: Spring Boot and reactive programming patterns

## Previous Versions

### [1.x] - Legacy Version
- Initial D-Bus client implementation
- Basic message handling capabilities
- Unix Domain Socket transport support
- Foundation for current architecture

---

## Migration Guides

### Migrating to 2.0

#### Breaking Changes
- Package restructuring for better organization
- Updated API for connection management
- Enhanced error handling patterns
- SASL authentication improvements

#### Migration Steps
1. **Update Dependencies**: Update to version 2.0 in your build configuration
2. **Package Imports**: Update package imports to reflect new structure
3. **Connection API**: Migrate to new connection configuration API
4. **Error Handling**: Update exception handling to use new exception hierarchy
5. **SASL Configuration**: Review and update authentication configurations

#### Example Migration
```java
// Before (1.x)
Connection connection = DBusConnection.create();

// After (2.0)
Connection connection = NettyConnection.newSessionBusConnection();
```

### Compatibility Matrix

| Library Version | Java Version | D-Bus Specification | Test Coverage | Spring Boot |
|----------------|--------------|-------------------|---------------|-------------|
| 2.0.0          | 17+          | 0.35+             | 83%           | 3.x         |
| 2.0-SNAPSHOT   | 17+          | 0.35              | 68%           | 3.x         |
| 1.x            | 11+          | 0.32              | ~60%          | 2.x         |

---

## Support

### Getting Help
- **Documentation**: [Developer Guide](docs/developer-guide.md)
- **Examples**: [Working Examples](examples/README.md)
- **Architecture**: [Architecture Documentation](docs/architecture/)
- **Issues**: [GitHub Issues](https://github.com/lucimber/dbus-client-java/issues)

### Contributing
- **Guidelines**: [Contributing Guide](CONTRIBUTING.md)
- **Code of Conduct**: [Code of Conduct](CODE_OF_CONDUCT.md)
- **Security**: [Security Policy](SECURITY.md)

### Versioning Policy
- **Major**: Breaking API changes, architectural changes
- **Minor**: New features, enhancements, deprecated APIs
- **Patch**: Bug fixes, documentation updates, security patches

---

*This changelog is automatically updated with each release. For development changes, see the [git commit history](https://github.com/lucimber/dbus-client-java/commits).*