# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**IMPORTANT**: This file is for local development assistance only. It must NEVER be committed to version control. You must handle this manually by:
1. Checking git status before any commits
2. Never staging or committing this file
3. If accidentally staged, use `git reset HEAD CLAUDE.md` to unstage it
4. Do NOT add CLAUDE.md to .gitignore (this must be handled manually to avoid modifying project files)

**FOR CLAUDE CODE**: After conversation compaction or when starting a new session, ALWAYS read this file first to understand the codebase architecture, build commands, and project conventions. This file contains critical context that may be lost during compaction.

## Build Commands

### Standard Build and Test
```bash
# Full build with unit tests
./gradlew build

# Run unit tests only
./gradlew test

# Run a specific test class
./gradlew test --tests "com.lucimber.dbus.type.DBusStringTest"

# Run tests with memory-intensive tests enabled
./gradlew test -PwithMemoryIntensiveTests
```

### Integration Testing
```bash
# Run integration tests (containerized) - REQUIRED for D-Bus testing
./gradlew integrationTest

# Run with verbose output
./gradlew integrationTest -PshowOutput

# Run with debug logs
./gradlew integrationTest -Pdebug
```

### Code Quality
```bash
# Run checkstyle
./gradlew checkstyleMain checkstyleTest

# Run PMD static analysis
./gradlew pmdMain pmdTest

# Check copyright headers
./gradlew checkCopyright

# Update copyright headers
./gradlew updateCopyright

# Generate Javadocs
./gradlew javadoc
```

### Performance Testing
```bash
# Run performance tests
./gradlew performanceTest

# Run chaos engineering tests
./gradlew chaosTest
```

## Architecture Overview

### Dual-Pipeline Architecture
The library implements a sophisticated dual-pipeline architecture:

1. **Transport Layer (Netty Pipeline)**
   - Handles low-level D-Bus protocol operations
   - Runs on Netty EventLoop threads
   - Responsible for frame encoding/decoding, SASL authentication, and network I/O

2. **Application Layer (Public API Pipeline)**
   - Runs on dedicated thread pools (ApplicationTaskExecutor)
   - Safe for blocking operations
   - Processes business logic through custom handlers

3. **RealityCheckpoint**
   - The critical bridge between transport and application layers
   - Located at: `lib/src/main/java/com/lucimber/dbus/netty/RealityCheckpoint.java`
   - Manages request-response correlation
   - Routes messages from Netty EventLoop to application threads
   - Handles method call timeouts

### Key Components

#### Connection Strategies
The library uses the Strategy Pattern for different transport types:
- `NettyTcpStrategy` - TCP/IP connections
- `NettyUnixSocketStrategy` - Unix domain sockets
- Both create `NettyConnectionHandle` instances with `RealityCheckpoint`

#### Message Flow
1. **Outbound**: Application → OutboundHandler → RealityCheckpoint → Netty Pipeline → Network
2. **Inbound**: Network → Netty Pipeline → RealityCheckpoint → InboundHandler → Application

#### Request-Response Correlation
- `sendRequest()` method returns `CompletionStage<InboundMessage>`
- Correlation handled by `RealityCheckpoint` using serial numbers
- Pending calls tracked in `ConcurrentHashMap<DBusUInt32, PendingMethodCall>`

#### SASL Authentication
- Supports EXTERNAL and DBUS_COOKIE_SHA1 mechanisms
- Implemented in `netty.sasl` package
- Configurable via `SaslAuthConfig`

## Testing Requirements

### Integration Tests
- **MUST** run inside Docker container (`./gradlew integrationTest`)
- Direct execution will fail on macOS/Windows due to D-Bus daemon requirements
- Container provides proper D-Bus daemon with authentication

### Test Categories
- Unit tests: Standard JUnit tests
- Integration tests: Tagged with `@Tag("integration")`
- Performance tests: Tagged with `@Tag("performance")`
- Memory-intensive tests: Tagged with `@Tag("memory-intensive")`

## Important Patterns

### Builder Pattern for Messages
```java
OutboundMethodCall call = OutboundMethodCall.Builder
    .create()
    .withSerial(connection.getNextSerial())  // ALWAYS use getNextSerial()
    .withPath(DBusObjectPath.valueOf("/path"))
    .withMember(DBusString.valueOf("MethodName"))
    .withDestination(DBusString.valueOf("destination"))
    .withInterface(DBusString.valueOf("interface"))
    .withReplyExpected(true)
    .build();
```

### Factory Pattern for Encoders/Decoders
- Encoders created via `EncoderFactory`
- Decoders created via `DecoderFactory`
- Type-specific implementations for each D-Bus type

### Handler Development
- Extend `AbstractInboundHandler` or `AbstractOutboundHandler`
- Blocking operations are safe in handlers (run on ApplicationTaskExecutor)
- Use `Context` for message propagation

## Common Issues and Solutions

### Missing Serial Numbers
- Always use `connection.getNextSerial()` for message serial numbers
- Never hardcode or reuse serial numbers

### sendRequest() Implementation
- Returns `CompletionStage<InboundMessage>`
- Implementation delegates to `RealityCheckpoint.writeMessage()`
- Handles Netty Future to Java CompletionStage conversion

### Test Failures on Local Machine
- Integration tests MUST run in container
- Use `./gradlew integrationTest`, not direct test execution
- Container provides Linux environment with D-Bus daemon

## Code Conventions

### Copyright Headers
- All source files must have SPDX headers
- Format: `SPDX-FileCopyrightText: [YEAR] Lucimber UG`
- License: `SPDX-License-Identifier: Apache-2.0`
- Update with: `./gradlew updateCopyright`

### Thread Safety
- Transport operations run on Netty EventLoop
- User handlers run on ApplicationTaskExecutor
- Use `ConcurrentHashMap` for shared state
- Avoid blocking operations in Netty pipeline handlers

### D-Bus Type System
- All D-Bus types extend `DBusType`
- Immutable value objects
- Factory methods like `DBusString.valueOf()`
- Proper equals/hashCode implementations

## Git Commit Guidelines

### Commit Requirements
- **ALWAYS** use the `-s` flag to sign-off commits: `git commit -s`
- **NEVER** mention AI/Claude involvement in commit messages
- Follow conventional commit format (feat:, fix:, refactor:, test:, docs:, etc.)
- Keep commit messages concise and focused on the "why" rather than the "what"

### Example Commit Command
```bash
git commit -s -m "feat: implement sendRequest() method with request-response correlation

- Add sendRequest() implementation in NettyConnectionHandle
- Integrate with RealityCheckpoint for message correlation
- Update connection strategies to pass RealityCheckpoint instance"
```

### Before Committing
1. Check git status to ensure CLAUDE.md is not staged
2. Compile all code: `./gradlew compileJava compileTestJava`
3. Run tests to ensure changes work: `./gradlew test`
4. Run integration tests if applicable: `./gradlew integrationTest`
5. Run PMD static analysis: `./gradlew pmdMain pmdTest`
6. Ensure code follows style guidelines: `./gradlew checkstyleMain checkstyleTest`