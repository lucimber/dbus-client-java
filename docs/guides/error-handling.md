# Error Handling Guide

This guide covers comprehensive error handling strategies for the D-Bus Client Java library. We'll explore different types of errors, how to handle them gracefully, and patterns for building resilient D-Bus applications.

## Error Categories

### 1. Connection Errors

These occur at the network and connection level:

```java
// Connection establishment failures
try {
    connection.connect().toCompletableFuture().get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause();
    if (cause instanceof ConnectException) {
        // D-Bus daemon not running or not accessible
        log.error("Cannot connect to D-Bus daemon: {}", cause.getMessage());
        handleConnectionFailure();
    } else if (cause instanceof AuthFailedException) {
        // SASL authentication failed
        log.error("D-Bus authentication failed: {}", cause.getMessage());
        handleAuthenticationFailure();
    } else if (cause instanceof TimeoutException) {
        // Connection timeout
        log.error("Connection timed out: {}", cause.getMessage());
        handleConnectionTimeout();
    }
}
```

### 2. Protocol Errors

These occur due to D-Bus protocol violations:

```java
// Invalid message format or protocol violations
public class ProtocolErrorHandler extends AbstractInboundHandler {
    @Override
    public void handleExceptionCaught(Context ctx, Throwable cause) {
        if (cause instanceof InvalidMessageException) {
            log.error("Invalid D-Bus message received: {}", cause.getMessage());
            // Close connection as protocol state may be corrupted
            ctx.getConnection().close();
        } else if (cause instanceof DecoderException) {
            log.error("Failed to decode D-Bus message: {}", cause.getMessage());
            // Message decoding failed, but connection may still be valid
        }
        
        ctx.propagateExceptionCaught(cause);
    }
}
```

### 3. Method Call Errors

These are application-level errors returned by D-Bus services:

```java
public CompletableFuture<String> callMethodWithErrorHandling(OutboundMethodCall call) {
    return connection.sendRequest(call)
        .thenCompose(response -> {
            if (response instanceof InboundError) {
                InboundError error = (InboundError) response;
                String errorName = error.getErrorName().toString();
                
                switch (errorName) {
                    case "org.freedesktop.DBus.Error.ServiceUnknown":
                        return CompletableFuture.failedFuture(
                            new ServiceNotFoundException("Service not available"));
                    
                    case "org.freedesktop.DBus.Error.AccessDenied":
                        return CompletableFuture.failedFuture(
                            new SecurityException("Access denied"));
                    
                    case "org.freedesktop.DBus.Error.InvalidArgs":
                        return CompletableFuture.failedFuture(
                            new IllegalArgumentException("Invalid arguments"));
                    
                    default:
                        return CompletableFuture.failedFuture(
                            new DBusException("D-Bus error: " + errorName));
                }
            } else if (response instanceof InboundMethodReturn) {
                return CompletableFuture.completedFuture(extractResult(response));
            } else {
                return CompletableFuture.failedFuture(
                    new DBusException("Unexpected response type: " + response.getClass()));
            }
        });
}
```

### 4. Timeout Errors

Handle method call timeouts gracefully:

```java
public CompletableFuture<String> callWithTimeout(OutboundMethodCall call, Duration timeout) {
    return connection.sendRequest(call)
        .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
        .exceptionally(throwable -> {
            if (throwable instanceof TimeoutException) {
                log.warn("Method call timed out after {}: {}", 
                        timeout, call.getMember());
                throw new DBusTimeoutException("Operation timed out", throwable);
            } else if (throwable instanceof CompletionException) {
                // Unwrap completion exception
                Throwable cause = throwable.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new DBusException("Method call failed", cause);
                }
            } else {
                throw new DBusException("Unexpected error", throwable);
            }
        });
}
```

## Error Handling Patterns

### 1. Retry Pattern

Implement exponential backoff for transient failures:

