# Changelog

All notable changes to the D-Bus Client Java library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive test coverage for SASL authentication classes
- Complete test coverage for utility package classes  
- Full test coverage for exception package (31 exception classes)
- Builder pattern support for inbound message construction
- Strategy pattern implementation for pluggable transport support
- Factory pattern implementation for encoders and decoders
- Request-response correlation support in `sendRequest()` method
- Integration test improvements with better error handling
- Performance monitoring and memory benchmark tests
- SPDX license headers across all source files

### Changed
- Improved integration test stability and reliability
- Enhanced error messaging for failed D-Bus operations
- Refactored switch statements to use factory pattern
- Optimized message encoding/decoding performance
- Updated PMD configuration to suppress intentional GC calls

### Fixed
- Major Checkstyle violations across the codebase
- Integration test failures related to message correlation
- `sendRequest()` method implementation with proper request-response handling
- Javadoc generation errors and documentation issues
- Missing serial numbers in test method calls
- Docker disk space issues in CI/CD pipeline

### Security
- Enhanced SASL authentication mechanism testing
- Improved error handling for authentication failures
- Better validation of D-Bus message integrity

## [2.0-SNAPSHOT] - Current Development

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

### Testing & Quality
- **Integration Testing**: Docker-based testing with real D-Bus daemon
- **Code Coverage**: 68% overall coverage with 100% coverage for critical components
- **Code Quality**: PMD, Checkstyle, and SpotBugs integration
- **Performance Testing**: Memory usage and throughput benchmarks

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

| Library Version | Java Version | D-Bus Specification | Spring Boot |
|----------------|--------------|-------------------|-------------|
| 2.0-SNAPSHOT   | 17+          | 0.35              | 3.x         |
| 1.x            | 11+          | 0.32              | 2.x         |

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