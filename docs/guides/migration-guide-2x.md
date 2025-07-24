# Migration Guide: 1.x to 2.0

This guide helps you migrate from D-Bus Client Java 1.x to 2.0, which introduces significant architectural improvements and API changes.

## Overview

Version 2.0 represents a major evolution of the library with:

- **Dual-Pipeline Architecture**: Separate application and transport layers for better performance
- **Thread Safety**: Enhanced thread isolation with safe blocking operations
- **Strategy Pattern**: Pluggable transport support (Unix sockets, TCP)
- **Builder Pattern**: Improved API design with builder patterns for message construction
- **Factory Pattern**: Streamlined encoder/decoder creation
- **Better Testing**: Comprehensive test coverage (83%) and DummyConnection for unit testing

## Breaking Changes Summary

| Change Category | 1.x | 2.0 | Impact |
|----------------|-----|-----|--------|
| **Maven Coordinates** | `com.lucimber:dbus-client` | `com.lucimber:lucimber-dbus-client` | High |
| **Connection API** | `DBusConnection.create()` | `NettyConnection.newSessionBusConnection()` | High |
| **Package Structure** | Separate encoder/decoder | Unified `codec` package | Medium |
| **Message Building** | Constructors | Builder pattern required | Medium |
| **Threading Model** | Single-threaded | Dual-pipeline with thread isolation | Medium |
| **SASL Configuration** | Basic config | Type-safe configuration classes | Low |

## Step-by-Step Migration

### 1. Update Dependencies

**Maven (pom.xml):**
```xml
<!-- Before (1.x) -->
<dependency>
    <groupId>com.lucimber</groupId>
    <artifactId>dbus-client</artifactId>
    <version>1.x.x</version>
</dependency>

<!-- After (2.0) -->
<dependency>
    <groupId>com.lucimber</groupId>
    <artifactId>lucimber-dbus-client</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```

**Gradle (build.gradle):**
```gradle
// Before (1.x)
implementation 'com.lucimber:dbus-client:1.x.x'

// After (2.0)
implementation 'com.lucimber:lucimber-dbus-client:2.0-SNAPSHOT'
```

### 2. Update Connection Creation

**Before (1.x):**
```java
// Basic connection creation
Connection connection = DBusConnection.create();
connection.connect();
```

**After (2.0):**
```java
// System bus connection
Connection connection = NettyConnection.newSystemBusConnection();
connection.connect().toCompletableFuture().get();

// Session bus connection  
Connection connection = NettyConnection.newSessionBusConnection();
connection.connect().toCompletableFuture().get();

// Custom configuration (using standard connection methods)
ConnectionConfig config = ConnectionConfig.builder()
    .withConnectTimeout(Duration.ofSeconds(10))
    .withHealthCheckEnabled(true)
    .withAutoReconnectEnabled(true)
    .build();
// Note: Custom config applied through connection factory methods
Connection connection = NettyConnection.newSystemBusConnection();
connection.connect().toCompletableFuture().get();
```

### 3. Update Package Imports

Several packages have been reorganized for better structure:

**Codec Packages (Major Change):**
```java
// Before (1.x)
import com.lucimber.dbus.encoder.*;
import com.lucimber.dbus.decoder.*;

// After (2.0) - Unified codec package
import com.lucimber.dbus.codec.encoder.*;
import com.lucimber.dbus.codec.decoder.*;
```

**Annotation Packages:**
```java
// Before (1.x)
import com.lucimber.dbus.annotation.*;
import com.lucimber.dbus.annotation.impl.*;

// After (2.0) - impl package merged
import com.lucimber.dbus.annotation.*;
// All implementation classes moved to main annotation package
```

**Connection Packages:**
```java
// Before (1.x)  
import com.lucimber.dbus.connection.*;

// After (2.0) - New Netty-based connection
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.connection.*;
```

### 4. Update Message Handling

**Method Calls with Builder Pattern:**
```java
// Before (1.x)
OutboundMethodCall call = new OutboundMethodCall(
    serial, path, member, destination, iface, replyExpected
);

// After (2.0) - Builder pattern
OutboundMethodCall call = OutboundMethodCall.Builder
    .create()
    .withSerial(connection.getNextSerial())
    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
    .withMember(DBusString.valueOf("ListNames"))
    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
    .withReplyExpected(true)
    .build();
```