```java
@Component
public class RetryableDBusService {
    private final Connection connection;
    private final int maxRetries;
    private final Duration initialDelay;
    
    public RetryableDBusService(Connection connection) {
        this.connection = connection;
        this.maxRetries = 3;
        this.initialDelay = Duration.ofMillis(500);
    }
    
    public CompletableFuture<InboundMessage> sendWithRetry(OutboundMethodCall call) {
        return sendWithRetry(call, 0);
    }
    
    private CompletableFuture<InboundMessage> sendWithRetry(OutboundMethodCall call, int attempt) {
        return connection.sendRequest(call)
            .exceptionally(throwable -> {
                if (attempt < maxRetries && isRetryableError(throwable)) {
                    Duration delay = calculateDelay(attempt);
                    log.warn("Retry attempt {} after {}ms due to: {}", 
                            attempt + 1, delay.toMillis(), throwable.getMessage());
                    
                    return CompletableFuture
                        .delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                        .execute(() -> sendWithRetry(call, attempt + 1))
                        .join();
                } else {
                    throw new DBusException("Failed after " + attempt + " retries", throwable);
                }
            });
    }
    
    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof TimeoutException ||
               throwable instanceof ConnectException ||
               (throwable instanceof DBusException && 
                throwable.getMessage().contains("Service temporarily unavailable"));
    }
    
    private Duration calculateDelay(int attempt) {
        // Exponential backoff with jitter
        long delay = initialDelay.toMillis() * (1L << attempt);
        long jitter = (long) (delay * 0.1 * Math.random());
        return Duration.ofMillis(delay + jitter);
    }
}
```

### 2. Circuit Breaker Pattern

Prevent cascading failures with circuit breaker:

```java
@Component
public class CircuitBreakerDBusService {
    
    public enum State { CLOSED, OPEN, HALF_OPEN }
    
    private volatile State state = State.CLOSED;
    private volatile int failureCount = 0;
    private volatile long lastFailureTime = 0;
    private final int failureThreshold = 5;
    private final Duration openTimeout = Duration.ofMinutes(1);
    
    public CompletableFuture<InboundMessage> sendWithCircuitBreaker(OutboundMethodCall call) {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > openTimeout.toMillis()) {
                state = State.HALF_OPEN;
                log.info("Circuit breaker transitioning to HALF_OPEN");
            } else {
                return CompletableFuture.failedFuture(
                    new CircuitBreakerOpenException("Circuit breaker is OPEN"));
            }
        }
        
        return connection.sendRequest(call)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleFailure();
                } else {
                    handleSuccess();
                }
            });
    }
    
    private void handleFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
            log.warn("Circuit breaker opened after {} failures", failureCount);
        }
    }
    
    private void handleSuccess() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            log.info("Circuit breaker closed after successful call");
        }
        failureCount = 0;
    }
}
```

### 3. Bulkhead Pattern

Isolate different types of operations:

```java
@Component
public class BulkheadDBusService {
    private final Executor criticalOperationExecutor;
    private final Executor normalOperationExecutor;
    private final Executor backgroundOperationExecutor;
    
    public BulkheadDBusService() {
        this.criticalOperationExecutor = 
            Executors.newFixedThreadPool(2, r -> new Thread(r, "critical-dbus"));
        this.normalOperationExecutor = 
            Executors.newFixedThreadPool(5, r -> new Thread(r, "normal-dbus"));
        this.backgroundOperationExecutor = 
            Executors.newCachedThreadPool(r -> new Thread(r, "background-dbus"));
    }
    
    public CompletableFuture<InboundMessage> sendCriticalOperation(OutboundMethodCall call) {
        return CompletableFuture
            .supplyAsync(() -> connection.sendRequest(call), criticalOperationExecutor)
            .thenCompose(Function.identity());
    }
    
    public CompletableFuture<InboundMessage> sendNormalOperation(OutboundMethodCall call) {
        return CompletableFuture
            .supplyAsync(() -> connection.sendRequest(call), normalOperationExecutor)
            .thenCompose(Function.identity());
    }
    
    public CompletableFuture<InboundMessage> sendBackgroundOperation(OutboundMethodCall call) {
        return CompletableFuture
            .supplyAsync(() -> connection.sendRequest(call), backgroundOperationExecutor)
            .thenCompose(Function.identity());
    }
}
```

## Comprehensive Error Handler

Create a centralized error handler for consistent error processing:

