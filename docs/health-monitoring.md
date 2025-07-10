# D-Bus Connection Health Monitoring

The D-Bus client provides comprehensive connection health monitoring capabilities to ensure reliable communication and automatic failure detection.

## Overview

The health monitoring system includes:
- **Periodic Health Checks**: Automatic ping operations to verify connection health
- **Connection State Tracking**: Detailed state management (CONNECTED, UNHEALTHY, DISCONNECTED, etc.)
- **Event Notifications**: Real-time events for state changes and health check results
- **Configurable Intervals**: Customizable health check frequency and timeouts

## Connection States

The connection can be in one of the following states:

| State | Description |
|-------|-------------|
| `DISCONNECTED` | No connection established (initial state) |
| `CONNECTING` | Connection establishment in progress |
| `AUTHENTICATING` | SASL authentication in progress |
| `CONNECTED` | Fully operational and healthy |
| `UNHEALTHY` | Connected but health checks are failing |
| `RECONNECTING` | Attempting to reconnect after failure |
| `FAILED` | Connection failed permanently |

## Configuration

Health monitoring is configured through the `ConnectionConfig` builder:

```java
ConnectionConfig config = ConnectionConfig.builder()
    .withHealthCheckEnabled(true)                    // Enable health monitoring
    .withHealthCheckInterval(Duration.ofSeconds(30)) // Check every 30 seconds
    .withHealthCheckTimeout(Duration.ofSeconds(5))   // 5 second timeout per check
    .build();

Connection connection = NettyConnection.newSystemBusConnection(config);
```

### Default Settings

| Setting | Default Value | Description |
|---------|---------------|-------------|
| Health Check Enabled | `true` | Whether health monitoring is active |
| Health Check Interval | 30 seconds | Time between health checks |
| Health Check Timeout | 5 seconds | Timeout for individual health checks |

## Event Monitoring

Register event listeners to receive notifications about connection health:

```java
connection.addConnectionEventListener((conn, event) -> {
    switch (event.getType()) {
        case STATE_CHANGED:
            System.out.println("Connection state: " + 
                event.getOldState().orElse(null) + " -> " + 
                event.getNewState().orElse(null));
            break;
        case HEALTH_CHECK_SUCCESS:
            System.out.println("Health check passed");
            break;
        case HEALTH_CHECK_FAILURE:
            System.out.println("Health check failed: " + 
                event.getCause().orElse(null));
            break;
    }
});
```

## Manual Health Checks

Trigger health checks manually when needed:

```java
// Trigger a health check
CompletableFuture<Void> healthCheck = connection.triggerHealthCheck();

healthCheck.whenComplete((result, error) -> {
    if (error != null) {
        System.err.println("Health check failed: " + error);
    } else {
        System.out.println("Health check completed");
    }
});
```

## Connection State Access

Query the current connection state:

```java
ConnectionState state = connection.getState();

if (state.canHandleRequests()) {
    // Connection is healthy enough to send requests
    sendDbusMessage();
} else if (state.isTransitioning()) {
    // Connection is being established or re-established
    waitForConnection();
} else if (state.isFailed()) {
    // Connection has failed permanently
    handleConnectionFailure();
}
```

## Health Check Mechanism

The health monitor uses the standard D-Bus `Peer.Ping` method:

1. **Ping Method**: Sends `org.freedesktop.DBus.Peer.Ping` to the D-Bus daemon
2. **Timeout Handling**: Fails if no response within configured timeout
3. **State Updates**: Updates connection state based on success/failure
4. **Event Firing**: Notifies listeners of health check results

## Integration with Pipeline

The health monitoring is implemented as a pipeline handler (`ConnectionHealthHandler`) that:
- Integrates seamlessly with the existing pipeline architecture
- Intercepts and handles health check responses
- Fires events asynchronously to avoid blocking
- Properly manages resources and cleanup

## Best Practices

1. **Configure Appropriate Intervals**: Balance between responsiveness and resource usage
2. **Monitor Health Events**: Implement listeners for production monitoring
3. **Handle State Transitions**: React appropriately to different connection states
4. **Resource Cleanup**: Ensure proper connection cleanup when shutting down

## Example: Complete Health Monitoring Setup

```java
// Create configuration with health monitoring
ConnectionConfig config = ConnectionConfig.builder()
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(15))
    .withHealthCheckTimeout(Duration.ofSeconds(3))
    .build();

// Create connection
Connection connection = NettyConnection.newSystemBusConnection(config);

// Add event listener
connection.addConnectionEventListener((conn, event) -> {
    System.out.println("Connection event: " + event);
    
    if (event.getType() == ConnectionEventType.STATE_CHANGED) {
        ConnectionState newState = event.getNewState().orElse(null);
        if (newState == ConnectionState.UNHEALTHY) {
            // Handle unhealthy connection
            handleUnhealthyConnection();
        }
    }
});

// Connect and use
connection.connect().thenRun(() -> {
    System.out.println("Connected with health monitoring active");
});
```

This health monitoring system provides robust connection reliability for production D-Bus applications.