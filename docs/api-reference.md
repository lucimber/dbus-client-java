# API Reference Guide

This guide provides an overview of the D-Bus Client Java API, highlighting key classes, interfaces, and patterns for effective use of the library.

## 📖 API Documentation

### Generated Documentation
- **[Complete Javadoc API](../lib/build/docs/javadoc/index.html)** - Full API documentation (generated with `./gradlew javadoc`)
- **[Package Overview](../lib/build/docs/javadoc/overview-summary.html)** - Package structure and dependencies

## 🚀 Core API Overview

### Primary Entry Points

| Class | Package | Purpose | Example |
|-------|---------|---------|---------|
| `NettyConnection` | `com.lucimber.dbus.netty` | Main connection factory | `NettyConnection.newSessionBusConnection()` |
| `ConnectionConfig` | `com.lucimber.dbus.connection` | Connection configuration | `ConnectionConfig.builder().build()` |
| `OutboundMethodCall` | `com.lucimber.dbus.message` | Method call builder | `OutboundMethodCall.Builder.create()` |

### Key Interfaces

| Interface | Package | Purpose |
|-----------|---------|---------|
| `Connection` | `com.lucimber.dbus.connection` | Main connection interface |
| `Pipeline` | `com.lucimber.dbus.connection` | Message processing pipeline |
| `Handler` | `com.lucimber.dbus.connection` | Message processing handlers |
| `Message` | `com.lucimber.dbus.message` | Base message interface |

---

## 🏗️ Package Structure

### Core Packages

#### `com.lucimber.dbus.connection`
**Primary connection and pipeline management**

Key Classes:
- `Connection` - Main API for D-Bus connections
- `Pipeline` - Message processing pipeline
- `AbstractInboundHandler` - Base class for inbound message handlers
- `AbstractOutboundHandler` - Base class for outbound message handlers
- `ConnectionConfig` - Configuration for connections
- `ConnectionEvent` - Connection lifecycle events

**Usage Pattern:**
```java
Connection connection = NettyConnection.newSessionBusConnection();
connection.getPipeline().addLast("handler", new MyHandler());
connection.connect().thenRun(() -> {
    // Connection ready
});
```

#### `com.lucimber.dbus.netty`
**Netty-based transport implementation**

Key Classes:
- `NettyConnection` - Main Netty-based connection implementation
- `NettyConnectionHandle` - Low-level connection handle
- `RealityCheckpoint` - Bridge between pipelines
- `NettyTcpStrategy` - TCP transport strategy
- `NettyUnixSocketStrategy` - Unix socket transport strategy

**Usage Pattern:**
```java
// Session bus
Connection connection = NettyConnection.newSessionBusConnection();

// System bus
Connection connection = NettyConnection.newSystemBusConnection();

// Custom address
SocketAddress address = new InetSocketAddress("localhost", 12345);
Connection connection = new NettyConnection(address);
```

#### `com.lucimber.dbus.message`
**D-Bus message types and builders**

Key Classes:
- `OutboundMethodCall` - Method call messages
- `InboundMethodReturn` - Method return messages
- `InboundSignal` - Signal messages
- `InboundError` - Error messages
- `MessageType` - Message type enumeration
- `HeaderField` - Message header fields

**Usage Pattern:**
```java
OutboundMethodCall call = OutboundMethodCall.Builder
    .create()
    .withSerial(DBusUInt32.valueOf(1))
    .withPath(DBusObjectPath.valueOf("/org/example/Object"))
    .withMember(DBusString.valueOf("Method"))
    .withInterface(DBusString.valueOf("org.example.Interface"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .build();
```

#### `com.lucimber.dbus.type`
**D-Bus type system implementation**

Key Classes:
- `DBusString` - D-Bus string type
- `DBusUInt32` - D-Bus unsigned 32-bit integer
- `DBusObjectPath` - D-Bus object path
- `DBusArray` - D-Bus array type
- `DBusSignature` - D-Bus signature type
- `DBusVariant` - D-Bus variant type

**Usage Pattern:**
```java
DBusString string = DBusString.valueOf("Hello World");
DBusUInt32 number = DBusUInt32.valueOf(42);
DBusObjectPath path = DBusObjectPath.valueOf("/org/example/Object");
DBusArray<DBusString> array = DBusArray.valueOf(
    DBusString.valueOf("item1"),
    DBusString.valueOf("item2")
);
```

### Specialized Packages

#### `com.lucimber.dbus.netty.sasl`
**SASL authentication mechanisms**

Key Classes:
- `ExternalSaslMechanism` - Unix credentials authentication
- `AnonymousSaslMechanism` - Anonymous authentication
- `CookieSaslMechanism` - Cookie-based authentication
- `SaslAuthenticationHandler` - Authentication handler

#### `com.lucimber.dbus.encoder` / `com.lucimber.dbus.decoder`
**Message encoding and decoding**

Key Classes:
- `EncoderFactory` / `DecoderFactory` - Factory interfaces
- `DefaultEncoderFactory` / `DefaultDecoderFactory` - Default implementations
- Type-specific encoders/decoders for each D-Bus type

#### `com.lucimber.dbus.exception`
**D-Bus exception hierarchy**

Key Classes:
- `AbstractException` - Base D-Bus exception
- Standard D-Bus error exceptions (31 total)
- `InvalidMessageException` - Message validation errors

---

## 🎯 Common Usage Patterns

### 1. Basic Connection Setup
```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.netty.NettyConnection;

Connection connection = NettyConnection.newSessionBusConnection();
connection.connect().toCompletableFuture().get();
// Use connection...
connection.close();
```

