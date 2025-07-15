# Architecture Overview

The D-Bus Client Java library is designed as a high-performance, asynchronous D-Bus client implementation built on Netty. This document provides a comprehensive overview of the architectural decisions, design patterns, and component interactions that make up the library.

## Design Philosophy

### Core Principles

1. **Asynchronous by Design**: All operations return `CompletableFuture` to avoid blocking threads
2. **Type Safety**: Strong typing prevents D-Bus marshalling errors at compile time
3. **Extensibility**: Handler-based pipeline allows custom message processing
4. **Performance**: Built on Netty for high-performance networking
5. **Cross-Platform**: Works reliably across different operating systems

### Quality Attributes

- **Reliability**: Comprehensive error handling and connection management
- **Performance**: Native transport optimization and efficient memory usage
- **Maintainability**: Clear separation of concerns and consistent patterns
- **Testability**: Comprehensive test suite with multiple test categories
- **Usability**: Clean APIs with sensible defaults

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│                    D-Bus Client API                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Connection  │  │  Messages   │  │    Type System      │  │
│  │ Management  │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Handler Pipeline                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Inbound   │  │  User       │  │     Outbound        │  │
│  │  Handlers   │  │ Handlers    │  │    Handlers         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                  Message Processing                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Encoders   │  │  Decoders   │  │   Message Types     │  │
│  │             │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                   Transport Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    Netty    │  │    SASL     │  │     Framing         │  │
│  │ Transport   │  │    Auth     │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Network Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Unix Domain │  │  TCP/IP     │  │   Native Epoll      │  │
│  │   Sockets   │  │  Sockets    │  │     (Linux)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Connection Management (`com.lucimber.dbus.connection`)

**Purpose**: Manages the lifecycle of D-Bus connections and provides a pipeline-based architecture for message processing.

**Key Classes**:
- `Connection`: Core interface defining connection operations
- `NettyConnection`: Primary implementation using Netty
- `ConnectionConfig`: Configuration builder for connection parameters
- `Pipeline`: Handler chain for message processing

**Design Patterns**:
- **Factory Pattern**: Connection creation through static factory methods
- **Builder Pattern**: Configuration construction
- **Observer Pattern**: Connection event listeners

### 2. Message System (`com.lucimber.dbus.message`)

**Purpose**: Represents all D-Bus message types with proper type safety and builder patterns.

**Message Hierarchy**:
```
Message (interface)
├── InboundMessage (interface)
│   ├── InboundMethodCall
│   ├── InboundMethodReturn  
│   ├── InboundSignal
│   └── InboundError
└── OutboundMessage (interface)
    ├── OutboundMethodCall
    ├── OutboundMethodReturn
    ├── OutboundSignal
    └── OutboundError
```

**Design Patterns**:
- **Builder Pattern**: Message construction
- **Abstract Factory**: Message type creation
- **Type Object**: Message metadata handling

### 3. Type System (`com.lucimber.dbus.type`)

**Purpose**: Provides type-safe wrappers for all D-Bus types, preventing marshalling errors.

**Type Hierarchy**:
```
DBusType (interface)
├── DBusBasicType (interface)
│   ├── DBusBoolean, DBusByte
│   ├── DBusInt16, DBusInt32, DBusInt64
│   ├── DBusUInt16, DBusUInt32, DBusUInt64
│   ├── DBusDouble, DBusString
│   ├── DBusObjectPath, DBusSignature
│   └── DBusUnixFD
└── DBusContainerType (interface)
    ├── DBusArray<T>
    ├── DBusStruct
    ├── DBusDict<K,V>
    ├── DBusDictEntry<K,V>
    └── DBusVariant
```

**Design Patterns**:
- **Wrapper Pattern**: Encapsulating Java types
- **Generic Types**: Type-safe containers
- **Value Object**: Immutable data containers

### 4. Encoding/Decoding (`com.lucimber.dbus.encoder`, `com.lucimber.dbus.decoder`)

**Purpose**: Handles D-Bus wire protocol serialization and deserialization.

**Key Features**:
- **Byte Order Support**: Both little-endian and big-endian
- **Type-Specific Codecs**: Each D-Bus type has dedicated encoder/decoder
- **Alignment Handling**: Proper D-Bus alignment requirements
- **Error Handling**: Comprehensive validation and error reporting

**Design Patterns**:
- **Strategy Pattern**: Different encoders for different types
- **Template Method**: Common encoding/decoding patterns
- **Factory Method**: Encoder/decoder instantiation

### 5. Transport Layer (`com.lucimber.dbus.netty`)

**Purpose**: Provides the Netty-based networking implementation with SASL authentication.

