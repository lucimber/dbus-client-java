# Using Standard D-Bus Interfaces

This guide explains how to use the standard D-Bus interfaces provided by the library.

## Two Approaches

The library provides two ways to implement standard D-Bus interfaces:

1. **Manual Implementation** - Direct handling of method calls in your handler
2. **Annotation-Based** - Use annotations and let the framework handle the details

## Available Standard Interfaces

The library provides Java interfaces for the following standard D-Bus interfaces:

1. **Introspectable** (`org.freedesktop.DBus.Introspectable`)
   - Discover interfaces, methods, signals, and properties of D-Bus objects
   - Single method: `introspect()` returns XML description

2. **Properties** (`org.freedesktop.DBus.Properties`)
   - Get, set, and monitor properties of D-Bus objects
   - Methods: `getProperty()`, `setProperty()`, `getProperties()`
   - Signal: `PropertiesChanged`

3. **Peer** (`org.freedesktop.DBus.Peer`)
   - Basic peer-to-peer functionality
   - Methods: `ping()`, `getMachineId()`

4. **ObjectManager** (`org.freedesktop.DBus.ObjectManager`)
   - Efficiently enumerate object hierarchies
   - Method: `getManagedObjects()`
   - Signals: `InterfacesAdded`, `InterfacesRemoved`

## Client-Side Usage

### Basic Example

```java
// Create connection
Connection connection = NettyConnection.newConnection(socketAddress, config);
connection.connect().toCompletableFuture().get();

// 1. Introspection
OutboundMethodCall introspectCall = OutboundMethodCall.Builder.create()
    .withPath(DBusObjectPath.valueOf("/org/example/Object"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
    .withMember(DBusString.valueOf("Introspect"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .withReplyExpected(true)
    .build();

CompletableFuture<InboundMessage> response = connection.sendRequest(introspectCall);
// Process XML response

// 2. Get Property
OutboundMethodCall getPropertyCall = OutboundMethodCall.Builder.create()
    .withPath(DBusObjectPath.valueOf("/org/example/Object"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
    .withMember(DBusString.valueOf("Get"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .withBody(Arrays.asList(
        DBusString.valueOf("org.example.Interface"),
        DBusString.valueOf("PropertyName")
    ))
    .withReplyExpected(true)
    .build();

// 3. Ping
OutboundMethodCall pingCall = OutboundMethodCall.Builder.create()
    .withPath(DBusObjectPath.valueOf("/"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
    .withMember(DBusString.valueOf("Ping"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .withReplyExpected(true)
    .build();
```

### Running the Client Example

```bash
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.StandardInterfacesExample
```

## Server-Side Implementation

### Approach 1: Manual Implementation

While the library provides the interface definitions, you need to handle the method calls in your service:

```java
public class StandardInterfacesHandler implements InboundHandler {
    @Override
    public void handleInboundMessage(PipelineContext ctx, InboundMessage msg) {
        if (msg instanceof InboundMethodCall) {
            InboundMethodCall call = (InboundMethodCall) msg;
            String interfaceName = call.getInterfaceName()
                .map(DBusString::toString)
                .orElse("");
            
            switch (interfaceName) {
                case "org.freedesktop.DBus.Introspectable":
                    handleIntrospectable(ctx, call);
                    break;
                case "org.freedesktop.DBus.Properties":
                    handleProperties(ctx, call);
                    break;
                case "org.freedesktop.DBus.Peer":
                    handlePeer(ctx, call);
                    break;
                case "org.freedesktop.DBus.ObjectManager":
                    handleObjectManager(ctx, call);
                    break;
                default:
                    ctx.propagateInboundMessage(msg);
            }
        }
    }
}
```

### Approach 2: Annotation-Based Implementation (Recommended)

Use annotations to define your D-Bus service and let the framework handle standard interfaces:

```java
@DBusInterface("com.example.MyService")
public class MyService {
    
    @DBusProperty(name = "Version")
    private String version = "1.0.0";
    
    @DBusProperty
    private int count = 0;
    
    @DBusMethod(name = "Echo")
    public String echo(String message) {
        count++;
        return message;
    }
    
    @DBusMethod
    public void reset() {
        count = 0;
    }
    
    @DBusSignal(name = "CountChanged")
    public void emitCountChanged(int oldValue, int newValue) {
        // Signal emission handled by framework
    }
}

// Register the service
MyService service = new MyService();
StandardInterfaceHandler handler = new StandardInterfaceHandler(
    "/com/example/MyService", service);
connection.getPipeline().addLast("myservice", handler);
```

