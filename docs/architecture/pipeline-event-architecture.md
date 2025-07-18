# Pipeline and Event Propagation Architecture

This document provides a comprehensive analysis of the dual-pipeline architecture and event propagation system in the D-Bus client Java library. It explains how messages and events flow through both the public API and Netty implementation layers, with detailed coverage of threading models, thread safety, and performance considerations.

## Architecture Overview

The D-Bus client implements a **sophisticated dual-pipeline system** with clear separation between the public API and Netty implementation layers. This architecture ensures optimal performance while maintaining clean abstractions and thread safety.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              APPLICATION LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                        Public API Pipeline                             │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │    HEAD     │→ │ User Handler│→ │ User Handler│→ │    TAIL     │    │    │
│  │  │ (Internal)  │  │     1       │  │     2       │  │ (Internal)  │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  │                                                                         │    │
│  │  Events: Connection Events, User Events, Message Events                │    │
│  │  Thread: Application Task Executor                                     │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                       │                                          │
│                                       │ AppLogicHandler (Bridge)                 │
│                                       ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                        Netty Pipeline                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │    SASL     │→ │   Frame     │→ │   Message   │→ │ AppLogic    │    │    │
│  │  │  Handlers   │  │  Handlers   │  │  Handlers   │  │  Handler    │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  │                                                                         │    │
│  │  Events: DBusChannelEvent, Channel Events, User Events                 │    │
│  │  Thread: Netty Event Loop                                              │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                 │
│                              TRANSPORT LAYER                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Dual-Pipeline Architecture

### 1. Public API Pipeline System

The public API pipeline provides a clean, extensible interface for application developers to process D-Bus messages and events.

#### **Core Components**

- **`Pipeline` Interface** - Contract for message and event propagation
- **`DefaultPipeline` Implementation** - Doubly-linked list with HEAD and TAIL handlers
- **Handler Hierarchy**:
  - `Handler` - Base interface for all handlers
  - `InboundHandler` - Processes incoming messages and failures
  - `OutboundHandler` - Processes outgoing messages with CompletableFuture
  - `AbstractDuplexHandler` - Handles both inbound and outbound

#### **Message Flow Patterns**

```java
// Inbound Flow: HEAD → ... → TAIL
pipeline.propagateInboundMessage(message);
pipeline.propagateInboundFailure(throwable);

// Outbound Flow: TAIL → ... → HEAD  
pipeline.propagateOutboundMessage(message, future);
```

#### **Event Propagation**

```java
// Connection lifecycle events
pipeline.propagateConnectionActive();
pipeline.propagateConnectionInactive();

// Custom user events
pipeline.propagateUserEvent(event);
```

#### **Thread Safety Mechanisms**

- **Concurrent Handler Management**: Uses `ConcurrentHashMap` for handler mapping
- **Doubly-Linked Structure**: Efficient insertion/removal at any position
- **Synchronization**: Thread-safe handler addition/removal with proper locking
- **Lifecycle Hooks**: `onHandlerAdded()`, `onHandlerRemoved()` callbacks

### 2. Netty Pipeline System

The Netty pipeline handles low-level D-Bus protocol details, SASL authentication, and network communication.

#### **Pipeline Configuration**

The pipeline is configured through centralized components:

- **`DBusHandlerConfiguration`** - Centralized handler ordering and creation
- **`DBusHandlerNames`** - Standardized handler names for consistency
- **`ReconnectionHandlerManager`** - Manages handler lifecycle during reconnection

#### **Handler Order and Flow**

```java
// Complete Netty Pipeline (left to right)
SaslInitiationHandler → SaslCodec → SaslAuthenticationHandler →
NettyByteLogger → FrameEncoder → OutboundMessageEncoder → 
FrameDecoder → InboundMessageDecoder → DBusMandatoryNameHandler → 
ConnectionCompletionHandler → ReconnectionHandlerManager → AppLogicHandler
```

#### **Handler Categories**