**Key Components**:
- **NettyConnection**: Main connection implementation
- **Channel Pipeline**: Netty handler chain setup
- **Frame Encoding/Decoding**: D-Bus message framing
- **SASL Authentication**: Multiple authentication mechanisms

**Design Patterns**:
- **Pipeline Pattern**: Netty channel pipeline
- **Handler Pattern**: Individual pipeline handlers
- **State Machine**: SASL authentication flow

## Pipeline Architecture

The heart of the library is the handler pipeline, inspired by Netty's design but adapted for D-Bus message processing.

### Handler Types

1. **Inbound Handlers**: Process messages coming from the D-Bus daemon
2. **Outbound Handlers**: Process messages being sent to the D-Bus daemon
3. **Duplex Handlers**: Handle both inbound and outbound messages

### Pipeline Flow

```
Inbound Flow:
Network → Frame Decoder → Message Decoder → User Handlers → Application

Outbound Flow:
Application → User Handlers → Message Encoder → Frame Encoder → Network
```

### Handler Lifecycle

1. **Handler Added**: Handler is added to the pipeline
2. **Channel Active**: Connection is established
3. **Message Processing**: Handle inbound/outbound messages
4. **Exception Handling**: Process any errors
5. **Channel Inactive**: Connection is closed
6. **Handler Removed**: Handler is removed from pipeline

## Authentication Architecture

### SASL Mechanisms

The library supports multiple SASL authentication mechanisms:

1. **EXTERNAL**: Unix credential-based authentication
2. **DBUS_COOKIE_SHA1**: Cookie-based authentication
3. **ANONYMOUS**: Anonymous authentication (limited scenarios)

### Authentication Flow

```
1. Connection Establishment
2. SASL Initiation
3. Mechanism Negotiation
4. Authentication Exchange
5. Authentication Success/Failure
6. D-Bus Hello Message
7. Ready for Message Exchange
```

## Error Handling Strategy

### Exception Hierarchy

All D-Bus errors inherit from `AbstractException` and map to standard D-Bus error names:

- **Network Errors**: Connection, timeout, I/O issues
- **Authentication Errors**: SASL authentication failures  
- **Protocol Errors**: Invalid messages, unknown methods
- **Application Errors**: Business logic errors from D-Bus services

### Error Propagation

1. **Transport Errors**: Handled at the Netty level
2. **Protocol Errors**: Converted to appropriate D-Bus error messages
3. **Application Errors**: Propagated as D-Bus error replies
4. **Connection Errors**: Trigger reconnection logic (if enabled)

## Performance Considerations

### Async Operations

- All blocking operations return `CompletableFuture`
- No thread blocking in the main message processing path
- Efficient event loop utilization

### Memory Management

- Object pooling for frequently allocated objects
- Efficient byte buffer management
- Memory-intensive tests ensure proper resource handling

### Native Transport

- **Linux**: Uses Epoll for high-performance networking
- **macOS**: Uses KQueue for optimal performance
- **Windows**: Falls back to NIO with good performance

## Configuration and Extensibility

### Connection Configuration

```java
ConnectionConfig config = ConnectionConfig.builder()
    .withAutoReconnectEnabled(true)
    .withReconnectInitialDelay(Duration.ofSeconds(1))
    .withMaxReconnectAttempts(10)
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(30))
    .withConnectTimeout(Duration.ofSeconds(10))
    .withMethodCallTimeout(Duration.ofSeconds(30))
    .build();
```

### Handler Extension

```java
// Custom inbound handler
public class CustomInboundHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // Custom processing
        if (shouldProcess(msg)) {
            processMessage(msg);
        }
        ctx.propagateInboundMessage(msg);
    }
}

// Add to pipeline
connection.getPipeline().addLast("custom", new CustomInboundHandler());
```

## Testing Strategy

The architecture supports comprehensive testing through multiple approaches:

1. **Unit Tests**: Component-level testing with mocking
2. **Integration Tests**: Full D-Bus communication testing
3. **Performance Tests**: Benchmarking and load testing
4. **Chaos Tests**: Resilience under adverse conditions
5. **Memory Tests**: Large data structure handling

## Deployment Considerations

### Production Deployment

- **Health Monitoring**: Built-in connection health checks
- **Graceful Shutdown**: Proper resource cleanup
- **Error Recovery**: Automatic reconnection with backoff
- **Logging**: Comprehensive logging with configurable levels

### Scalability

- **Connection Pooling**: Future enhancement for high-load scenarios
- **Message Batching**: Potential optimization for bulk operations
- **Resource Management**: Efficient memory and thread usage

This architecture provides a solid foundation for D-Bus communication in Java applications while maintaining flexibility for future enhancements and customizations.