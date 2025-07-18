# D-Bus Client Java - Developer Guide

Welcome to the D-Bus Client Java library! This guide provides a structured learning path for developers using the library for the first time, organized by practical importance and usefulness.

## 🚀 Quick Start (5 minutes)

Get up and running immediately with basic D-Bus operations.

### Essential Concepts
- **What it does**: High-performance, asynchronous D-Bus client for Java applications
- **Key benefit**: Thread-safe blocking operations with sophisticated dual-pipeline architecture
- **Use cases**: System integration, IPC, desktop applications, IoT device communication

### First Steps
```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.netty.NettyConnection;

// Connect to system bus
Connection connection = NettyConnection.newSystemBusConnection();
connection.connect().toCompletableFuture().get();

// Your D-Bus operations here...

connection.close();
```

**📖 Detailed Documentation**: [Quick Start Guide](guides/quick-start.md)

---

## 🏗️ Basic Message Operations (15 minutes)

Learn to send method calls, handle responses, and work with D-Bus messages.

### Core Operations
- **Method Calls**: Send synchronous and asynchronous method calls
- **Signal Handling**: Subscribe to and process D-Bus signals  
- **Message Types**: Work with method calls, returns, signals, and errors
- **Request-Response**: Handle method call timeouts and correlation

### Example
```java
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.*;

OutboundMethodCall call = OutboundMethodCall.Builder
    .create()
    .withSerial(DBusUInt32.valueOf(1))
    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
    .withMember(DBusString.valueOf("ListNames"))
    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
    .withReplyExpected(true)
    .build();

CompletableFuture<InboundMessage> response = connection.sendRequest(call);
```