1. **SASL Handlers** (Temporary - removed after authentication):
   - `SaslInitiationHandler` - Sends initial NUL byte
   - `SaslCodec` - Manages SASL sub-handlers
   - `SaslAuthenticationHandler` - Handles SASL authentication

2. **Protocol Handlers** (Permanent):
   - `FrameEncoder/Decoder` - D-Bus frame processing
   - `OutboundMessageEncoder/InboundMessageDecoder` - Message serialization
   - `DBusMandatoryNameHandler` - Unique name acquisition

3. **Management Handlers** (Permanent):
   - `ConnectionCompletionHandler` - Connection establishment
   - `ReconnectionHandlerManager` - Reconnection support
   - `AppLogicHandler` - Bridge to public API

#### **Event Types**

```java
// Protocol-specific events
DBusChannelEvent.SASL_NUL_BYTE_SENT
DBusChannelEvent.SASL_AUTH_COMPLETE
DBusChannelEvent.SASL_AUTH_FAILED
DBusChannelEvent.MANDATORY_NAME_ACQUIRED
DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED
DBusChannelEvent.RECONNECTION_STARTING
DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED
```

## Pipeline Coordination and Bridge Architecture

### AppLogicHandler - The Critical Bridge

The `AppLogicHandler` serves as the crucial bridge between the Netty pipeline and the public API pipeline, ensuring proper message routing and event translation.

```java
public final class AppLogicHandler extends ChannelDuplexHandler {
    
    // Inbound message bridging
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof InboundMessage) {
            // Thread switch: Netty Event Loop → Application Executor
            applicationTaskExecutor.submit(() -> {
                connection.getPipeline().propagateInboundMessage((InboundMessage) msg);
            });
        }
    }
    
    // Outbound message bridging
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof OutboundMessage) {
            // Handle method call correlation and timeout
            handleOutboundMessage((OutboundMessage) msg, promise);
        }
        ctx.write(msg, promise);
    }
    
    // Event bridging
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Translate Netty events to public API events
        applicationTaskExecutor.submit(() -> {
            connection.getPipeline().propagateConnectionActive();
        });
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Handle connection loss
        applicationTaskExecutor.submit(() -> {
            connection.getPipeline().propagateConnectionInactive();
        });
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof DBusChannelEvent) {
            // Translate D-Bus events to public API events
            translateChannelEvent((DBusChannelEvent) evt);
        }
        ctx.fireUserEventTriggered(evt);
    }
}
```

### Event Translation and Bridging

The bridge handles multiple types of event translation:

1. **Channel Events** → **Connection Events**
2. **DBusChannelEvent** → **Public API Events**
3. **Netty Exceptions** → **Public API Exceptions**
4. **User Events** → **Pipeline Events**

## Complete Message Flow Architecture

### Inbound Message Journey

1. **Network Layer**: Raw bytes received from D-Bus daemon
2. **Netty Pipeline Processing**:
   - `FrameDecoder` → Parses D-Bus frames
   - `InboundMessageDecoder` → Deserializes to `InboundMessage`
   - `DBusMandatoryNameHandler` → Handles name acquisition messages
   - `ConnectionCompletionHandler` → Handles connection establishment
   - `AppLogicHandler` → Routes to public API
3. **Thread Switch**: Netty Event Loop → Application Task Executor
4. **Public API Pipeline Processing**:
   - `InternalHeadHandler` → Entry point
   - User handlers → Custom processing
   - `InternalTailHandler` → Final fallback
5. **Application Logic**: Final message consumption

### Outbound Message Journey

1. **Application Code**: Creates `OutboundMessage`
2. **Public API Pipeline Processing**:
   - `InternalTailHandler` → Entry point (reverse flow)
   - User handlers → Custom processing
   - `InternalHeadHandler` → Routes to connection
3. **Connection Layer**: `connection.sendAndRouteResponse()`
4. **AppLogicHandler**: Message correlation and timeout management
5. **Netty Pipeline Processing**:
   - `OutboundMessageEncoder` → Serializes message
   - `FrameEncoder` → Creates D-Bus frames
   - `NettyByteLogger` → Optional logging
