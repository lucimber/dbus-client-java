# D-Bus Timeout Configuration Example

This example demonstrates the new timeout configuration features added to the D-Bus client.

## Connection-Level Timeout Configuration

```java
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.netty.NettyConnection;
import java.time.Duration;

// Create custom connection configuration
ConnectionConfig config = ConnectionConfig.builder()
    .withMethodCallTimeout(Duration.ofSeconds(15))    // 15 second method call timeout
    .withConnectTimeout(Duration.ofSeconds(5))        // 5 second connection timeout
    .withReadTimeout(Duration.ofSeconds(30))          // 30 second read timeout
    .withWriteTimeout(Duration.ofSeconds(8))          // 8 second write timeout
    .build();

// Create connection with custom configuration
Connection connection = NettyConnection.newSystemBusConnection(config);

// All method calls will use the 15-second timeout by default
```

## Per-Call Timeout Override

```java
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.*;
import java.time.Duration;

// Create a method call with custom timeout override
UInt32 serial = connection.getNextSerial();
OutboundMethodCall urgentCall = OutboundMethodCall.Builder
    .create()
    .withSerial(serial)
    .withPath(ObjectPath.valueOf("/org/example/Service"))
    .withMember(DBusString.valueOf("UrgentMethod"))
    .withDestination(DBusString.valueOf("org.example.Service"))
    .withInterface(DBusString.valueOf("org.example.Interface"))
    .withReplyExpected(true)
    .withTimeout(Duration.ofSeconds(3))  // Override with 3-second timeout
    .build();

// This call will timeout after 3 seconds instead of the connection default
CompletionStage<InboundMessage> reply = connection.sendRequest(urgentCall);
```

## Default Configuration

If no configuration is provided, the following defaults are used:

- **Method Call Timeout**: 30 seconds
- **Connection Timeout**: 10 seconds  
- **Read Timeout**: 60 seconds
- **Write Timeout**: 10 seconds

```java
// Using defaults
Connection connection = NettyConnection.newSystemBusConnection();
// Equivalent to:
Connection connection = NettyConnection.newSystemBusConnection(ConnectionConfig.defaultConfig());
```

## Timeout Behavior

- **Method Call Timeout**: How long to wait for a D-Bus method call reply before giving up
- **Connection Timeout**: How long to wait when establishing the initial D-Bus connection
- **Read Timeout**: How long to wait for data to be received from the D-Bus daemon
- **Write Timeout**: How long to wait for data to be sent to the D-Bus daemon

When a timeout occurs, a `TimeoutException` is thrown with details about which operation timed out and the duration that was exceeded.