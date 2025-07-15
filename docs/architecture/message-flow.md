# Message Flow Architecture

This document details how D-Bus messages flow through the library, from network bytes to application code and back. Understanding this flow is essential for developing custom handlers and troubleshooting integration issues.

## Overview

The D-Bus client library processes messages through a multi-layered pipeline that handles:
- Network-level byte streams
- D-Bus protocol framing
- Message parsing and validation
- Type marshalling/unmarshalling
- Handler pipeline processing
- Application-level message delivery

## Complete Message Flow Diagram

```
Inbound Message Flow (Receiving):
┌─────────────────┐
│  Network Layer  │ Raw bytes from D-Bus daemon
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Frame Decoder  │ D-Bus message framing
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Message Decoder │ Parse D-Bus message structure
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Type Decoders   │ Convert D-Bus types to Java objects
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Handler Pipeline│ User-defined message processing
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Application    │ Final message delivery
└─────────────────┘

Outbound Message Flow (Sending):
┌─────────────────┐
│  Application    │ Application creates outbound message
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Handler Pipeline│ User-defined message processing
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Message Encoder │ Build D-Bus message structure
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Type Encoders   │ Convert Java objects to D-Bus types
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Frame Encoder  │ D-Bus message framing
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Network Layer  │ Send bytes to D-Bus daemon
└─────────────────┘
```

## Detailed Layer Analysis

### 1. Network Layer

**Responsibilities**:
- TCP socket or Unix domain socket communication
- Raw byte stream handling
- Connection establishment and maintenance

**Components**:
- Netty's `NioSocketChannel` or `EpollDomainSocketChannel`
- SASL authentication handlers
- Connection state management

**Data Format**: Raw byte streams

### 2. Frame Layer

**Responsibilities**:
- D-Bus message boundary detection
- Endianness handling
- Message size validation

**Inbound Frame Decoding**:
```java
public class FrameDecoder extends LengthFieldBasedFrameDecoder {
    // D-Bus message structure:
    // - 1 byte: Endianness flag
    // - 1 byte: Message type
    // - 1 byte: Flags
    // - 1 byte: Major protocol version
    // - 4 bytes: Message body length
    // - 4 bytes: Serial number
    // - Variable: Header fields
    // - Variable: Body (aligned to 8-byte boundary)
}
```

**Outbound Frame Encoding**:
```java
public class FrameEncoder extends MessageToByteEncoder<OutboundMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, OutboundMessage msg, ByteBuf out) {
        // Write D-Bus message header and body
        writeHeader(msg, out);
        writeBody(msg, out);
    }
}
```

**Data Format**: Complete D-Bus message frames

### 3. Message Layer

**Responsibilities**:
- D-Bus message structure parsing
- Header field extraction
- Message type identification
- Serial number management

**Inbound Message Structure**:
```java
// Raw D-Bus message components
class RawDBusMessage {
    byte endianness;          // 'l' (little) or 'B' (big)
    MessageType type;         // METHOD_CALL, METHOD_RETURN, SIGNAL, ERROR
    Set<MessageFlag> flags;   // NO_REPLY_EXPECTED, NO_AUTO_START, etc.
    byte majorVersion;        // Protocol version (currently 1)
    int bodyLength;           // Length of message body
    UInt32 serial;           // Message serial number
    Map<HeaderField, Object> headers; // Header fields
    ByteBuffer body;         // Message body
}
```

**Message Type Mapping**:
```java
public enum MessageType {
    METHOD_CALL(1),    // -> InboundMethodCall / OutboundMethodCall
    METHOD_RETURN(2),  // -> InboundMethodReturn / OutboundMethodReturn
    ERROR(3),          // -> InboundError / OutboundError
    SIGNAL(4);         // -> InboundSignal / OutboundSignal
}
```

**Data Format**: Structured message objects with parsed headers

### 4. Type Layer

**Responsibilities**:
- D-Bus type marshalling/unmarshalling
- Type signature parsing
- Data alignment handling
- Container type processing

**Type Encoding Process**:
```java
public class TypeEncoder {
    public EncoderResult encode(DBusType value, ByteBuffer buffer, int offset) {
        // 1. Handle alignment requirements
        int alignedOffset = TypeAlignment.align(offset, value.getType());
        
        // 2. Encode based on type
        if (value instanceof DBusBasicType) {
            return encodeBasicType((DBusBasicType) value, buffer, alignedOffset);
        } else if (value instanceof DBusContainerType) {
            return encodeContainerType((DBusContainerType) value, buffer, alignedOffset);
        }
        
        // 3. Return encoding result
        return new EncoderResultImpl(bytesWritten, newOffset);
    }
}
```

**Type Decoding Process**:
```java
public class TypeDecoder {
    public DecoderResult<DBusType> decode(Type type, ByteBuffer buffer, int offset) {
        // 1. Handle alignment requirements
        int alignedOffset = TypeAlignment.align(offset, type);
        
        // 2. Decode based on type
        switch (type) {
            case STRING: return StringDecoder.decode(buffer, alignedOffset);
            case INT32: return Int32Decoder.decode(buffer, alignedOffset);
            case ARRAY: return ArrayDecoder.decode(type, buffer, alignedOffset);
            // ... other types
        }
    }
}
```

**Data Format**: Type-safe Java objects (DBusString, DBusInt32, etc.)

### 5. Handler Pipeline Layer

**Responsibilities**:
- User-defined message processing
- Message filtering and routing
- Request/response correlation
- Application-specific logic

