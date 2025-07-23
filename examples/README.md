# D-Bus Client Java Examples

This directory contains examples demonstrating how to use the D-Bus Client Java library.

## Examples Overview

### Core Examples

#### 1. SimpleAnnotationExample
**File:** `SimpleAnnotationExample.java`  
**Purpose:** Demonstrates the annotation-based approach to implementing D-Bus services.

This example shows how to:
- Create a D-Bus service using annotations (`@DBusInterface`, `@DBusProperty`)
- Register the service with `StandardInterfaceHandler` 
- Get automatic implementations of standard D-Bus interfaces (Introspectable, Properties, Peer)

**Key Features:**
- Properties automatically exposed via D-Bus Properties interface
- Introspection XML generated from annotations
- Standard Peer interface (Ping, GetMachineId) provided automatically

#### 2. SimpleClientExample
**File:** `SimpleClientExample.java`  
**Purpose:** Demonstrates how to interact with D-Bus services as a client.

This example shows how to:
- Connect to the D-Bus session bus
- Call standard D-Bus methods (introspection, properties, ping)
- Handle responses and errors properly
- List available D-Bus services

#### 3. AuthenticationExample
**File:** `AuthenticationExample.java`  
**Purpose:** Shows different SASL authentication mechanisms.

#### 4. BasicClientExample
**File:** `BasicClientExample.java`  
**Purpose:** Basic client operations and message handling.

#### 5. ServiceDiscoveryExample
**File:** `ServiceDiscoveryExample.java`  
**Purpose:** Discovering and interacting with D-Bus services.

#### 6. SignalHandlingExample
**File:** `SignalHandlingExample.java`  
**Purpose:** Handling D-Bus signals and event-driven programming.

#### 7. SimpleReactiveExample
**File:** `SimpleReactiveExample.java`  
**Purpose:** Reactive programming patterns with D-Bus.

#### 8. SpringBootIntegrationExample
**File:** `SpringBootIntegrationExample.java`  
**Purpose:** Integration with Spring Boot applications.

### New 2.0 Features Examples

#### 9. ConnectionHealthExample
**File:** `ConnectionHealthExample.java`  
**Purpose:** Demonstrates connection health monitoring and event handling.

This example shows how to:
- Monitor connection state changes with event listeners
- Handle connection health events and failures
- Configure connection timeouts and retry logic
- Implement graceful shutdown and reconnection

**Key Features:**
- Comprehensive event handling for all connection states
- Health check monitoring with configurable intervals
- Automatic reconnection with exponential backoff
- Graceful error handling and logging

#### 10. DummyConnectionTestingExample
**File:** `DummyConnectionTestingExample.java`  
**Purpose:** Unit testing with DummyConnection for isolated testing.

This example shows how to:
- Use DummyConnection for unit testing without a real D-Bus daemon
- Test custom handlers in isolation
- Simulate various D-Bus scenarios (success, error, timeout)
- Verify handler behavior programmatically

**Key Features:**
- Complete isolation from D-Bus daemon for reliable unit tests
- Custom handler testing with mock responses
- Error scenario simulation and testing
- Programmatic verification of message handling

#### 11. TransportStrategiesExample
**File:** `TransportStrategiesExample.java`  
**Purpose:** Different transport strategies and SASL configurations.

This example shows how to:
- Use Unix domain socket transport (standard for system/session bus)
- Configure TCP transport for remote D-Bus connections
- Set up different SASL authentication mechanisms (EXTERNAL, COOKIE, ANONYMOUS)
- Handle transport-specific connection options

**Key Features:**
- Strategy pattern implementation for pluggable transports
- Advanced TCP configuration with keep-alive and timeouts
- Type-safe SASL configuration classes
- Comprehensive transport option demonstrations

## Running the Examples

### Prerequisites
- Java 17 or later
- D-Bus daemon running (usually automatic on Linux/macOS)
- Access to D-Bus session bus

### Core Examples

#### Running the Annotation-Based Service

```bash
# Start the annotation-based service
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.SimpleAnnotationExample

# The service will start and display:
# 🚀 Simple Annotation-based D-Bus Service Started
#    Service Name: com.example.SimpleService
#    Object Path:  /com/example/Simple
```

#### Running the Client

```bash
# In another terminal, run the client
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.SimpleClientExample

# The client will:
# 🔗 Connected to D-Bus session bus
# 🔍 Testing introspection on org.freedesktop.DBus/org/freedesktop/DBus
# 🏓 Testing ping to org.freedesktop.DBus
# 📋 Listing available D-Bus services...
# 🔍 Testing annotation-based service (if running)...
```

### New 2.0 Features Examples

#### Running the Connection Health Example

```bash
# Monitor connection health and events
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.ConnectionHealthExample

# Output will show:
# 🔍 D-Bus Connection Health Monitoring Example
# 📡 Connecting to system D-Bus...
# ✅ Connected successfully!
# 🔄 State change: null → CONNECTED
# ⏳ Monitoring connection health for 60 seconds...
```

#### Running the DummyConnection Testing Example

