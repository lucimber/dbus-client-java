# Changelog

All notable changes to the D-Bus Client Java library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0] - 2025-07-24

### Added

#### Core Features
- **High-Level Abstractions**
  - `ServiceProxy` for client-side dynamic proxy generation
  - `DBusPromise` for promise-style asynchronous operations
  - Annotation-based interface mapping with `@DBusInterface` and `@DBusMethod`

- **Type System Improvements**
  - Factory methods for common D-Bus types (`DBusArray.ofStrings()`, `DBusDict.ofStringToString()`, etc.)
  - Conversion methods from Java collections (`DBusDict.fromStringMap()`)
  - Simplified type creation without manual signature handling

- **Connection & Communication**
  - `sendRequest()` method with automatic request-response correlation
  - Comprehensive connection health monitoring with event-driven architecture
  - Automatic reconnection with exponential backoff
  - Configurable timeout system for all operations
  - `DummyConnection` for unit testing without D-Bus daemon

- **Transport & Architecture**
  - Strategy pattern for pluggable transport support (Unix sockets, TCP)
  - Factory pattern for streamlined encoder/decoder creation
  - Builder pattern for all message types
  - Dual-pipeline architecture with thread isolation (RealityCheckpoint)

- **Standard D-Bus Interfaces**
  - `StandardInterfaceHandler` for server-side D-Bus service implementation
  - Automatic implementation of Introspectable, Properties, and Peer interfaces
  - Annotation-based property and method exposure

- **Documentation & Examples**
  - Comprehensive developer guide with structured learning path
  - Platform setup guide for Windows, macOS, and Linux
  - Client-server patterns documentation
  - Migration guide from 1.x to 2.0
  - Docker support for cross-platform development
  - Extensive code examples demonstrating all major features

- **Testing Infrastructure**
  - Container-based integration testing with cross-platform SASL support
  - Memory-intensive test tagging system with configurable heap allocation
  - Testcontainers framework integration
  - Performance benchmarks for memory and throughput
  - Test coverage increased from 68% to 83%

- **Development Tools**
  - Spotless plugin for automatic code formatting
  - Enhanced Checkstyle and PMD configurations
  - SPDX license headers with automated management
  - Versioned documentation system with GitHub Pages
  - GitHub Actions workflows with comprehensive caching

### Changed

#### Breaking Changes
- **Maven Coordinates**: `com.lucimber:dbus-client` → `com.lucimber:lucimber-dbus-client`
- **Java Version**: Now requires Java 17 (upgraded from Java 11)
- **Connection API**: `DBusConnection.create()` → `NettyConnection.newSessionBusConnection()`
- **Message Construction**: All messages now require builder pattern instead of constructors
- **Package Structure**:
  - Merged encoder/decoder packages into unified `codec` package
  - Merged `impl` package into `annotation` package
  - Unified pipeline interfaces and removed legacy implementations

#### Architecture Improvements
- **Threading Model**: Introduced dual-pipeline architecture with safe blocking operations
- **SASL Configuration**: Type-safe configuration classes replacing string-based config
- **Handler Architecture**: Centralized handler configuration for reconnection support
- **Error Handling**: Enhanced exception handling with detailed error messages
- **Performance**: Optimized message encoding/decoding with reduced memory allocations

#### Documentation Updates
- Reorganized documentation structure with guides consolidated in `docs/guides/`
- Professional tone with selective emoji usage
- Added "Getting Started" sections to all package-info.java files
- Separated development and integration test Docker configurations

### Fixed

#### Critical Fixes
- **Security Vulnerabilities**:
  - Fixed buffer overflow risks in FrameDecoder
  - Fixed path traversal vulnerability in CookieSaslMechanism
  - Fixed information disclosure in SASL authentication
  - Upgraded Netty to 4.2.3.Final for security patches

- **Connection & Authentication**:
  - Resolved D-Bus SASL authentication failures in integration tests
  - Fixed race conditions in connection close operations
  - Fixed thread-safe handler initialization in NettyConnection
  - Corrected byte order handling in message body decoding

- **Test Reliability**:
  - Fixed integration test failure reporting
  - Resolved connection timeout test reliability issues
  - Fixed Java compilation encoding issues in Docker containers
  - Corrected exit code handling for failed integration tests

- **Code Quality**:
  - Resolved all major Checkstyle violations
  - Fixed PMD violations and suppressed intentional patterns
  - Fixed Javadoc generation errors
  - Resolved Gradle build errors and deprecation warnings

### Removed
- Redundant `test-container.sh` script (functionality moved to Gradle tasks)
- Security workflow for OWASP dependency check
- Unused SASL configuration classes
- Legacy implementation packages

### Dependencies
- **Runtime**:
  - Netty: 4.1.115.Final → 4.2.3.Final
  - SLF4J API: 2.0.11 → 2.0.17
  - JNA: 5.14.0 → 5.17.0
  - Replaced Log4j with Logback (1.5.18)

- **Test**:
  - JUnit: 5.12.0 → 5.13.3
  - Mockito: → 5.8.0
  - Awaitility: 4.2.2 → 4.3.0
  - Testcontainers: → 1.21.3

## [1.x] - Previous Releases

For changes in 1.x releases, please refer to the git history or previous documentation.