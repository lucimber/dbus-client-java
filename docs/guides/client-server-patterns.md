# Client-Server Patterns in D-Bus Client Java

This guide clarifies the different components for implementing D-Bus clients and servers.

## Overview

The library provides complementary components for both sides of D-Bus communication:

| Component | Side | Purpose | Use When |
|-----------|------|---------|----------|
| **ServiceProxy** | Client | Call remote D-Bus services | You want to invoke methods on existing D-Bus services |
| **StandardInterfaceHandler** | Server | Implement D-Bus services | You want to expose your Java objects as D-Bus services |
| **Manual Message Building** | Both | Full control | You need complex scenarios like signals or custom protocols |

## Client-Side: Consuming D-Bus Services

### Simple Request/Response with ServiceProxy

For basic client scenarios where you only need to call methods and get responses:

```java
// 1. Define interface matching the D-Bus service
@DBusInterface("org.freedesktop.DBus")
public interface DBusService {
    @DBusMethod("GetId")
    String getId();
    
    @DBusMethod("ListNames")
    CompletableFuture<String[]> listNames();
}

// 2. Create proxy
DBusService service = ServiceProxy.create(
    connection,
    "/org/freedesktop/DBus",
    DBusService.class
);

// 3. Call methods like regular Java
String id = service.getId();
```

**Limitations of ServiceProxy:**
- ❌ Cannot receive D-Bus signals
- ❌ Cannot implement services (server-side)
- ❌ Limited argument marshalling support
- ✅ Perfect for simple method calls

### Advanced Client with Manual Message Building

For complex client scenarios including signals:

```java
// Method call
OutboundMethodCall call = OutboundMethodCall.Builder.create()
    .withPath(DBusObjectPath.valueOf("/org/example/Object"))
    .withInterface(DBusString.valueOf("org.example.Interface"))
    .withMember(DBusString.valueOf("Method"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .build();

// Signal handler
connection.getPipeline().addLast("signal-handler", new AbstractInboundHandler() {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundSignal) {
            // Handle signal
        }
        ctx.propagateInboundMessage(msg);
    }
});
```

## Server-Side: Implementing D-Bus Services

### Using StandardInterfaceHandler

For exposing Java objects as D-Bus services with standard interfaces:

```java
// 1. Create service class with annotations
@DBusInterface("com.example.Calculator")
public class CalculatorService {
    @DBusProperty
    private String version = "1.0.0";
    
    @DBusMethod
    public int add(int a, int b) {
        return a + b;
    }
}

// 2. Register handler
CalculatorService service = new CalculatorService();
StandardInterfaceHandler handler = new StandardInterfaceHandler(
    "/com/example/Calculator",
    service
);
connection.getPipeline().addLast("calculator", handler);
```

**StandardInterfaceHandler provides:**
- ✅ Automatic introspection (org.freedesktop.DBus.Introspectable)
- ✅ Property access (org.freedesktop.DBus.Properties)
- ✅ Peer interface (org.freedesktop.DBus.Peer)
- ✅ Annotation-based configuration

### Custom Service Implementation

For full control over service behavior:

```java
public class CustomServiceHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundMethodCall) {
            InboundMethodCall call = (InboundMethodCall) msg;
            
            // Check if this is for our service
            if (!"/my/object/path".equals(call.getObjectPath().toString())) {
                ctx.propagateInboundMessage(msg);
                return;
            }
            
            // Handle method call
            String method = call.getMember().toString();
            switch (method) {
                case "MyMethod":
                    // Send response
                    OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
                        .withReplySerial(call.getSerial())
                        .withBody(/* ... */)
                        .build();
                    ctx.propagateOutboundMessage(reply, new CompletableFuture<>());
                    break;
                default:
                    // Send error
                    break;
            }
        }
    }
}
```

## Complete Example: Calculator Service

Here's a complete example showing both server and client:

### Server Implementation

```java
// Server side - implement the service
@DBusInterface("com.example.Calculator")
public class CalculatorImpl {
    @DBusMethod
    public int add(int a, int b) {
        return a + b;
    }
    
    @DBusMethod
    public double divide(double a, double b) {
        if (b == 0) throw new IllegalArgumentException("Division by zero");
        return a / b;
    }
}

// Register the service
StandardInterfaceHandler handler = new StandardInterfaceHandler(
    "/com/example/Calculator",
    new CalculatorImpl()
);
serverConnection.getPipeline().addLast("calc", handler);
```

### Client Implementation

```java
// Client side - define the same interface
@DBusInterface("com.example.Calculator")
public interface Calculator {
    @DBusMethod("add")
    int add(int a, int b);
    
    @DBusMethod("divide")
    CompletableFuture<Double> divide(double a, double b);
}

// Create proxy and use it
Calculator calc = ServiceProxy.create(
    clientConnection,
    "com.example.Calculator",  // destination
    "/com/example/Calculator",  // object path
    Calculator.class
);

// Synchronous call
int sum = calc.add(5, 3);  // Returns 8

// Asynchronous call
calc.divide(10, 2)
    .thenAccept(result -> System.out.println("Result: " + result))
    .exceptionally(error -> {
        System.err.println("Division failed: " + error);
        return null;
    });
```

## Decision Tree

```
Need to work with D-Bus?
├── Client (calling services)
│   ├── Simple method calls only?
│   │   └── Use ServiceProxy ✓
│   └── Need signals or complex scenarios?
│       └── Use manual message building
└── Server (implementing services)
    ├── Standard D-Bus interfaces needed?
    │   └── Use StandardInterfaceHandler ✓
    └── Custom protocol or behavior?
        └── Implement custom handler
```

## Best Practices

1. **Use annotations consistently** - Define interfaces once, use for both client and server
2. **Start simple** - Use ServiceProxy and StandardInterfaceHandler first
3. **Graduate to manual** - Only when you need signals or custom behavior
4. **Keep interfaces focused** - Small, cohesive D-Bus interfaces are easier to maintain
5. **Handle errors gracefully** - Both components support proper error propagation