```bash
# Demonstrate unit testing with DummyConnection
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.DummyConnectionTestingExample

# Output will show:
# 🧪 D-Bus Unit Testing with DummyConnection Example
# 📋 Test 1: Basic DummyConnection Usage
# 🔗 Connecting to DummyConnection...
# ✅ Connected successfully!
# 📤 Sending method call: GetVersion
# 📥 Received reply: OutboundMethodReturn
```

#### Running the Transport Strategies Example

```bash
# Explore different transport configurations
./gradlew :examples:run -PmainClass=com.lucimber.dbus.examples.TransportStrategiesExample

# Output will show:
# 🚀 D-Bus Transport Strategies Example
# 📋 Example 1: Unix Domain Socket Connections
# 🔗 Connecting to session bus...
# ✅ Session bus connected successfully!
# 📞 Testing Session Bus with basic D-Bus call...
```

### Testing with D-Bus Tools

You can also test the annotation service using standard D-Bus command-line tools:

```bash
# Introspect the service
dbus-send --session --print-reply \
  --dest=com.example.SimpleService /com/example/Simple \
  org.freedesktop.DBus.Introspectable.Introspect

# Get all properties
dbus-send --session --print-reply \
  --dest=com.example.SimpleService /com/example/Simple \
  org.freedesktop.DBus.Properties.GetAll \
  string:"com.example.Calculator"

# Get specific property
dbus-send --session --print-reply \
  --dest=com.example.SimpleService /com/example/Simple \
  org.freedesktop.DBus.Properties.Get \
  string:"com.example.Calculator" string:"Version"

# Test connectivity
dbus-send --session --print-reply \
  --dest=com.example.SimpleService /com/example/Simple \
  org.freedesktop.DBus.Peer.Ping
```

## Understanding the Annotation Framework

### Basic Usage

1. **Annotate your service class:**
```java
@DBusInterface("com.example.MyService")
public class MyService {
    @DBusProperty(name = "Version")
    private String version = "1.0.0";
    
    @DBusProperty
    private int count = 0;
}
```

2. **Register with StandardInterfaceHandler:**
```java
MyService service = new MyService();
StandardInterfaceHandler handler = new StandardInterfaceHandler("/com/example/MyService", service);
connection.getPipeline().addLast("myservice", handler);
```

3. **Standard interfaces provided automatically:**
- `org.freedesktop.DBus.Introspectable` - XML generated from annotations
- `org.freedesktop.DBus.Properties` - Get/GetAll access to `@DBusProperty` fields
- `org.freedesktop.DBus.Peer` - Standard Ping and GetMachineId implementation

### Available Annotations

- `@DBusInterface(name)` - Marks class as D-Bus interface
- `@DBusProperty(name, access)` - Marks field as D-Bus property
- `@DBusMethod(name)` - Marks method as D-Bus method (for future implementation)
- `@DBusSignal(name)` - Marks method as D-Bus signal emitter (for future implementation)

### Property Access Modes

```java
@DBusProperty(access = DBusProperty.Access.READ)     // Read-only
@DBusProperty(access = DBusProperty.Access.WRITE)    // Write-only  
@DBusProperty(access = DBusProperty.Access.READWRITE) // Read-write
@DBusProperty(access = DBusProperty.Access.AUTO)     // Auto-detect (default)
```

## Type Mapping

The framework automatically converts between Java types and D-Bus types:

| Java Type | D-Bus Type | Notes |
|-----------|------------|-------|
| `String` | `s` (string) | UTF-8 strings |
| `int`/`Integer` | `i` (int32) | 32-bit signed integer |
| `boolean`/`Boolean` | `b` (boolean) | Boolean value |
| `AtomicInteger` | `i` (int32) | Thread-safe integer (converted via `get()`) |

## Architecture

The annotation framework works by:

1. **Scanning** - Reflects on your annotated classes to discover interfaces, properties, methods
2. **Introspection** - Generates D-Bus introspection XML from the discovered metadata
3. **Property Access** - Uses reflection to get/set annotated field values
4. **Message Handling** - Automatically handles standard D-Bus interface method calls

## Best Practices

1. **Thread Safety** - Ensure your service objects are thread-safe (D-Bus calls can come from multiple threads)
2. **Property Naming** - Use PascalCase for D-Bus property names: `@DBusProperty(name = "MyProperty")`
3. **Interface Naming** - Use reverse domain notation: `@DBusInterface("com.example.MyService")`
4. **Resource Management** - Always close D-Bus connections in finally blocks
5. **Error Handling** - Handle connection and method call exceptions appropriately

## Troubleshooting

### Service Not Found
- Ensure the service is running before testing
- Check that the service name was registered successfully
- Verify D-Bus session bus is accessible

### Permission Denied
- Check D-Bus policies and permissions
- Ensure you're using the correct bus (session vs system)

### Connection Issues
- Verify D-Bus daemon is running
- Check network connectivity for TCP connections
- Ensure socket permissions for Unix domain sockets

## Future Enhancements

The annotation framework will be extended to support:
- Custom method invocation (`@DBusMethod`)
- Signal emission (`@DBusSignal`) 
- ObjectManager interface support
- Advanced type mapping
- Method parameter validation

For more information, see the [Standard Interfaces Documentation](../docs/standard-interfaces.md).