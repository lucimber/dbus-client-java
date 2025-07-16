# Transport Usage Guide

This guide demonstrates how to use the different transport options available in the D-Bus Client Java library.

## Overview

The library uses the Strategy pattern to support multiple transport types in a pluggable way. Transport selection is automatic based on the socket address type you provide.

## Supported Transports

### 1. Unix Domain Sockets (Recommended for Local Connections)

Unix domain sockets provide the best performance for local D-Bus connections and are the standard transport for D-Bus on Linux systems.

#### Factory Methods (Recommended)

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.netty.NettyConnection;

// System bus connection (typically /var/run/dbus/system_bus_socket)
Connection systemBus = NettyConnection.newSystemBusConnection();

// Session bus connection (uses DBUS_SESSION_BUS_ADDRESS environment variable)
Connection sessionBus = NettyConnection.newSessionBusConnection();
```

#### Manual Socket Path

```java
import io.netty.channel.unix.DomainSocketAddress;

// Custom Unix domain socket path
Connection customSocket = new NettyConnection(
    new DomainSocketAddress("/path/to/custom/dbus.socket")
);
```

#### Platform Support

- **Linux**: Uses Epoll native transport for optimal performance
- **macOS**: Uses KQueue native transport for optimal performance  
- **Other platforms**: Automatic fallback (though Unix sockets may not be available)

### 2. TCP/IP Connections (For Remote or Container Environments)

TCP connections are useful for remote D-Bus servers or containerized environments where Unix sockets aren't available.

```java
import java.net.InetSocketAddress;

// Remote D-Bus server
Connection remoteConnection = new NettyConnection(
    new InetSocketAddress("dbus-server.example.com", 12345)
);

// Local TCP connection (useful in containers)
Connection localTcp = new NettyConnection(
    new InetSocketAddress("localhost", 12345)
);

// IPv6 support
Connection ipv6Connection = new NettyConnection(
    new InetSocketAddress("::1", 12345)
);
```

## Automatic Transport Selection

The library automatically selects the appropriate transport strategy:

```java
// This will use NettyUnixSocketStrategy
Connection unixConn = new NettyConnection(
    new DomainSocketAddress("/tmp/dbus-test")
);

// This will use NettyTcpStrategy  
Connection tcpConn = new NettyConnection(
    new InetSocketAddress("localhost", 8080)
);
```

You can see which transport is being used in the logs:
```
INFO  c.l.dbus.netty.NettyConnection - Using transport strategy: Epoll (Unix Domain Socket)
INFO  c.l.dbus.netty.NettyConnection - Using transport strategy: NIO (TCP/IP)
```

## Connection Configuration

Both transport types support the same configuration options:

```java
import com.lucimber.dbus.connection.ConnectionConfig;
import java.time.Duration;

ConnectionConfig config = ConnectionConfig.builder()
    .withConnectTimeout(Duration.ofSeconds(30))
    .withMethodCallTimeout(Duration.ofSeconds(60))
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(30))
    .withAutoReconnectEnabled(true)
    .withMaxReconnectAttempts(5)
    .build();

Connection connection = new NettyConnection(address, config);
```

## Transport-Specific Optimizations

### Unix Domain Socket Optimizations

- **Native Event Loops**: Epoll (Linux) and KQueue (macOS) for lower latency
- **Zero-copy Operations**: Where supported by the platform
- **EXTERNAL Authentication**: Leverages Unix credentials for secure authentication

### TCP Connection Optimizations

- **TCP_NODELAY**: Disabled Nagle's algorithm for lower latency
- **SO_KEEPALIVE**: Automatic connection health monitoring
- **SO_REUSEADDR**: Allow address reuse for quick reconnection
- **Connection Timeout**: Configurable connection establishment timeout

## Error Handling

### Transport Unavailable

```java
try {
    Connection connection = new NettyConnection(
        new DomainSocketAddress("/nonexistent/socket")
    );
} catch (IllegalArgumentException e) {
    // No strategy available for this address type
    System.err.println("Transport not supported: " + e.getMessage());
}
```

### Platform Limitations

```java
// This might throw UnsupportedOperationException on Windows
try {
    Connection connection = new NettyConnection(
        new DomainSocketAddress("/tmp/dbus-socket")
    );
} catch (UnsupportedOperationException e) {
    // Unix Domain Sockets require native transport
    // Fall back to TCP if available
    Connection fallback = new NettyConnection(
        new InetSocketAddress("localhost", 12345)
    );
}
```

## Best Practices

### 1. Use Factory Methods When Possible

```java
// Preferred: Let the library handle socket paths
Connection systemBus = NettyConnection.newSystemBusConnection();

// Only use manual paths when necessary
Connection custom = new NettyConnection(
    new DomainSocketAddress("/custom/path")
);
```

### 2. Handle Platform Differences

```java
public Connection createConnection() {
    // Try Unix socket first for local connections
    if (isLocalConnection() && supportsUnixSockets()) {
        return NettyConnection.newSystemBusConnection();
    }
    
    // Fall back to TCP
    return new NettyConnection(
        new InetSocketAddress("localhost", 12345)
    );
}
```

### 3. Configure Appropriate Timeouts

```java
ConnectionConfig config = ConnectionConfig.builder()
    // Shorter timeout for local connections
    .withConnectTimeout(Duration.ofSeconds(5))
    // Longer timeout for remote connections  
    .withConnectTimeout(Duration.ofSeconds(30))
    .build();
```

### 4. Use Health Monitoring for Long-lived Connections

```java
ConnectionConfig config = ConnectionConfig.builder()
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(30))
    .withAutoReconnectEnabled(true)
    .build();
```

## Testing Different Transports

### Unit Testing with Mock Strategies

```java
// Custom registry for testing
ConnectionStrategyRegistry testRegistry = new ConnectionStrategyRegistry();
testRegistry.registerStrategy(new MockUnixSocketStrategy());
testRegistry.registerStrategy(new MockTcpStrategy());
```

### Integration Testing

The library provides container-based integration tests that work across platforms:

```bash
# Test both Unix and TCP transports
./gradlew integrationTestContainer
```

## Extending with Custom Transports

The strategy pattern makes it easy to add new transport types:

```java
public class WebSocketStrategy implements ConnectionStrategy {
    @Override
    public boolean supports(SocketAddress address) {
        return address instanceof WebSocketAddress;
    }
    
    @Override
    public CompletionStage<ConnectionHandle> connect(
            SocketAddress address, 
            ConnectionConfig config, 
            ConnectionContext context) {
        // WebSocket-specific connection logic
        return connectWebSocket(address, config, context);
    }
    
    @Override
    public String getTransportName() {
        return "WebSocket";
    }
    
    @Override
    public boolean isAvailable() {
        return WebSocketClient.isAvailable();
    }
}
```

See the [Transport Strategies Architecture](../architecture/transport-strategies.md) documentation for detailed information about implementing custom transport strategies.