6. **Network Layer**: Raw bytes sent to D-Bus daemon

## Event Processing Architecture

### Public API Event System

#### **Connection Event Components**

- **`ConnectionEventListener`** - Functional interface for user callbacks
- **`ConnectionEventType`** - High-level event types (STATE_CHANGED, HEALTH_CHECK_SUCCESS, etc.)
- **`ConnectionEvent`** - Immutable event objects with builder pattern

#### **Event Types and Flow**

```java
// Connection state events
ConnectionEventType.STATE_CHANGED
ConnectionEventType.HEALTH_CHECK_SUCCESS
ConnectionEventType.HEALTH_CHECK_FAILURE
ConnectionEventType.RECONNECTION_ATTEMPT
ConnectionEventType.RECONNECTION_SUCCESS
ConnectionEventType.RECONNECTION_FAILURE
```

#### **Event Registration and Firing**

```java
// User registration
connection.addConnectionEventListener((conn, event) -> {
    // Handle connection event - runs on dedicated thread pool
});

// Event firing (internal)
private void fireConnectionEvent(ConnectionEvent event) {
    eventExecutor.execute(() -> {
        for (ConnectionEventListener listener : listeners) {
            try {
                listener.onConnectionEvent(connection, event);
            } catch (Exception e) {
                // Exception isolation - one listener can't affect others
            }
        }
    });
}
```

### Netty Event System

#### **Event Propagation Patterns**

```java
// Standard Netty events
ctx.fireChannelActive();
ctx.fireChannelInactive();
ctx.fireExceptionCaught(cause);

// D-Bus specific events
ctx.fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUIRED);

// Event handling in pipeline
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt == DBusChannelEvent.MANDATORY_NAME_ACQUIRED) {
        handleNameAcquisition();
    } else if (evt == DBusChannelEvent.RECONNECTION_STARTING) {
        handleReconnectionStart();
    }
    // Always propagate unless consumed
    ctx.fireUserEventTriggered(evt);
}
```

#### **Event Lifecycle Management**

Events in the Netty pipeline follow specific lifecycle patterns:

1. **Generation**: Events created by protocol handlers
2. **Propagation**: Events flow through pipeline using `fireUserEventTriggered()`
3. **Interception**: Handlers can intercept and process events
4. **Translation**: `AppLogicHandler` translates to public API events
5. **Consumption**: Final handlers may consume events

## Threading Model and Thread Safety

### Thread Isolation Strategy

The architecture implements strict thread isolation to prevent blocking and ensure performance:

#### **Thread Categories**

1. **Netty Event Loop Threads**
   - Handle all I/O operations
   - Process Netty pipeline events
   - **Never blocked** by user code

2. **Application Task Executor**
   - Dedicated thread pool for user code
   - Configurable size (default: CPU cores / 2)
   - Processes public API pipeline events

3. **Event Executor**
   - Dedicated thread pool for connection events
   - Fires events to registered listeners
   - Isolated from main processing

4. **Health Check Scheduler**
   - Separate `ScheduledExecutorService`
   - Handles periodic health monitoring
   - Independent of main processing

### Thread Safety Mechanisms

#### **Event Object Immutability**

```java
public final class ConnectionEvent {
    private final ConnectionEventType type;
    private final ConnectionState oldState;
    private final ConnectionState newState;
    private final Instant timestamp;
    private final String message;
    private final Throwable cause;
    
    // All fields are final - completely immutable
    // Safe to pass between threads without synchronization
}
```

#### **Concurrent Collections**

```java
// Public API Pipeline
private final ConcurrentHashMap<String, InternalContext> handlerMap = new ConcurrentHashMap<>();
private final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<>();

// AppLogicHandler
private final ConcurrentHashMap<DBusUInt32, PendingMethodCall> pendingMethodCalls = new ConcurrentHashMap<>();
```

#### **Thread-Safe State Management**

