# Handler Architecture

The handler architecture is the core extensibility mechanism of the D-Bus client library. This document explains how handlers work, how to create custom handlers, and best practices for handler development.

> **Note**: This document focuses on handler development and usage patterns. For comprehensive coverage of the **dual-pipeline architecture**, **event propagation**, **threading models**, and **AppLogicHandler bridge**, see [pipeline-event-architecture.md](pipeline-event-architecture.md).

## Handler Overview

Handlers are processing units in the message pipeline that can intercept, modify, or respond to D-Bus messages. The D-Bus client uses a **dual-pipeline architecture** with handlers in both the **public API pipeline** and the **Netty pipeline**, coordinated through sophisticated event propagation and thread isolation mechanisms.

## Handler Types

### 1. Inbound Handlers

Process messages received from the D-Bus daemon.

```java
public abstract class AbstractInboundHandler implements InboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // Override this method to process inbound messages
        ctx.propagateInboundMessage(msg);
    }
    
    @Override
    public void handleExceptionCaught(Context ctx, Throwable cause) {
        // Override to handle exceptions
        ctx.propagateExceptionCaught(cause);
    }
}
```

**Common Use Cases**:
- Message filtering and routing
- Signal subscription handling
- Method call interception
- Logging and monitoring
- Security and validation

### 2. Outbound Handlers

Process messages being sent to the D-Bus daemon.

```java
public abstract class AbstractOutboundHandler implements OutboundHandler {
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg, 
                                    CompletableFuture<Void> writeFuture) {
        // Override this method to process outbound messages
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
}
```

**Common Use Cases**:
- Message modification and enhancement
- Request/response correlation
- Rate limiting and throttling
- Caching and optimization
- Metrics collection

### 3. Duplex Handlers

Handle both inbound and outbound messages.

```java
public abstract class AbstractDuplexHandler extends AbstractInboundHandler 
                                             implements OutboundHandler {
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg,
                                    CompletableFuture<Void> writeFuture) {
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
}
```

**Common Use Cases**:
- Request/response correlation
- Connection state management
- Bidirectional protocol handling
- Session management

## Pipeline Architecture

### Pipeline Structure

The pipeline processes messages in a specific order:

```
Inbound Flow:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    Head     │ -> │  Handler 1  │ -> │  Handler 2  │ -> │    Tail     │
│  (System)   │    │   (User)    │    │   (User)    │    │  (System)   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘

Outbound Flow:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    Tail     │ <- │  Handler 2  │ <- │  Handler 1  │ <- │    Head     │
│  (System)   │    │   (User)    │    │   (User)    │    │  (System)   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Handler Context

Each handler receives a `Context` object that provides:

```java
public interface Context {
    // Message propagation
    void propagateInboundMessage(InboundMessage msg);
    void propagateOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future);
    
    // Exception handling
    void propagateExceptionCaught(Throwable cause);
    
    // Handler management
    String getName();
    Pipeline getPipeline();
    
    // Connection access
    Connection getConnection();
}
```

## Creating Custom Handlers

### Example 1: Message Logging Handler

```java
public class MessageLoggingHandler extends AbstractDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageLoggingHandler.class);
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        LOGGER.debug("Received: {} from {}", 
                    msg.getType(), 
                    msg.getSender().orElse(DBusString.valueOf("unknown")));
        
        // Logging handler doesn't consume messages - always propagate
        ctx.propagateInboundMessage(msg);
    }
    
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg, 
                                    CompletableFuture<Void> writeFuture) {
        LOGGER.debug("Sending: {} to {}", 
                    msg.getType(),
                    msg.getDestination().orElse(DBusString.valueOf("unknown")));
        
        // Propagate the message
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
}
```

### Example 2: Signal Filter Handler

```java
public class SignalFilterHandler extends AbstractInboundHandler {
    private final Set<String> interestedInterfaces;
    
    public SignalFilterHandler(Set<String> interfaces) {
        this.interestedInterfaces = new HashSet<>(interfaces);
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundSignal) {
            InboundSignal signal = (InboundSignal) msg;
            String interfaceName = signal.getInterfaceName()
                                        .map(DBusString::toString)
                                        .orElse("");
            
            if (interestedInterfaces.contains(interfaceName)) {
                // Signal passes filter - propagate to next handler
                ctx.propagateInboundMessage(msg);
            }
            // Filtered signals are not propagated (this handler consumed them)
        } else {
            // Always propagate non-signal messages
            ctx.propagateInboundMessage(msg);
        }
    }
}
```

### Example 3: Application Logic Handler (Terminal Handler)

```java
public class ApplicationLogicHandler extends AbstractInboundHandler {
    private final SignalProcessor signalProcessor;
    
    public ApplicationLogicHandler(SignalProcessor signalProcessor) {
        this.signalProcessor = signalProcessor;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundSignal) {
            InboundSignal signal = (InboundSignal) msg;
            
            // Process the signal in application logic
            signalProcessor.processSignal(signal);
            
            // Don't propagate - this is the final destination for signals
            return;
        }
        
