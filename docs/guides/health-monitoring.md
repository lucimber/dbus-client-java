# D-Bus Connection Health Monitoring

The D-Bus client provides comprehensive connection health monitoring and automatic reconnection capabilities to ensure reliable communication and automatic failure recovery.

## Overview

The health monitoring system includes:
- **Periodic Health Checks**: Automatic ping operations to verify connection health
- **Connection State Tracking**: Detailed state management (CONNECTED, UNHEALTHY, DISCONNECTED, etc.)
- **Event Notifications**: Real-time events for state changes and health check results
- **Configurable Intervals**: Customizable health check frequency and timeouts
- **Automatic Reconnection**: Exponential backoff reconnection when connection is lost
- **Reconnection Management**: Configurable retry limits and backoff strategies

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
    .withAutoReconnectEnabled(true)                  // Enable automatic reconnection
    .withReconnectInitialDelay(Duration.ofSeconds(1)) // Initial delay before first reconnect
    .withReconnectMaxDelay(Duration.ofMinutes(5))    // Maximum delay between reconnects
    .withReconnectBackoffMultiplier(2.0)             // Exponential backoff multiplier
    .withMaxReconnectAttempts(10)                    // Maximum number of reconnect attempts
    .build();

Connection connection = NettyConnection.newSystemBusConnection(config);
```

### Default Settings

| Setting | Default Value | Description |
|---------|---------------|-------------|
| Health Check Enabled | `true` | Whether health monitoring is active |
| Health Check Interval | 30 seconds | Time between health checks |
| Health Check Timeout | 5 seconds | Timeout for individual health checks |
| Auto Reconnect Enabled | `true` | Whether automatic reconnection is active |
| Reconnect Initial Delay | 1 second | Initial delay before first reconnect attempt |
| Reconnect Max Delay | 5 minutes | Maximum delay between reconnect attempts |
| Reconnect Backoff Multiplier | 2.0 | Exponential backoff multiplier |
| Max Reconnect Attempts | 10 | Maximum number of reconnection attempts (0 = unlimited) |

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
        case RECONNECTION_ATTEMPT:
            System.out.println("Reconnection attempt started");
            break;
        case RECONNECTION_SUCCESS:
            System.out.println("Reconnection succeeded");
            break;
        case RECONNECTION_FAILURE:
            System.out.println("Reconnection failed: " + 
                event.getCause().orElse(null));
            break;
        case RECONNECTION_EXHAUSTED:
            System.out.println("Maximum reconnection attempts reached");
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

## Reconnection Management

Monitor and control automatic reconnection:

```java
// Check reconnection status
int attempts = connection.getReconnectAttemptCount();
System.out.println("Reconnection attempts: " + attempts);

// Cancel pending reconnection
connection.cancelReconnection();

// Reset reconnection state
connection.resetReconnectionState();
```

## Health Check Mechanism

The health monitor uses the standard D-Bus `Peer.Ping` method:

1. **Ping Method**: Sends `org.freedesktop.DBus.Peer.Ping` to the D-Bus daemon
2. **Timeout Handling**: Fails if no response within configured timeout
3. **State Updates**: Updates connection state based on success/failure
4. **Event Firing**: Notifies listeners of health check results

## Integration with Pipeline

The health monitoring and reconnection systems are implemented as pipeline handlers that:
- **ConnectionHealthHandler**: Monitors connection health using D-Bus Peer.Ping
- **ConnectionReconnectHandler**: Handles automatic reconnection with exponential backoff
- Both integrate seamlessly with the existing pipeline architecture
- Intercept and handle connection events appropriately
- Fire events asynchronously to avoid blocking
- Properly manage resources and cleanup

## Automatic Reconnection

The automatic reconnection system:
1. **Monitors Connection Events**: Listens for connection failures and state changes
2. **Exponential Backoff**: Uses configurable backoff strategy to avoid overwhelming the server
3. **Retry Limits**: Respects maximum attempt limits to prevent infinite loops
4. **Event Firing**: Notifies listeners of reconnection attempts and results
5. **State Management**: Properly manages connection state during reconnection

### Backoff Strategy

The reconnection delay follows this formula:
```
delay = min(initial_delay * (backoff_multiplier ^ attempt_number), max_delay)
```

Example with default settings:
- Attempt 1: 1 second
- Attempt 2: 2 seconds  
- Attempt 3: 4 seconds
- Attempt 4: 8 seconds
- ...
- Capped at 5 minutes maximum

## Best Practices

1. **Configure Appropriate Intervals**: Balance between responsiveness and resource usage
2. **Monitor Health Events**: Implement listeners for production monitoring
3. **Handle State Transitions**: React appropriately to different connection states
4. **Resource Cleanup**: Ensure proper connection cleanup when shutting down
5. **Reconnection Limits**: Set reasonable maximum reconnection attempts to prevent infinite loops
6. **Backoff Configuration**: Use exponential backoff to avoid overwhelming the server during failures
7. **Event Handling**: Implement robust event listeners to handle reconnection scenarios

## Example: Complete Health Monitoring Setup

```java
// Create configuration with health monitoring and reconnection
ConnectionConfig config = ConnectionConfig.builder()
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(15))
    .withHealthCheckTimeout(Duration.ofSeconds(3))
    .withAutoReconnectEnabled(true)
    .withReconnectInitialDelay(Duration.ofSeconds(2))
    .withReconnectMaxDelay(Duration.ofMinutes(2))
    .withReconnectBackoffMultiplier(2.0)
    .withMaxReconnectAttempts(5)
    .build();

// Create connection
Connection connection = NettyConnection.newSystemBusConnection(config);

// Add event listener
connection.addConnectionEventListener((conn, event) -> {
    System.out.println("Connection event: " + event);
    
    switch (event.getType()) {
        case STATE_CHANGED:
            ConnectionState newState = event.getNewState().orElse(null);
            if (newState == ConnectionState.UNHEALTHY) {
                handleUnhealthyConnection();
            } else if (newState == ConnectionState.RECONNECTING) {
                System.out.println("Reconnecting... (attempt " + conn.getReconnectAttemptCount() + ")");
            }
            break;
        case RECONNECTION_SUCCESS:
            System.out.println("Reconnection successful!");
            break;
        case RECONNECTION_EXHAUSTED:
            System.out.println("Max reconnection attempts reached, manual intervention required");
            break;
    }
});

// Connect and use
connection.connect().thenRun(() -> {
    System.out.println("Connected with health monitoring and auto-reconnection active");
});
```

This comprehensive health monitoring and reconnection system provides robust connection reliability for production D-Bus applications.