The `StandardInterfaceHandler` automatically provides:
- **Introspectable**: Generates XML from annotations
- **Properties**: Get/Set/GetAll for `@DBusProperty` fields/methods
- **Peer**: Standard Ping and GetMachineId implementation

### Running the Server Example

1. Start the manual implementation server:
```bash
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.StandardInterfacesServer
```

2. Or start the annotation-based server:
```bash
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.AnnotationBasedService
```

3. In another terminal, test with the client:
```bash
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.StandardInterfacesExample
```

3. Or use D-Bus tools:
```bash
# Introspect
dbus-send --session --print-reply --dest=com.example.StandardInterfacesDemo \
  /com/example/Demo org.freedesktop.DBus.Introspectable.Introspect

# Get property
dbus-send --session --print-reply --dest=com.example.StandardInterfacesDemo \
  /com/example/Demo org.freedesktop.DBus.Properties.Get \
  string:"com.example.Demo" string:"Version"

# Ping
dbus-send --session --print-reply --dest=com.example.StandardInterfacesDemo \
  / org.freedesktop.DBus.Peer.Ping

# Get managed objects
dbus-send --session --print-reply --dest=com.example.StandardInterfacesDemo \
  /com/example/Demo org.freedesktop.DBus.ObjectManager.GetManagedObjects
```

## Best Practices

1. **Always implement Introspectable** - It helps clients discover your service's capabilities
2. **Use Properties for configuration** - It provides a standard way to get/set values
3. **Implement Peer.Ping for health checks** - Clients can verify your service is responsive
4. **Use ObjectManager for complex hierarchies** - More efficient than multiple introspection calls

## Common Patterns

### Property Change Notifications

```java
// Send PropertiesChanged signal when a property changes
OutboundSignal signal = OutboundSignal.Builder.create()
    .withPath(DBusObjectPath.valueOf("/com/example/Object"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
    .withMember(DBusString.valueOf("PropertiesChanged"))
    .withBody(Arrays.asList(
        DBusString.valueOf("com.example.Interface"),
        changedProperties,  // Dict of changed properties
        invalidatedProperties  // Array of invalidated property names
    ))
    .build();
```

### Service Discovery

```java
// List all services
OutboundMethodCall listNames = OutboundMethodCall.Builder.create()
    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
    .withMember(DBusString.valueOf("ListNames"))
    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
    .withReplyExpected(true)
    .build();
```

## Type Mappings

When working with standard interfaces, use these D-Bus type mappings:

| D-Bus Type | Java Type | Example |
|------------|-----------|---------|
| `s` (string) | `DBusString` | Method names, property names |
| `v` (variant) | `DBusVariant` | Property values |
| `o` (object path) | `DBusObjectPath` | Object paths |
| `a{sv}` | `DBusDict<DBusString, DBusVariant>` | Property dictionaries |
| `as` | `DBusArray<DBusString>` | String arrays |

## Error Handling

Standard D-Bus errors to handle:

- `org.freedesktop.DBus.Error.UnknownInterface` - Interface not found
- `org.freedesktop.DBus.Error.UnknownMethod` - Method not found
- `org.freedesktop.DBus.Error.UnknownProperty` - Property not found
- `org.freedesktop.DBus.Error.PropertyReadOnly` - Attempt to set read-only property
- `org.freedesktop.DBus.Error.InvalidArgs` - Invalid method arguments

## Integration with Existing Code

The standard interfaces work alongside your custom interfaces:

```xml
<!-- Introspection XML includes both standard and custom interfaces -->
<node>
  <interface name="org.freedesktop.DBus.Properties">
    <!-- Standard properties interface -->
  </interface>
  <interface name="com.example.MyCustomInterface">
    <!-- Your custom interface -->
  </interface>
</node>
```

This allows clients to use standard tools and libraries to interact with your service while still providing custom functionality.