        if (msg instanceof InboundMethodReturn) {
            InboundMethodReturn response = (InboundMethodReturn) msg;
            
            // Handle method response in application logic
            handleMethodResponse(response);
            
            // Don't propagate - application consumed the response
            return;
        }
        
        if (msg instanceof InboundError) {
            InboundError error = (InboundError) msg;
            
            // Handle error in application logic
            handleError(error);
            
            // Don't propagate - application handled the error
            return;
        }
        
        // Unknown message type - propagate in case other handlers need it
        ctx.propagateInboundMessage(msg);
    }
    
    private void handleMethodResponse(InboundMethodReturn response) {
        // Application-specific response handling
    }
    
    private void handleError(InboundError error) {
        // Application-specific error handling
    }
}
```

### Example 4: Request/Response Correlation Handler

```java
public class RequestCorrelationHandler extends AbstractDuplexHandler {
    private final Map<UInt32, CompletableFuture<InboundMessage>> pendingRequests = 
        new ConcurrentHashMap<>();
    
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg,
                                    CompletableFuture<Void> writeFuture) {
        if (msg instanceof OutboundMethodCall) {
            OutboundMethodCall call = (OutboundMethodCall) msg;
            if (call.isReplyExpected()) {
                // Store the correlation for response matching
                CompletableFuture<InboundMessage> responseFuture = new CompletableFuture<>();
                pendingRequests.put(call.getSerial(), responseFuture);
                
                // Set up timeout handling
                scheduleTimeout(call.getSerial(), Duration.ofSeconds(30));
            }
        }
        
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundReply) {
            InboundReply reply = (InboundReply) msg;
            UInt32 replySerial = reply.getReplySerial();
            
            CompletableFuture<InboundMessage> pendingRequest = 
                pendingRequests.remove(replySerial);
                
            if (pendingRequest != null) {
                pendingRequest.complete(msg);
                // Don't propagate - this handler is the final destination
                return;
            }
        }
        
        ctx.propagateInboundMessage(msg);
    }
    
    private void scheduleTimeout(UInt32 serial, Duration timeout) {
        // Implementation would use a scheduled executor
        // to timeout requests that don't receive responses
    }
}
```

## Handler Lifecycle

### Lifecycle Events

Handlers participate in the following lifecycle events:

1. **Handler Added**: Called when handler is added to pipeline
2. **Channel Active**: Called when connection is established
3. **Message Processing**: Process inbound/outbound messages
4. **Exception Caught**: Handle any exceptions
5. **Channel Inactive**: Called when connection is closed
6. **Handler Removed**: Called when handler is removed

### Lifecycle Example

```java
public class LifecycleAwareHandler extends AbstractInboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAwareHandler.class);
    
    @Override
    public void handlerAdded(Context ctx) {
        LOGGER.info("Handler added to pipeline: {}", ctx.getName());
        super.handlerAdded(ctx);
    }
    
    @Override
    public void channelActive(Context ctx) {
        LOGGER.info("Connection established: {}", ctx.getConnection());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(Context ctx) {
        LOGGER.info("Connection closed: {}", ctx.getConnection());
        super.channelInactive(ctx);
    }
    
    @Override
    public void handlerRemoved(Context ctx) {
        LOGGER.info("Handler removed from pipeline: {}", ctx.getName());
        super.handlerRemoved(ctx);
    }
    
    @Override
    public void handleExceptionCaught(Context ctx, Throwable cause) {
        LOGGER.error("Exception in handler: {}", ctx.getName(), cause);
        super.handleExceptionCaught(ctx, cause);
    }
}
```

## Handler Best Practices

### 1. Propagate Messages Correctly

**Key Rule**: Only propagate inbound messages if the current handler is **not** the final destination for the message.

```java
// Good - Handler processes but doesn't consume the message
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    logMessage(msg);  // Just logging, not consuming
    ctx.propagateInboundMessage(msg);  // Continue to next handler
}

// Good - Handler consumes the message (no propagation)
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    if (msg instanceof InboundMethodReturn) {
        InboundMethodReturn response = (InboundMethodReturn) msg;
        if (pendingRequests.containsKey(response.getReplySerial())) {
            // This handler is the final destination for this message
            CompletableFuture<InboundMessage> future = 
                pendingRequests.remove(response.getReplySerial());
            future.complete(msg);
            return;  // Don't propagate - we consumed it
        }
    }
    // Not for us, pass it along
    ctx.propagateInboundMessage(msg);
}

// Bad - Handler consumes message but still propagates
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    if (isForThisHandler(msg)) {
        processAndConsume(msg);
        ctx.propagateInboundMessage(msg);  // Wrong! Don't propagate consumed messages
    }
}