### 2. Method Call with Response
```java
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;

OutboundMethodCall call = OutboundMethodCall.Builder.create()
    .withSerial(connection.getNextSerial())
    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
    .withMember(DBusString.valueOf("ListNames"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
    .withReplyExpected(true)
    .build();

CompletableFuture<InboundMessage> response = connection.sendRequest(call);
InboundMessage result = response.get();
```

### 3. Signal Handling
```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;

connection.getPipeline().addLast("signal-handler", new AbstractInboundHandler() {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundSignal) {
            InboundSignal signal = (InboundSignal) msg;
            System.out.println("Received signal: " + signal.getMember());
        }
        ctx.propagateInboundMessage(msg);
    }
});
```

### 4. Custom Handler with Error Handling
```java
connection.getPipeline().addLast("business-logic", new AbstractInboundHandler() {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // ✅ Blocking operations are safe here!
            String result = performDatabaseOperation(msg);
            InboundMessage enriched = enrichMessage(msg, result);
            ctx.propagateInboundMessage(enriched);
        } catch (Exception e) {
            ctx.propagateExceptionCaught(e);
        }
    }
    
    @Override
    public void handleExceptionCaught(Context ctx, Throwable cause) {
        logger.error("Handler error", cause);
        ctx.propagateExceptionCaught(cause);
    }
});
```

### 5. Configuration with Timeouts
```java
import com.lucimber.dbus.connection.ConnectionConfig;
import java.time.Duration;

ConnectionConfig config = ConnectionConfig.builder()
    .withConnectTimeout(Duration.ofSeconds(10))
    .withMethodCallTimeout(Duration.ofSeconds(30))
    .withAutoReconnectEnabled(true)
    .withHealthCheckEnabled(true)
    .build();

Connection connection = NettyConnection.newSessionBusConnection(config);
```

---

## 🔧 Advanced API Usage

### Threading Model
- **Connection Methods**: Thread-safe, can be called from any thread
- **Pipeline Operations**: Thread-safe for adding/removing handlers
- **Handler Methods**: Called on dedicated thread pools (blocking operations safe)
- **Message Building**: Thread-safe (immutable builders)

### Memory Management
- **Connection**: Call `connection.close()` to release resources
- **Messages**: Automatically managed, no manual cleanup needed
- **Handlers**: Implement `dispose()` if holding external resources

### Error Handling
- **Checked Exceptions**: Method signatures declare specific exceptions
- **Runtime Exceptions**: Indicate programming errors or system issues
- **D-Bus Errors**: Converted to specific exception types from `com.lucimber.dbus.exception`

---

## 📚 API Documentation Navigation

### By Use Case

| Use Case | Primary Classes | Documentation |
|----------|----------------|---------------|
| **Basic Connection** | `NettyConnection`, `ConnectionConfig` | [Connection Guide](guides/quick-start.md) |
| **Message Handling** | `OutboundMethodCall`, `InboundMessage` | [Message Operations](guides/quick-start.md#message-operations) |
| **Signal Processing** | `InboundSignal`, `AbstractInboundHandler` | [Signal Example](examples/README.md#signal-handling) |
| **Authentication** | SASL mechanism classes | [Authentication Guide](guides/blocking-operations.md) |
| **Error Handling** | Exception package classes | [Error Handling Guide](guides/error-handling.md) |
| **Spring Integration** | All core classes | [Spring Boot Guide](guides/spring-boot-integration.md) |

### By Package

| Package | Documentation | Best For |
|---------|--------------|----------|
| `connection` | [Handler Architecture](architecture/handler-architecture.md) | Pipeline and handler development |
| `message` | [Message Flow](architecture/message-flow.md) | Understanding message structure |
| `type` | [Type System Examples](examples/README.md) | Working with D-Bus types |
| `netty` | [Transport Architecture](architecture/transport-strategies.md) | Transport customization |
| `exception` | [Error Handling](guides/error-handling.md) | Exception handling patterns |

---

## 🛠️ Development Tools

### IDE Integration
- **IntelliJ IDEA**: Import Gradle project, Javadoc available in tooltips
- **Eclipse**: Import as Gradle project, configure Javadoc location
- **VS Code**: Java Extension Pack provides IntelliSense support

### API Discovery
- **Javadoc**: `./gradlew javadoc` then open `lib/build/docs/javadoc/index.html`
- **IDE Navigation**: Ctrl+Click (IntelliJ) or F3 (Eclipse) on any API element
- **Search**: Use IDE search functionality to find classes by name

### Code Completion
All major IDEs provide code completion for:
- Method parameters and return types
- Available builder methods
- Exception types that can be thrown
- Interface implementations

---

## 📖 Additional Resources

### Generated Documentation
- **[Complete API Javadoc](../lib/build/docs/javadoc/index.html)** - Comprehensive API reference
- **[Package Index](../lib/build/docs/javadoc/allpackages-index.html)** - All packages overview
- **[Class Index](../lib/build/docs/javadoc/allclasses-index.html)** - All classes alphabetically

### Conceptual Documentation
- **[Architecture Overview](architecture/overview.md)** - High-level system design
- **[Pipeline Architecture](architecture/pipeline-event-architecture.md)** - Detailed pipeline design
- **[Handler Development](architecture/handler-architecture.md)** - Handler patterns and best practices

### Practical Guides
- **[Quick Start](guides/quick-start.md)** - Get started quickly
- **[Integration Patterns](guides/integration-patterns.md)** - Common integration approaches
- **[Error Handling](guides/error-handling.md)** - Robust error handling
- **[Testing Guide](testing-guide.md)** - Testing strategies

---

*This API reference is automatically updated with each release. The generated Javadoc provides complete method signatures, parameters, and detailed documentation for all public APIs.*