**Request-Response Pattern:**
```java
// Before (1.x)
connection.send(call);
// Manual correlation required

// After (2.0) - Built-in correlation
CompletableFuture<InboundMessage> response = connection.sendRequest(call);
InboundMessage reply = response.get();
```

### 5. Update Handler Implementation

**Thread-Safe Handlers:**
```java
// Before (1.x)
public class MyHandler implements MessageHandler {
    @Override
    public void handleMessage(Message msg) {
        // Needed to avoid blocking the event loop
        executor.submit(() -> {
            processMessage(msg);
        });
    }
}

// After (2.0) - Safe blocking operations
public class MyHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // ✅ SAFE - Blocking operations are completely safe!
        String result = databaseCall(msg);  // OK to block here
        ctx.propagateInboundMessage(processedMessage(msg, result));
    }
}
```

### 6. Update Connection Configuration

**Enhanced Connection Configuration:**
```java
// Before (1.x)
// Basic connection with minimal configuration

// After (2.0) - Rich configuration options
ConnectionConfig config = ConnectionConfig.builder()
    .withConnectTimeout(Duration.ofSeconds(10))
    .withMethodCallTimeout(Duration.ofSeconds(30))
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(60))
    .withAutoReconnectEnabled(true)
    .withMaxReconnectAttempts(5)
    .build();

// Note: Configuration is built but applied through connection factory methods
Connection connection = NettyConnection.newSessionBusConnection();
```

### 7. Update Exception Handling

**Enhanced Exception Hierarchy:**
```java
// Before (1.x)
try {
    connection.send(message);
} catch (DBusException e) {
    // Generic exception handling
}

// After (2.0) - Specific exceptions
try {
    CompletableFuture<InboundMessage> response = connection.sendRequest(call);
    InboundMessage reply = response.get();
} catch (AuthFailedException e) {
    // Handle authentication failure
} catch (TimeoutException e) {
    // Handle timeout
} catch (DisconnectedException e) {
    // Handle disconnection
}
```

## New Features in 2.0

### 1. Safe Blocking Operations
Handlers can now safely perform blocking operations without affecting transport performance:

```java
public class DatabaseHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // All of these are now SAFE:
        String data = database.queryBlocking(msg);        // ✅ Database calls
        String result = restClient.postBlocking(data);    // ✅ REST API calls  
        Files.write(path, result.getBytes());            // ✅ File I/O
        Thread.sleep(1000);                              // ✅ Sleep/wait
        
        ctx.propagateInboundMessage(createResponse(result));
    }
}
```

### 2. Connection Health Monitoring
```java
Connection connection = NettyConnection.newSystemBusConnection();

// Add health monitoring with event listeners
connection.addConnectionEventListener(event -> {
    switch (event.getType()) {
        case STATE_CHANGED:
            log.info("Connection state: {} -> {}", 
                event.getOldState().orElse(null), 
                event.getNewState().orElse(null));
            break;
        case HEALTH_CHECK_FAILED:
            log.warn("Health check failed");
            break;
    }
});
```

### 3. Strategy Pattern for Transport
```java
// 2.0 supports pluggable transport strategies
ConnectionConfig config = ConnectionConfig.builder()
    .withTransportStrategy(new NettyTcpStrategy("localhost", 1234))
    // or .withTransportStrategy(new NettyUnixSocketStrategy("/tmp/dbus-socket"))
    .build();

Connection connection = NettyConnection.create(config);
```

### 4. Enhanced Testing Support
```java
// 2.0 provides DummyConnection for unit testing
@Test
void testMyHandler() {
    DummyConnection connection = DummyConnection.builder()
        .withAutoResponse(true)
        .build();
    
    // Test without real D-Bus daemon
    MyHandler handler = new MyHandler();
    connection.pipeline().addLast("handler", handler);
    
    // Send test message
    OutboundMethodCall call = OutboundMethodCall.Builder
        .create()
        .withSerial(1)
        .withPath(DBusObjectPath.valueOf("/test"))
        .build();
        
    CompletableFuture<InboundMessage> response = connection.sendRequest(call);
    // Verify handler behavior
}
```

## Testing Changes

### Enhanced Test Coverage
Version 2.0 includes comprehensive test coverage improvements:

- **83% overall test coverage** (improved from 68%)
- **DummyConnection** for unit testing without D-Bus daemon
- **SASL authentication** testing for all mechanisms
- **Connection lifecycle** testing with 100% coverage
- **Integration tests** using containerized D-Bus daemon