```java
// Atomic state management
private final AtomicBoolean connecting = new AtomicBoolean(false);
private final AtomicBoolean closing = new AtomicBoolean(false);
private final AtomicReference<ConnectionHandle> connectionHandle = new AtomicReference<>();

// Thread-safe handler addition
public synchronized void addLast(String name, Handler handler) {
    // Synchronized handler management
    InternalContext newContext = new InternalContext(handler, name);
    // ... thread-safe doubly-linked list manipulation
}
```

### Performance Optimizations

#### **Non-Blocking Design**

- **Event Loop Protection**: User code never runs on Netty event loop
- **Asynchronous Processing**: All user callbacks submitted to application executor
- **Message Buffering**: Efficient message queuing without blocking

#### **Resource Management**

```java
// Proper executor lifecycle management
public void close() {
    try {
        eventExecutor.shutdown();
        if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            eventExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        eventExecutor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

## Advanced Pipeline Features

### Dynamic Handler Management

#### **Handler Lifecycle During Reconnection**

```java
// ReconnectionHandlerManager
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt == DBusChannelEvent.RECONNECTION_STARTING) {
        // Reset channel attributes
        ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).set(new AtomicLong(1));
        ctx.channel().attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(null);
    } else if (evt == DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED) {
        // Re-add handlers that were removed during connection
        addSaslHandlersIfMissing(ctx);
        updateConnectionCompletionHandler(ctx);
    }
    super.userEventTriggered(ctx, evt);
}
```

#### **Centralized Handler Configuration**

```java
// DBusHandlerConfiguration
public static void initializePipeline(ChannelPipeline pipeline, 
                                    Promise<Void> connectPromise, 
                                    AppLogicHandler appLogicHandler) {
    Map<String, Supplier<ChannelHandler>> handlers = createHandlerMap(connectPromise, appLogicHandler);
    
    for (Map.Entry<String, Supplier<ChannelHandler>> entry : handlers.entrySet()) {
        String name = entry.getKey();
        ChannelHandler handler = entry.getValue().get();
        pipeline.addLast(name, handler);
    }
}
```

### Request-Response Correlation

#### **Method Call Management**

```java
// AppLogicHandler method call correlation
private void handleOutboundMethodCall(OutboundMethodCall call, ChannelPromise promise) {
    if (call.isReplyExpected()) {
        PendingMethodCall pendingCall = new PendingMethodCall(call, promise);
        pendingMethodCalls.put(call.getSerial(), pendingCall);
        
        // Schedule timeout
        scheduleTimeout(call.getSerial(), config.getMethodCallTimeout());
    }
}

private void handleInboundMethodReturn(InboundMethodReturn response) {
    PendingMethodCall pendingCall = pendingMethodCalls.remove(response.getReplySerial());
    if (pendingCall != null) {
        pendingCall.complete(response);
    }
}
```

### Health Monitoring Integration

#### **Health Check Pipeline Integration**

```java
// ConnectionHealthHandler
public class ConnectionHealthHandler extends AbstractDuplexHandler {
    