**📖 Detailed Documentation**: [Message Operations](guides/quick-start.md#message-operations)

---

## 🔧 Configuration & Connection Management (10 minutes)

Configure connections for different environments and use cases.

### Connection Types
- **System Bus**: For system-level services (requires permissions)
- **Session Bus**: For user-level applications  
- **Custom Address**: TCP, Unix sockets, custom transports

### Configuration Options
```java
import com.lucimber.dbus.connection.ConnectionConfig;

ConnectionConfig config = ConnectionConfig.builder()
    .withAutoReconnectEnabled(true)
    .withHealthCheckEnabled(true)
    .withConnectTimeout(Duration.ofSeconds(10))
    .withMethodCallTimeout(Duration.ofSeconds(30))
    .build();

Connection connection = NettyConnection.newSystemBusConnection(config);
```

**📖 Detailed Documentation**: 
- [Transport Usage Guide](guides/transport-usage.md)
- [Connection Configuration](guides/quick-start.md#configuration)

---

## 🎯 Handler Development (20 minutes)

Create custom handlers for processing D-Bus messages in your application logic.

### Handler Types
- **Inbound Handlers**: Process incoming messages (signals, method calls)
- **Outbound Handlers**: Modify outgoing messages
- **Duplex Handlers**: Handle both directions

### Safe Blocking Operations
**✅ Important**: Your handlers run on dedicated thread pools - blocking operations are completely safe!

```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;

public class MyBusinessLogicHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // ✅ SAFE - Database calls, REST APIs, file I/O all OK here!
        String result = databaseCall(msg);  // Blocking operation - totally fine!
        
        ctx.propagateInboundMessage(enrichMessage(msg, result));
    }
}

// Add to pipeline
connection.getPipeline().addLast("business-logic", new MyBusinessLogicHandler());
```

**📖 Detailed Documentation**: 
- **Essential**: [Blocking Operations Guide](guides/blocking-operations.md)
- [Handler Architecture](architecture/handler-architecture.md)

---

## 🌱 Spring Boot Integration (15 minutes)

Integrate with Spring Boot applications using dependency injection and configuration.

### Auto-Configuration
```java
@Configuration
public class DBusConfiguration {
    
    @Bean
    public Connection dbusConnection() {
        return NettyConnection.newSystemBusConnection();
    }
}

@Service
public class MyDBusService {
    private final Connection dbusConnection;
    
    public MyDBusService(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
        setupHandlers();
    }
}
```

### Spring Boot Features
- Health checks with Actuator
- Metrics with Micrometer
- Configuration properties
- Lifecycle management

**📖 Detailed Documentation**: 
- [Spring Boot Integration Guide](guides/spring-boot-integration.md)
- [Integration Patterns](guides/integration-patterns.md)

---

## 🔧 Advanced Features (30 minutes)

Explore sophisticated features for production applications.

### Error Handling & Resilience
- Exception propagation and handling
- Circuit breakers and retry patterns
- Health monitoring and reconnection
- Graceful degradation

### Performance & Monitoring
- Connection pooling strategies
- Metrics collection
- Performance tuning
- Memory management

### Reactive Programming
- Project Reactor integration
- RxJava support
- Async/await patterns
- Backpressure handling

**📖 Detailed Documentation**: 
- [Error Handling Guide](guides/error-handling.md)
- [Integration Patterns](guides/integration-patterns.md)
- [Health Monitoring](health-monitoring.md)

---

## 🏛️ Architecture Deep Dive (45 minutes)

Understand the sophisticated dual-pipeline architecture for advanced use cases.

### Core Architecture
- **Dual-Pipeline System**: Public API + Netty transport layers
- **RealityCheckpoint**: The bridge between pipeline layers (named after the Logistics drum & bass track!)
- **Thread Isolation**: ApplicationTaskExecutor vs Netty EventLoop
- **Event Propagation**: How messages flow through the system

### When You Need This
- Custom transport implementations
- Advanced handler development  
- Performance optimization
- Understanding thread safety guarantees

**📖 Detailed Documentation**: 
- **Comprehensive**: [Pipeline & Event Architecture](architecture/pipeline-event-architecture.md)
- [Architecture Overview](architecture/overview.md)
- [Transport Strategies](architecture/transport-strategies.md)

---

## 🧪 Testing & Debugging (20 minutes)

Test your D-Bus integration effectively.

### Testing Strategies
- Unit testing with mocks
- Integration testing with test containers
- Performance testing
- Memory testing

### Debugging Tools
- Connection health checks
- Pipeline introspection
- Thread analysis
- Performance monitoring

**📖 Detailed Documentation**: 
- [Testing Guide](testing-guide.md)
- [Integration Patterns - Testing](guides/integration-patterns.md#testing-patterns)

---

## 📚 Reference Documentation

### Quick Reference
- [D-Bus Specification Overview](dbus-spec.md) - Understanding the D-Bus protocol
- [Examples Collection](examples/README.md) - Working code samples
- [Timeout Configuration](timeout-example.md) - Managing timeouts

### Advanced Topics
- [Message Flow Architecture](architecture/message-flow.md) - Traditional view of message processing
- [Transport Strategies](architecture/transport-strategies.md) - Custom transport implementations

---

## 🎯 Recommended Learning Path

### For New Developers (1-2 hours)
1. **Quick Start** (5 min) → **Message Operations** (15 min) → **Configuration** (10 min)
2. **Handler Development** (20 min) + **Blocking Operations** (essential reading!)
3. **Spring Boot Integration** (15 min) if using Spring

### For Production Applications (3-4 hours)
1. Complete the New Developer path
2. **Advanced Features** (30 min) → **Architecture Deep Dive** (45 min)
3. **Testing & Debugging** (20 min)
4. **Error Handling** and **Performance Monitoring**

### For Library Contributors (5+ hours)
1. Complete all previous paths
2. Study all **Architecture Documentation** in detail
3. Review **Transport Strategies** and **Pipeline Architecture**
4. Understand the **RealityCheckpoint** bridge pattern

---

## 💡 Key Tips for Success

1. **Start Simple**: Begin with basic message operations before diving into advanced features
2. **Embrace Blocking**: Unlike other async libraries, blocking operations are safe and encouraged in handlers
3. **Use Spring Boot**: The integration patterns make enterprise development much easier  
4. **Read Blocking Operations Guide**: This is the most important document for handler development
5. **Understand the Bridge**: The RealityCheckpoint class is the key to understanding thread safety

---

## 🆘 Getting Help

- **Common Issues**: Check the [Error Handling Guide](guides/error-handling.md)
- **Performance**: See [Integration Patterns](guides/integration-patterns.md) for optimization
- **Architecture Questions**: Start with [Pipeline Architecture](architecture/pipeline-event-architecture.md)
- **Examples**: Browse [working examples](examples/README.md) for your use case

Happy coding with D-Bus! 🚀