# Quick Start Guide

This guide will get you up and running with the D-Bus Client Java library in 5 minutes. We'll create a simple application that connects to the D-Bus system bus and makes a basic method call.

## Prerequisites

- Java 17 or higher
- D-Bus daemon running (typically available on Linux systems)
- For testing on non-Linux systems: Docker installed

## Installation

### Gradle

```gradle
dependencies {
    implementation 'com.lucimber:lucimber-dbus-client:2.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>com.lucimber</groupId>
    <artifactId>lucimber-dbus-client</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```

## Your First D-Bus Connection

### Step 1: Create a Connection

The library automatically selects the appropriate transport strategy based on the connection type:

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.netty.NettyConnection;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;

public class QuickStartExample {
    public static void main(String[] args) throws Exception {
        // Option 1: System bus (Unix domain socket - automatic)
        Connection connection = NettyConnection.newSystemBusConnection();
        
        // Option 2: Manual Unix domain socket
        // Connection connection = new NettyConnection(
        //     new DomainSocketAddress("/var/run/dbus/system_bus_socket")
        // );
        
        // Option 3: TCP connection (for remote D-Bus or containers)
        // Connection connection = new NettyConnection(
        //     new InetSocketAddress("localhost", 12345)
        // );
        
        // Connect asynchronously
        connection.connect().toCompletableFuture().get();
        
        System.out.println("Connected to D-Bus!");
        
        // Don't forget to close the connection
        connection.close();
    }
}
```

### Step 2: Make Your First Method Call

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;

import java.util.concurrent.CompletableFuture;

public class FirstMethodCall {
    public static void main(String[] args) throws Exception {
        Connection connection = NettyConnection.newSystemBusConnection();
        
        try {
            // Connect to D-Bus
            connection.connect().toCompletableFuture().get();
            
            // Create a method call to get the D-Bus daemon ID
            OutboundMethodCall methodCall = OutboundMethodCall.Builder
                .create()
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("GetId"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
            
            // Send the method call and wait for response
            CompletableFuture<InboundMessage> response = connection.sendRequest(methodCall);
            InboundMessage reply = response.get();
            
            System.out.println("D-Bus daemon ID received: " + reply.getType());
            
        } finally {
            connection.close();
        }
    }
}
```

### Step 3: Handle Signals

```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;

public class SignalHandlerExample {
    public static void main(String[] args) throws Exception {
        Connection connection = NettyConnection.newSystemBusConnection();
        
        // Add a signal handler to the pipeline
        connection.getPipeline().addLast("signal-handler", new SignalHandler());
        
        try {
            connection.connect().toCompletableFuture().get();
            
            // Keep the connection alive to receive signals
            System.out.println("Listening for signals... Press Enter to exit.");
            System.in.read();
            
        } finally {
            connection.close();
        }
    }
    
    private static class SignalHandler extends AbstractInboundHandler {
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal) {
                InboundSignal signal = (InboundSignal) msg;
                System.out.println("Received signal: " + signal.getMember());
            }
            
            // Always propagate the message
            ctx.propagateInboundMessage(msg);
        }
    }
}
```

## Common Patterns

### Configuration with Builder

```java
import com.lucimber.dbus.connection.ConnectionConfig;
import java.time.Duration;

// Create a connection with custom configuration
ConnectionConfig config = ConnectionConfig.builder()
    .withAutoReconnectEnabled(true)
    .withReconnectInitialDelay(Duration.ofSeconds(1))
    .withMaxReconnectAttempts(5)
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(30))
    .withConnectTimeout(Duration.ofSeconds(10))
    .withMethodCallTimeout(Duration.ofSeconds(30))
    .build();

Connection connection = new NettyConnection(
    NettyConnection.getSystemBusAddress(),
    config
);
```

### Error Handling

```java
try {
    CompletableFuture<InboundMessage> response = connection.sendRequest(methodCall);
    InboundMessage reply = response.get(10, TimeUnit.SECONDS);
    
    if (reply instanceof InboundError) {
        InboundError error = (InboundError) reply;
        System.err.println("D-Bus error: " + error.getErrorName());
    } else {
        // Process successful response
        processResponse(reply);
    }
    
} catch (TimeoutException e) {
    System.err.println("Method call timed out");
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
}
```

### Working with D-Bus Types

```java
import com.lucimber.dbus.type.*;

// Basic types
DBusString text = DBusString.valueOf("Hello, D-Bus!");
DBusInt32 number = DBusInt32.valueOf(42);
DBusBoolean flag = DBusBoolean.valueOf(true);

// Arrays - NEW simplified factory methods (v2.0+)
DBusArray<DBusString> stringArray = DBusArray.ofStrings("first", "second", "third");
DBusArray<DBusInt32> intArray = DBusArray.ofInt32s(1, 2, 3, 4, 5);
DBusArray<DBusBoolean> boolArray = DBusArray.ofBooleans(true, false, true);

// Dictionaries - NEW simplified factory methods (v2.0+)
DBusDict<DBusString, DBusString> config = DBusDict.ofStringToString();
config.put(DBusString.valueOf("host"), DBusString.valueOf("localhost"));
config.put(DBusString.valueOf("port"), DBusString.valueOf("8080"));

// Convert from Java collections
Map<String, String> javaMap = Map.of("key1", "value1", "key2", "value2");
DBusDict<DBusString, DBusString> dbusDict = DBusDict.fromStringMap(javaMap);

// Properties dictionary (string->variant) - common pattern
DBusDict<DBusString, DBusVariant> properties = DBusDict.ofStringToVariant();
properties.put(DBusString.valueOf("Active"), DBusVariant.valueOf(DBusBoolean.valueOf(true)));
properties.put(DBusString.valueOf("Count"), DBusVariant.valueOf(DBusInt32.valueOf(42)));

// Legacy approach (still supported)
DBusSignature arraySignature = DBusSignature.valueOf("as");
DBusArray<DBusString> legacyArray = new DBusArray<>(arraySignature);
```

