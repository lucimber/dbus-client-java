# Transport Strategies

The D-Bus Client Java library uses the Strategy pattern to support multiple transport types in a pluggable and extensible manner. This architectural decision allows for clean separation of transport-specific logic while maintaining a unified connection interface.

## Overview

The transport strategy system consists of several key components:

- **ConnectionStrategy**: Abstract interface for transport implementations
- **ConnectionStrategyRegistry**: Registry that automatically selects appropriate strategies
- **ConnectionHandle**: Transport-agnostic connection handle
- **ConnectionContext**: Callback interface for strategy-connection communication

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    NettyConnection                          │
│  ┌─────────────────────────────────────────────────────────┤
│  │            ConnectionStrategyRegistry                   │
│  │  ┌─────────────────┐  ┌─────────────────────────────┐   │
│  │  │ Unix Socket     │  │      TCP Strategy           │   │
│  │  │ Strategy        │  │                             │   │
│  │  └─────────────────┘  └─────────────────────────────┘   │
│  └─────────────────────────────────────────────────────────┤
│  │            ConnectionHandle                             │
│  │  ┌─────────────────────────────────────────────────────┤
│  │  │        Netty Channel + EventLoopGroup               │
│  │  └─────────────────────────────────────────────────────┤
├─────────────────────────────────────────────────────────────┤
│                 Transport Layer                             │
│  ┌─────────────────┐  ┌─────────────────────────────────┐   │
│  │ Epoll/KQueue    │  │         NIO                     │   │
│  │ (Unix Sockets)  │  │      (TCP/IP)                   │   │
│  └─────────────────┘  └─────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Strategy Pattern Components

### ConnectionStrategy Interface

```java
public interface ConnectionStrategy {
  boolean supports(SocketAddress address);
  CompletionStage<ConnectionHandle> connect(SocketAddress address, 
                                          ConnectionConfig config, 
                                          ConnectionContext context);
  String getTransportName();
  boolean isAvailable();
}
```

The `ConnectionStrategy` interface provides a clean abstraction for different transport types:

- **`supports()`**: Determines if the strategy can handle a specific socket address type
- **`connect()`**: Establishes the connection and returns a transport-agnostic handle
- **`getTransportName()`**: Provides human-readable transport identification
- **`isAvailable()`**: Checks platform availability (e.g., native transports on specific OS)

### Available Strategies

#### NettyUnixSocketStrategy

Handles Unix domain socket connections with native transport optimization:

- **Platforms**: Linux (Epoll), macOS (KQueue)
- **Address Type**: `DomainSocketAddress`
- **Transport**: Native event loops for optimal performance
- **Use Cases**: Local D-Bus connections, system/session buses

```java
// Automatic selection for Unix socket
Connection connection = new NettyConnection(
    new DomainSocketAddress("/var/run/dbus/system_bus_socket")
);
```

#### NettyTcpStrategy

Handles TCP/IP connections for network-based D-Bus communication:

- **Platforms**: All platforms (NIO)
- **Address Type**: `InetSocketAddress`  
- **Transport**: NIO with TCP optimizations
- **Use Cases**: Remote D-Bus connections, container environments

```java
// Automatic selection for TCP
Connection connection = new NettyConnection(
    new InetSocketAddress("dbus-server.example.com", 12345)
);
```

## Strategy Selection

The `ConnectionStrategyRegistry` automatically selects the appropriate strategy based on the socket address type:

```java
private static ConnectionStrategyRegistry createDefaultStrategyRegistry() {
    ConnectionStrategyRegistry registry = new ConnectionStrategyRegistry();
    registry.registerStrategy(new NettyUnixSocketStrategy());
    registry.registerStrategy(new NettyTcpStrategy());
    return registry;
}
```

Selection process:
1. **Address Type Detection**: Registry examines the `SocketAddress` type
2. **Strategy Matching**: Finds strategies that support the address type
3. **Availability Check**: Verifies the strategy is available on current platform
4. **Automatic Selection**: Returns the first matching available strategy

## Connection Lifecycle

### 1. Strategy-Based Connection Establishment

```java
// In NettyConnection.connect()
return strategy.connect(serverAddress, config, context)
        .thenApply(handle -> {
            this.connectionHandle = handle;
            return null;
        });
```

### 2. Transport-Specific Handshake

Each strategy handles its transport-specific connection process:

- **Socket Connection**: Establish underlying transport connection
- **D-Bus Handshake**: SASL authentication and Hello exchange
- **Event Notification**: Notify context when fully established

### 3. ConnectionHandle Abstraction

The `ConnectionHandle` interface provides transport-agnostic operations:

```java
public interface ConnectionHandle {
    boolean isActive();
    CompletionStage<Void> send(OutboundMessage message);
    CompletionStage<InboundMessage> sendRequest(OutboundMessage message);
    DBusUInt32 getNextSerial();
    CompletionStage<Void> close();
    String getAssignedBusName();
}
```

## Extensibility

### Adding New Transport Types

The strategy pattern makes it easy to add new transport types:

```java
// Example: WebSocket strategy
public class NettyWebSocketStrategy implements ConnectionStrategy {
    @Override
    public boolean supports(SocketAddress address) {
        return address instanceof WebSocketAddress;
    }
    
    @Override
    public CompletionStage<ConnectionHandle> connect(SocketAddress address, 
                                                    ConnectionConfig config, 
                                                    ConnectionContext context) {
        // WebSocket-specific connection logic
    }
    
    // ... other methods
}

// Registration
registry.registerStrategy(new NettyWebSocketStrategy());
```

### Custom Strategy Registry

Applications can create custom registries with specific strategies:

```java
ConnectionStrategyRegistry customRegistry = new ConnectionStrategyRegistry();
customRegistry.registerStrategy(new CustomTransportStrategy());
customRegistry.registerStrategy(new NettyTcpStrategy());

// Use in custom connection class
```

## Benefits

### 1. **Separation of Concerns**
- Transport logic isolated from connection management
- Each strategy handles only its specific transport type
- Clean abstraction boundaries

### 2. **Extensibility**
- Easy to add new transport types
- No modification of existing connection logic required
- Plugin-like architecture

### 3. **Platform Independence**
- Core connection logic doesn't depend on specific transports
- Graceful handling of platform-specific features
- Automatic fallback mechanisms

### 4. **Testability**
- Each strategy can be tested independently
- Mock strategies for unit testing
- Clear interfaces for test verification

### 5. **Maintainability**
- Transport-specific changes isolated to respective strategies
- Consistent patterns across all transport types
- Reduced complexity in core connection logic

## Migration from Conditional Logic

The strategy pattern replaced the previous conditional transport selection:

### Before (Conditional Logic)
```java
// Old approach in constructor
if (serverAddress instanceof DomainSocketAddress) {
    if (Epoll.isAvailable()) {
        this.workerGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        this.channelClass = EpollDomainSocketChannel.class;
    } else if (KQueue.isAvailable()) {
        // ... more conditionals
    }
} else if (serverAddress instanceof InetSocketAddress) {
    // ... TCP logic
}
```

### After (Strategy Pattern)
```java
// New approach with strategy pattern
this.strategy = strategyRegistry.findStrategy(serverAddress)
        .orElseThrow(() -> new IllegalArgumentException("No strategy available"));
```

This change improves code maintainability, reduces complexity, and provides a foundation for future transport type additions.