## Performance Considerations

### 1. Memory Usage
2.0 includes memory optimizations:
- Buffer pooling for reduced GC pressure
- Lazy loading of message components
- Optimized serialization paths

### 2. Threading Model
The new dual-pipeline architecture provides:
- Better CPU utilization
- Reduced contention between application and transport layers
- Safe blocking operations without performance impact

## Compatibility

| Feature | 1.x | 2.0 | Notes |
|---------|-----|-----|-------|
| **Java Version** | 11+ | 17+ | Minimum Java version increased |
| **D-Bus Protocol** | 0.32 | 0.35+ | Enhanced protocol support |
| **Test Coverage** | ~60% | 83% | Comprehensive test improvements |
| **Transport** | Unix sockets | Unix + TCP via Strategy pattern | Pluggable transport support |
| **Code Quality** | Basic | PMD + Checkstyle + Spotless | Enhanced quality tooling |

## Common Migration Issues

### Issue 1: Import Errors
**Problem:** Package not found errors after upgrade
**Solution:** Update imports according to the package mapping above

### Issue 2: Connection Creation Fails  
**Problem:** `DBusConnection.create()` method not found
**Solution:** Use `NettyConnection.newSessionBusConnection()` or `NettyConnection.newSystemBusConnection()`

### Issue 3: Handler Blocking
**Problem:** Worried about blocking operations in handlers
**Solution:** In 2.0, blocking operations are completely safe in handlers due to the dual-pipeline architecture

### Issue 4: Maven Coordinates
**Problem:** Artifact not found with old coordinates
**Solution:** Update to `com.lucimber:lucimber-dbus-client`

### Issue 5: Builder Pattern Required
**Problem:** Constructor-based message creation no longer works  
**Solution:** All message types now require Builder pattern for construction

### Issue 6: Connection Configuration Changes
**Problem:** Limited connection configuration options in 1.x
**Solution:** Use `ConnectionConfig.builder()` for rich configuration options (timeouts, health checks, reconnection)

## Getting Help

If you encounter issues during migration:

1. **Check Documentation**: [Developer Guide](developer-guide.md)
2. **Review Examples**: [Working Examples](../../examples/README.md)  
3. **Architecture Guide**: [Architecture Documentation](../architecture/)
4. **GitHub Issues**: [Report Problems](https://github.com/lucimber/dbus-client-java/issues)

## Migration Checklist

- [ ] Update Maven/Gradle coordinates
- [ ] Update package imports
- [ ] Replace connection creation code
- [ ] Update message handling to use builders
- [ ] Update connection configuration
- [ ] Test with new threading model
- [ ] Update exception handling
- [ ] Review handler implementations
- [ ] Run integration tests
- [ ] Update documentation/comments

## Example: Complete Migration

**Before (1.x):**
```java
import com.lucimber.dbus.*;
import com.lucimber.dbus.encoder.*;
import com.lucimber.dbus.decoder.*;

public class DBusExample {
    public void example() throws Exception {
        Connection connection = DBusConnection.create();
        connection.connect();
        
        OutboundMethodCall call = new OutboundMethodCall(
            1, "/org/example", "Method", "org.example", "org.example.Interface", true
        );
        
        connection.send(call);
        // Manual response handling...
        
        connection.close();
    }
}
```

**After (2.0):**
```java
import com.lucimber.dbus.connection.*;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;
import com.lucimber.dbus.codec.encoder.*;
import com.lucimber.dbus.codec.decoder.*;

public class DBusExample {
    public void example() throws Exception {
        Connection connection = NettyConnection.newSessionBusConnection();
        connection.connect().toCompletableFuture().get();
        
        OutboundMethodCall call = OutboundMethodCall.Builder
            .create()
            .withSerial(connection.getNextSerial())
            .withPath(DBusObjectPath.valueOf("/org/example"))
            .withMember(DBusString.valueOf("Method"))
            .withDestination(DBusString.valueOf("org.example"))
            .withInterface(DBusString.valueOf("org.example.Interface"))
            .withReplyExpected(true)
            .build();
        
        CompletableFuture<InboundMessage> response = connection.sendRequest(call);
        InboundMessage reply = response.get();
        
        connection.close();
    }
}
```

This migration guide should help you smoothly transition from 1.x to 2.0. The new architecture provides significant benefits in terms of performance, thread safety, and ease of use.