## Transport Options

The library supports multiple transport types through a pluggable strategy pattern:

### Unix Domain Sockets (Recommended for Local Connections)

```java
import io.netty.channel.unix.DomainSocketAddress;

// System bus
Connection systemBus = NettyConnection.newSystemBusConnection();

// Session bus  
Connection sessionBus = NettyConnection.newSessionBusConnection();

// Custom Unix socket
Connection customUnix = new NettyConnection(
    new DomainSocketAddress("/path/to/custom/socket")
);
```

**Benefits:**
- High performance with native transports (Epoll on Linux, KQueue on macOS)
- Standard D-Bus authentication (EXTERNAL mechanism)
- Lower latency than TCP

### TCP/IP Connections (For Remote or Container Environments)

```java
import java.net.InetSocketAddress;

// Remote D-Bus server
Connection remoteConnection = new NettyConnection(
    new InetSocketAddress("dbus-server.example.com", 12345)
);

// Local TCP (useful in containers)
Connection localTcp = new NettyConnection(
    new InetSocketAddress("localhost", 12345)
);
```

**Benefits:**
- Works across network boundaries
- Platform independent (uses NIO)
- Suitable for containerized environments

### Automatic Strategy Selection

The library automatically selects the appropriate transport strategy:

1. **Unix Domain Socket → NettyUnixSocketStrategy**
   - Uses native transports when available
   - Falls back gracefully if native transport unavailable

2. **TCP Address → NettyTcpStrategy** 
   - Uses NIO transport with TCP optimizations
   - Configures connection timeouts and keep-alive

## Testing Your Application

### Unit Testing

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

public class DBusClientTest {
    private Connection connection;
    
    @BeforeEach
    void setUp() throws Exception {
        // Use session bus for testing to avoid affecting system
        connection = NettyConnection.newSessionBusConnection();
        connection.connect().toCompletableFuture().get();
    }
    
    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
    }
    
    @Test
    void testBasicMethodCall() throws Exception {
        OutboundMethodCall call = OutboundMethodCall.Builder
            .create()
            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
            .withMember(DBusString.valueOf("GetId"))
            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
            .withReplyExpected(true)
            .build();
        
        InboundMessage response = connection.sendRequest(call)
                                           .toCompletableFuture()
                                           .get();
        
        assertNotNull(response);
    }
}
```

### Integration Testing with Docker

If you're running on a non-Linux system or want isolated testing:

```bash
# Use the provided container-based testing
./test-container.sh

# Or use Gradle task
./gradlew integrationTestContainer
```

## Common Issues and Solutions

### Issue: Connection Refused

```
java.net.ConnectException: Connection refused
```

**Solution**: Make sure the D-Bus daemon is running and accessible.

```bash
# Check if D-Bus is running
systemctl status dbus

# Check D-Bus socket permissions
ls -la /var/run/dbus/system_bus_socket
```

### Issue: Authentication Failed

```
AuthFailedException: SASL authentication failed
```

**Solution**: 
- On Linux: Ensure your user has permission to access D-Bus
- On other platforms: Use container-based testing

### Issue: Method Call Timeout

```
TimeoutException: Method call timed out
```

**Solution**: Increase timeout or check if the target service is available.

```java
ConnectionConfig config = ConnectionConfig.builder()
    .withMethodCallTimeout(Duration.ofMinutes(1))
    .build();
```

## Next Steps

Now that you have a basic D-Bus client working, explore these topics:

- **[Integration Patterns](integration-patterns.md)**: Common integration scenarios
- **[Handler Architecture](../architecture/handler-architecture.md)**: Custom message processing
- **[Spring Boot Integration](spring-boot-integration.md)**: Framework integration
- **[Error Handling](error-handling.md)**: Robust error handling patterns

## Complete Example

Here's a complete working example that demonstrates the key concepts:

```java
import com.lucimber.dbus.connection.*;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class CompleteExample {
    public static void main(String[] args) throws Exception {
        // 1. Create connection with configuration
        ConnectionConfig config = ConnectionConfig.builder()
            .withAutoReconnectEnabled(true)
            .withHealthCheckEnabled(true)
            .withConnectTimeout(Duration.ofSeconds(10))
            .build();
        
        Connection connection = new NettyConnection(
            NettyConnection.getSystemBusAddress(),
            config
        );
        
        // 2. Add custom handler
        connection.getPipeline().addLast("logger", new LoggingHandler());
        
        try {
            // 3. Connect
            connection.connect().toCompletableFuture().get();
            System.out.println("✅ Connected to D-Bus");
            
            // 4. Make method call
            OutboundMethodCall call = OutboundMethodCall.Builder
                .create()
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("ListNames"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
            
            CompletableFuture<InboundMessage> response = connection.sendRequest(call);
            InboundMessage reply = response.get();
            
            System.out.println("✅ Received response: " + reply.getType());
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            connection.close();
            System.out.println("✅ Connection closed");
        }
    }
    
    private static class LoggingHandler extends AbstractInboundHandler {
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            System.out.println("📨 Received: " + msg.getType());
            ctx.propagateInboundMessage(msg);
        }
    }
}
```

This guide should give you everything you need to start building D-Bus applications with the library. For more advanced use cases, check out the other guides in this documentation.