```java
@Component
public class DBusErrorHandler extends AbstractInboundHandler {
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    
    public DBusErrorHandler(ApplicationEventPublisher eventPublisher, MeterRegistry meterRegistry) {
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }
    
    @Override
    public void handleExceptionCaught(Context ctx, Throwable cause) {
        // Increment error metrics
        Counter.builder("dbus.errors")
            .tag("type", cause.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
        
        // Log error with appropriate level
        if (cause instanceof TimeoutException) {
            log.warn("D-Bus operation timed out: {}", cause.getMessage());
        } else if (cause instanceof AuthFailedException) {
            log.error("D-Bus authentication failed: {}", cause.getMessage());
            eventPublisher.publishEvent(new DBusAuthenticationFailedEvent(cause));
        } else if (cause instanceof ConnectException) {
            log.error("D-Bus connection failed: {}", cause.getMessage());
            eventPublisher.publishEvent(new DBusConnectionFailedEvent(cause));
        } else if (cause instanceof InvalidMessageException) {
            log.error("Invalid D-Bus message: {}", cause.getMessage());
            // Protocol error - close connection
            ctx.getConnection().close();
        } else {
            log.error("Unexpected D-Bus error", cause);
        }
        
        // Determine if error should be propagated
        if (shouldPropagateError(cause)) {
            ctx.propagateExceptionCaught(cause);
        }
    }
    
    private boolean shouldPropagateError(Throwable cause) {
        // Don't propagate expected errors that are handled locally
        return !(cause instanceof TimeoutException) &&
               !(cause instanceof InvalidMessageException);
    }
}
```

## Graceful Degradation

Implement fallback mechanisms when D-Bus is unavailable:

```java
@Service
public class ResilientSystemInfoService {
    private final Connection dbusConnection;
    private final SystemInfoCache cache;
    private final FallbackSystemInfoProvider fallback;
    
    public CompletableFuture<SystemInfo> getSystemInfo() {
        return getSystemInfoFromDBus()
            .exceptionally(throwable -> {
                log.warn("D-Bus unavailable, trying cache: {}", throwable.getMessage());
                return cache.getSystemInfo();
            })
            .exceptionally(throwable -> {
                log.warn("Cache unavailable, using fallback: {}", throwable.getMessage());
                return fallback.getSystemInfo();
            });
    }
    
    private CompletableFuture<SystemInfo> getSystemInfoFromDBus() {
        if (!dbusConnection.isConnected()) {
            return CompletableFuture.failedFuture(
                new DBusException("D-Bus connection not available"));
        }
        
        OutboundMethodCall call = createSystemInfoCall();
        return dbusConnection.sendRequest(call)
            .orTimeout(5, TimeUnit.SECONDS)
            .thenApply(this::parseSystemInfo)
            .thenApply(info -> {
                cache.put(info); // Update cache with fresh data
                return info;
            });
    }
}
```

## Connection Recovery

Implement automatic connection recovery with exponential backoff:

```java
@Component
public class DBusConnectionManager {
    private final Connection connection;
    private final ScheduledExecutorService scheduler;
    private volatile boolean reconnecting = false;
    
    @EventListener
    public void handleConnectionLost(DBusConnectionLostEvent event) {
        if (!reconnecting) {
            scheduleReconnection(Duration.ofSeconds(1), 0);
        }
    }
    
    private void scheduleReconnection(Duration delay, int attempt) {
        if (attempt >= 10) {
            log.error("Failed to reconnect after {} attempts, giving up", attempt);
            return;
        }
        
        reconnecting = true;
        
        scheduler.schedule(() -> {
            try {
                log.info("Attempting to reconnect (attempt {})", attempt + 1);
                connection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
                
                log.info("Successfully reconnected to D-Bus");
                reconnecting = false;
                
            } catch (Exception e) {
                log.warn("Reconnection attempt {} failed: {}", attempt + 1, e.getMessage());
                
                // Exponential backoff
                Duration nextDelay = Duration.ofMillis(
                    Math.min(delay.toMillis() * 2, 60000) // Max 1 minute
                );
                
                scheduleReconnection(nextDelay, attempt + 1);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
}
```

## Error Monitoring and Alerting

Set up comprehensive monitoring for D-Bus errors:

```java
@Component
public class DBusErrorMonitor {
    private final MeterRegistry meterRegistry;
    private final AlertingService alertingService;
    
    private final Counter connectionErrors;
    private final Counter authenticationErrors;
    private final Counter timeoutErrors;
    private final Timer errorRecoveryTime;
    
    public DBusErrorMonitor(MeterRegistry meterRegistry, AlertingService alertingService) {
        this.meterRegistry = meterRegistry;
        this.alertingService = alertingService;
        
        this.connectionErrors = Counter.builder("dbus.errors.connection").register(meterRegistry);
        this.authenticationErrors = Counter.builder("dbus.errors.authentication").register(meterRegistry);
        this.timeoutErrors = Counter.builder("dbus.errors.timeout").register(meterRegistry);
        this.errorRecoveryTime = Timer.builder("dbus.recovery.time").register(meterRegistry);
    }
    
    @EventListener
    public void handleConnectionError(DBusConnectionFailedEvent event) {
        connectionErrors.increment();
        
        // Alert if error rate is high
        if (connectionErrors.count() > 10) { // Within the last minute
            alertingService.sendAlert(
                "High D-Bus connection error rate",
                "Connection errors: " + connectionErrors.count()
            );
        }
    }
    
    @EventListener
    public void handleAuthenticationError(DBusAuthenticationFailedEvent event) {
        authenticationErrors.increment();
        
        // Authentication errors are always critical
        alertingService.sendAlert(
            "D-Bus authentication failure",
            "Authentication failed: " + event.getCause().getMessage()
        );
    }
    
    @EventListener
    public void handleTimeout(DBusTimeoutEvent event) {
        timeoutErrors.increment();
    }
    
    @EventListener
    public void handleRecovery(DBusConnectionRecoveredEvent event) {
        errorRecoveryTime.record(event.getRecoveryDuration());
    }
}
```

## Testing Error Conditions

Test error handling scenarios:

```java
@SpringBootTest
class ErrorHandlingTest {
    
    @MockBean
    private Connection mockConnection;
    
    @Autowired
    private SystemInfoService service;
    
    @Test
    void testTimeoutHandling() {
        // Given
        CompletableFuture<InboundMessage> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.completeExceptionally(new TimeoutException("Operation timed out"));
        
        when(mockConnection.sendRequest(any())).thenReturn(timeoutFuture);
        
        // When & Then
        assertThatThrownBy(() -> service.getSystemInfo().get())
            .hasCauseInstanceOf(DBusTimeoutException.class);
    }
    
    @Test
    void testConnectionFailureHandling() {
        // Given
        when(mockConnection.isConnected()).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> service.getSystemInfo().get())
            .hasCauseInstanceOf(DBusException.class)
            .hasMessageContaining("connection not available");
    }
    
    @Test
    void testErrorResponseHandling() {
        // Given
        InboundError errorResponse = mock(InboundError.class);
        when(errorResponse.getErrorName()).thenReturn(DBusString.valueOf("org.freedesktop.DBus.Error.AccessDenied"));
        when(mockConnection.sendRequest(any())).thenReturn(CompletableFuture.completedFuture(errorResponse));
        
        // When & Then
        assertThatThrownBy(() -> service.getSystemInfo().get())
            .hasCauseInstanceOf(SecurityException.class);
    }
}
```

## Best Practices Summary

1. **Error Classification**: Distinguish between recoverable and non-recoverable errors
2. **Retry Logic**: Implement exponential backoff for transient failures
3. **Circuit Breakers**: Prevent cascading failures in distributed systems
4. **Graceful Degradation**: Provide fallback mechanisms when D-Bus is unavailable
5. **Monitoring**: Track error rates and recovery times
6. **Alerting**: Set up alerts for critical error conditions
7. **Testing**: Test all error scenarios thoroughly
8. **Documentation**: Document error conditions and recovery procedures
9. **Logging**: Use appropriate log levels for different error types
10. **Resource Cleanup**: Ensure resources are cleaned up properly during error conditions

By implementing these error handling patterns, your D-Bus applications will be more resilient and provide better user experience even when facing network issues, service unavailability, or other error conditions.