    @Override
    public void channelActive(Context ctx) {
        // Start health monitoring when connection is active
        startHealthMonitoring();
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(Context ctx) {
        // Stop health monitoring when connection is lost
        stopHealthMonitoring();
        super.channelInactive(ctx);
    }
    
    private void performHealthCheck() {
        // Send ping message through public API pipeline
        OutboundMethodCall ping = createPingMessage();
        ctx.propagateOutboundMessage(ping, new CompletableFuture<>());
    }
}
```

## Error Handling and Recovery

### Exception Propagation Patterns

#### **Cross-Pipeline Error Handling**

```java
// Public API Pipeline error handling
@Override
public void propagateInboundFailure(Throwable cause) {
    // Error flows toward tail in public API pipeline
    next.propagateInboundFailure(cause);
}

// Netty Pipeline error handling
@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Error flows through Netty pipeline
    logger.error("Pipeline error", cause);
    ctx.fireExceptionCaught(cause);
}
```

#### **Error Translation and Recovery**

```java
// AppLogicHandler error translation
@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Translate Netty exceptions to public API exceptions
    applicationTaskExecutor.submit(() -> {
        connection.getPipeline().propagateInboundFailure(cause);
    });
    
    // Trigger connection recovery if needed
    if (isRecoverableError(cause)) {
        triggerReconnection(cause);
    }
}
```

### Recovery Mechanisms

#### **Automatic Reconnection**

```java
// ConnectionReconnectHandler
public void triggerReconnection(Throwable cause) {
    if (shouldReconnect(cause)) {
        // Fire reconnection events through both pipelines
        ctx.fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
        
        // Schedule reconnection attempt
        scheduleReconnection();
    }
}
```

## Best Practices and Guidelines

### Pipeline Handler Development

1. **Thread Safety**: Always assume multi-threaded access
2. **Exception Handling**: Isolate exceptions to prevent pipeline corruption
3. **Resource Management**: Properly clean up resources in lifecycle methods
4. **Event Propagation**: Only consume events that are intended for your handler
5. **Performance**: Avoid blocking operations in handlers

### Event System Usage

1. **Event Immutability**: Always use immutable event objects
2. **Listener Isolation**: Ensure one listener's exception doesn't affect others
3. **Asynchronous Processing**: Use dedicated thread pools for event processing
4. **Event Ordering**: Consider event ordering requirements for your use case

### Threading Considerations

1. **Never Block Event Loop**: User code must never run on Netty event loop threads
2. **Executor Management**: Properly manage custom executor lifecycles
3. **Thread-Safe Collections**: Use concurrent collections for shared state
4. **Atomic Operations**: Use atomic operations for simple state management

## Performance Characteristics

### Throughput Optimization

- **Lock-Free Operations**: Minimal locking in hot paths
- **Efficient Memory Usage**: Object pooling and reuse
- **Batch Processing**: Efficient bulk operations where possible

### Latency Optimization

- **Direct Memory Access**: Netty's direct buffer usage
- **Minimal Copying**: Zero-copy operations where possible
- **Efficient Serialization**: Optimized D-Bus encoding/decoding

### Scalability Features

- **Configurable Thread Pools**: Tunable for different workloads
- **Connection Pooling**: Future enhancement for high-load scenarios
- **Resource Monitoring**: Built-in metrics and monitoring hooks

## Future Enhancements

### Planned Improvements

1. **Reactive Streams Support**: Integration with reactive programming models
2. **Backpressure Handling**: Flow control for high-throughput scenarios
3. **Enhanced Metrics**: More detailed performance monitoring
4. **Hot Reloading**: Dynamic handler reconfiguration
5. **Load Balancing**: Multiple connection support with load balancing

### Extension Points

The architecture provides several extension points for custom implementations:

- **Custom Transport Strategies**: Alternative transport mechanisms
- **Custom Authentication**: Additional SASL mechanisms
- **Custom Message Processing**: Specialized message handlers
- **Custom Event Types**: Application-specific event types

## Conclusion

The dual-pipeline architecture provides a robust, performant, and extensible foundation for D-Bus communication in Java applications. The clear separation between public API and implementation details, combined with comprehensive thread safety and performance optimizations, makes it suitable for both simple applications and high-performance systems.

The architecture's key strengths include:

1. **Clean Abstractions**: Public API independent of transport implementation
2. **Thread Safety**: Comprehensive thread safety without performance penalties
3. **Performance**: Optimized for both throughput and latency
4. **Extensibility**: Multiple extension points for customization
5. **Reliability**: Comprehensive error handling and recovery mechanisms

This design enables developers to build robust D-Bus applications while maintaining the flexibility to extend and customize the library for specific use cases.

## Related Documentation

- [Blocking Operations Guide](../guides/blocking-operations.md) - Comprehensive guide for handling database calls, REST APIs, and file I/O in D-Bus handlers
- [Handler Architecture](handler-architecture.md) - Detailed handler development patterns and best practices
- [Message Flow Architecture](message-flow.md) - Traditional view of message processing through the library