// Bad - Handler doesn't propagate when it should
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    processMessage(msg);  // Just processing, not consuming
    // Missing propagation! Other handlers won't see this message
}
```

**When to propagate**:
- ✅ Message logging/monitoring handlers
- ✅ Message filtering handlers (when message passes filter)
- ✅ Message transformation handlers
- ✅ Authentication/authorization handlers (when validation passes)

**When NOT to propagate**:
- ❌ Request/response correlation handlers (when handling their own responses)
- ❌ Application logic handlers (when message is final destination)
- ❌ Message filtering handlers (when message is filtered out)
- ❌ Error handlers (when handling application-specific errors)

### 2. Handle Exceptions Gracefully

```java
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    try {
        processMessage(msg);
    } catch (Exception e) {
        LOGGER.error("Error processing message", e);
        // Still propagate the original message
    }
    ctx.propagateInboundMessage(msg);
}
```

### 3. Use Appropriate Handler Types

- Use `InboundHandler` for message consumption/filtering
- Use `OutboundHandler` for message modification/enhancement
- Use `DuplexHandler` for request/response correlation

### 4. Resource Management

```java
public class ResourceManagedHandler extends AbstractInboundHandler {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Override
    public void handlerRemoved(Context ctx) {
        executor.shutdown();
        super.handlerRemoved(ctx);
    }
}
```

### 5. Thread Safety

Handlers may be called from multiple threads, so ensure thread safety:

```java
public class ThreadSafeHandler extends AbstractInboundHandler {
    private final AtomicLong messageCount = new AtomicLong(0);
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        long count = messageCount.incrementAndGet();
        LOGGER.debug("Processed {} messages", count);
        ctx.propagateInboundMessage(msg);
    }
}
```

### 6. Message Propagation Decision Tree

Use this decision tree to determine whether to propagate messages:

```
Is this handler the final destination for this message?
├── YES: Don't propagate (return without calling ctx.propagateInboundMessage())
│   ├── Application logic handlers consuming messages
│   ├── Request/response correlation handlers handling their responses
│   └── Filtering handlers rejecting messages
└── NO: Propagate (call ctx.propagateInboundMessage(msg))
    ├── Logging and monitoring handlers
    ├── Message transformation handlers
    ├── Authentication/authorization handlers (when passing)
    └── Filtering handlers allowing messages through
```

### 7. Common Propagation Patterns

```java
// Pattern 1: Always propagate (monitoring/logging)
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    monitor(msg);
    ctx.propagateInboundMessage(msg);
}

// Pattern 2: Conditional propagation (filtering)
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    if (shouldProcess(msg)) {
        ctx.propagateInboundMessage(msg);
    }
    // Filtered messages are not propagated
}

// Pattern 3: Selective consumption (correlation)
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    if (isForThisHandler(msg)) {
        consume(msg);
        return; // Don't propagate consumed messages
    }
    ctx.propagateInboundMessage(msg);
}

// Pattern 4: Terminal consumption (application logic)
@Override
public void handleInboundMessage(Context ctx, InboundMessage msg) {
    processInApplication(msg);
    // Don't propagate - application is the final destination
}
```

## Pipeline Management

### Adding Handlers

```java
Pipeline pipeline = connection.getPipeline();

// Add at the end
pipeline.addLast("logger", new MessageLoggingHandler());

// Add at specific position
pipeline.addBefore("logger", "filter", new SignalFilterHandler(interfaces));

// Add at the beginning
pipeline.addFirst("security", new SecurityHandler());
```

### Removing Handlers

```java
// Remove by name
pipeline.remove("logger");

// Remove by class
pipeline.remove(MessageLoggingHandler.class);

// Remove by instance
pipeline.remove(handlerInstance);
```

### Handler Ordering

The order of handlers in the pipeline matters:

```java
pipeline.addLast("authentication", new AuthenticationHandler());
pipeline.addLast("authorization", new AuthorizationHandler());
pipeline.addLast("logging", new LoggingHandler());
pipeline.addLast("application", new ApplicationHandler());
```

## Advanced Patterns

### Conditional Message Processing

```java
public class ConditionalHandler extends AbstractInboundHandler {
    private final Predicate<InboundMessage> condition;
    
    public ConditionalHandler(Predicate<InboundMessage> condition) {
        this.condition = condition;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (condition.test(msg)) {
            processMessage(msg);
        }
        ctx.propagateInboundMessage(msg);
    }
}
```

### Message Transformation

```java
public class MessageTransformHandler extends AbstractDuplexHandler {
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage original,
                                    CompletableFuture<Void> writeFuture) {
        OutboundMessage transformed = transformMessage(original);
        ctx.propagateOutboundMessage(transformed, writeFuture);
    }
    
    private OutboundMessage transformMessage(OutboundMessage msg) {
        // Transform the message as needed
        return msg;
    }
}
```

### State Management

```java
public class StatefulHandler extends AbstractInboundHandler {
    private volatile State currentState = State.INITIAL;
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        State newState = processMessage(msg, currentState);
        currentState = newState;
        ctx.propagateInboundMessage(msg);
    }
    
    private enum State {
        INITIAL, AUTHENTICATED, READY, ERROR
    }
}
```

The handler architecture provides a powerful and flexible way to extend the D-Bus client library. By following these patterns and best practices, you can create robust and maintainable message processing logic that integrates seamlessly with the library's core functionality.