**Pipeline Processing**:
```java
public class DefaultPipeline implements Pipeline {
    private final List<HandlerEntry> handlers = new ArrayList<>();
    
    public void fireInboundMessage(InboundMessage msg) {
        InternalContext ctx = new InternalContext(this, 0);
        ctx.propagateInboundMessage(msg);
    }
    
    public void fireOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future) {
        InternalContext ctx = new InternalContext(this, handlers.size() - 1);
        ctx.propagateOutboundMessage(msg, future);
    }
}
```

**Handler Invocation**:
```java
class InternalContext implements Context {
    @Override
    public void propagateInboundMessage(InboundMessage msg) {
        HandlerEntry next = findNextInboundHandler();
        if (next != null) {
            next.handler.handleInboundMessage(next.context, msg);
        }
    }
}
```

**Data Format**: Structured message objects ready for application processing

## Message Flow Examples

### Example 1: Inbound Method Call

```
1. Network Layer:
   Raw bytes: [6c 01 00 01 ...] (little-endian method call)

2. Frame Layer:
   Decoded frame: 108 bytes, method call message

3. Message Layer:
   Parsed message:
   - Type: METHOD_CALL
   - Serial: 123
   - Path: /org/example/Object
   - Interface: org.example.Interface
   - Member: TestMethod

4. Type Layer:
   Decoded arguments:
   - String: "hello"
   - Int32: 42

5. Handler Pipeline:
   - LoggingHandler: logs the method call
   - AuthorizationHandler: checks permissions
   - ApplicationHandler: processes the call

6. Application:
   Receives InboundMethodCall with typed arguments
```

### Example 2: Outbound Signal

```
1. Application:
   Creates OutboundSignal:
   - Path: /org/example/Object
   - Interface: org.example.Interface
   - Member: StatusChanged
   - Arguments: [String("active"), Int32(100)]

2. Handler Pipeline:
   - ApplicationHandler: validates signal
   - LoggingHandler: logs the signal

3. Type Layer:
   Encodes arguments:
   - "active" -> D-Bus string encoding
   - 100 -> D-Bus int32 encoding

4. Message Layer:
   Builds D-Bus message:
   - Header with path, interface, member
   - Body with encoded arguments

5. Frame Layer:
   Creates D-Bus frame with proper length and alignment

6. Network Layer:
   Sends bytes: [6c 04 01 01 ...] to D-Bus daemon
```

## Error Handling in Message Flow

### Network-Level Errors

```java
// Connection failures, timeouts, I/O errors
public void handleNetworkError(Throwable cause) {
    if (cause instanceof IOException) {
        // Trigger reconnection logic
        connectionManager.scheduleReconnect();
    }
    fireExceptionCaught(cause);
}
```

### Protocol-Level Errors

```java
// Invalid frames, protocol violations
public void handleProtocolError(InvalidMessageException cause) {
    // Log protocol error
    LOGGER.error("Protocol error: {}", cause.getMessage());
    
    // Close connection
    connection.close();
}
```

### Application-Level Errors

```java
// Handler errors, processing failures
public void handleApplicationError(Throwable cause) {
    // Create error reply if responding to method call
    if (currentMessage instanceof InboundMethodCall) {
        OutboundError error = createErrorReply(cause);
        sendMessage(error);
    }
}
```

## Performance Considerations

### Memory Management

```java
// Efficient buffer handling
public class OptimizedMessageProcessor {
    private final ByteBufferPool bufferPool = new ByteBufferPool();
    
    public void processMessage(ByteBuffer data) {
        ByteBuffer buffer = bufferPool.acquire();
        try {
            // Process message
        } finally {
            bufferPool.release(buffer);
        }
    }
}
```

### Zero-Copy Optimizations

```java
// Direct buffer usage to avoid copying
public EncoderResult encode(DBusType value, ByteBuffer directBuffer) {
    // Write directly to the buffer without intermediate copies
    return encoder.encodeDirect(value, directBuffer);
}
```

### Async Processing

```java
// Non-blocking message processing
public CompletableFuture<InboundMessage> sendMethodCall(OutboundMethodCall call) {
    CompletableFuture<InboundMessage> future = new CompletableFuture<>();
    
    // Register for response
    responseTracker.register(call.getSerial(), future);
    
    // Send asynchronously
    pipeline.fireOutboundMessage(call, new CompletableFuture<>());
    
    return future;
}
```

## Debugging Message Flow

### Enable Wire-Level Logging

```xml
<!-- logback-test.xml -->
<logger name="com.lucimber.dbus.netty" level="DEBUG"/>
<logger name="com.lucimber.dbus.decoder" level="TRACE"/>
<logger name="com.lucimber.dbus.encoder" level="TRACE"/>
```

### Message Flow Tracing

```java
public class TracingHandler extends AbstractDuplexHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        LOGGER.debug("INBOUND[{}]: {} -> {}", 
                    ctx.getName(), msg.getType(), msg.getSerial());
        ctx.propagateInboundMessage(msg);
    }
    
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg, 
                                    CompletableFuture<Void> writeFuture) {
        LOGGER.debug("OUTBOUND[{}]: {} -> {}", 
                    ctx.getName(), msg.getType(), msg.getSerial());
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
}
```

### Performance Monitoring

```java
public class PerformanceMonitoringHandler extends AbstractDuplexHandler {
    private final Timer inboundTimer = Timer.start();
    private final Timer outboundTimer = Timer.start();
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        Timer.Sample sample = Timer.start();
        try {
            ctx.propagateInboundMessage(msg);
        } finally {
            sample.stop(inboundTimer);
        }
    }
}
```

Understanding the message flow architecture is crucial for effective use of the D-Bus client library. This knowledge enables you to:
- Create efficient custom handlers
- Debug message processing issues
- Optimize performance for your use case
- Extend the library with new functionality