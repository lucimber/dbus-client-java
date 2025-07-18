# Handling Blocking Operations in D-Bus Handlers

This guide explains how to safely handle blocking operations (database calls, REST APIs, file I/O) when implementing D-Bus handlers. The D-Bus client library is specifically designed to support blocking operations through its dual-pipeline architecture with strict thread isolation.

## TL;DR - The Good News

**✅ Blocking operations are completely safe in D-Bus handlers!**

Your handlers run on the `ApplicationTaskExecutor` thread pool, not the Netty event loop, so you can safely perform:
- Database operations
- REST API calls  
- File I/O operations
- Any other blocking operations

## Understanding the Thread Architecture

The D-Bus client library uses a sophisticated dual-pipeline architecture with strict thread isolation:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Your Handler Code                          │
│                 (ApplicationTaskExecutor)                      │
│                                                                 │
│  ✅ Database calls, REST APIs, file I/O - ALL SAFE TO BLOCK    │
│                                                                 │
│  public void handleInboundMessage(Context ctx, InboundMessage msg) {
│      // This runs on ApplicationTaskExecutor - blocking is safe!
│      String result = blockingDatabaseCall(msg);
│      ctx.propagateInboundMessage(enrichMessage(msg, result));
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │ RealityCheckpoint Bridge
                                 │ (Thread switching happens here)
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Netty Event Loop                            │
│                  (Never blocked by your code)                  │
│                                                                 │
│  Network I/O, Protocol handling, SASL authentication          │
│  High-performance, non-blocking operations                     │
└─────────────────────────────────────────────────────────────────┘
```

### Thread Configuration

The default configuration provides a properly sized thread pool for blocking operations:

```java
// Default ApplicationTaskExecutor configuration
applicationTaskExecutor = Executors.newFixedThreadPool(
    Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
    runnable -> {
        Thread t = new Thread(runnable, "dbus-app-worker-" + hash);
        t.setDaemon(true);  // Proper shutdown handling
        return t;
    });
```

**Key characteristics:**
- **Pool size**: `max(2, CPU_cores / 2)` - sufficient for blocking operations
- **Thread names**: `"dbus-app-worker-*"` - easy to identify in thread dumps
- **Daemon threads**: Proper application shutdown behavior
- **Isolated**: Completely separate from Netty event loop threads

## Safe Blocking Patterns

### Database Operations

Database calls are completely safe and straightforward in D-Bus handlers:

```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseHandler extends AbstractInboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);
    private final DataSource dataSource;
    
    public DatabaseHandler(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // ✅ SAFE - Database operations are fine here
        try (Connection conn = dataSource.getConnection()) {
            // Extract user ID from D-Bus message
            int userId = extractUserId(msg);
            
            // Perform database query (blocking operation)
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT username, email, status FROM users WHERE id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String status = rs.getString("status");
                
                // Process user data and continue message processing
                InboundMessage enrichedMsg = enrichWithUserData(msg, username, email, status);
                ctx.propagateInboundMessage(enrichedMsg);
            } else {
                // User not found - propagate error
                ctx.propagateInboundFailure(new UserNotFoundException("User ID: " + userId));
            }
            
        } catch (SQLException e) {
            LOGGER.error("Database operation failed for user ID: {}", extractUserId(msg), e);
            ctx.propagateInboundFailure(e);
        }
    }
    
    private int extractUserId(InboundMessage msg) {
        // Extract user ID from D-Bus message body
        // Implementation depends on your message format
        return 0; // Placeholder
    }
    
    private InboundMessage enrichWithUserData(InboundMessage msg, String username, String email, String status) {
        // Create enriched message with user data
        // Implementation depends on your message format
        return msg; // Placeholder
    }
}
```

### REST API Calls

External API calls are also completely safe:

```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ExternalApiHandler extends AbstractInboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalApiHandler.class);
    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    
    public ExternalApiHandler(RestTemplate restTemplate, String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // Extract data from D-Bus message
            String deviceId = extractDeviceId(msg);
            
            // ✅ SAFE - HTTP calls are fine here
            String url = apiBaseUrl + "/devices/" + deviceId + "/status";
            ResponseEntity<DeviceStatus> response = restTemplate.getForEntity(
                url, DeviceStatus.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                DeviceStatus status = response.getBody();
                
                // Process API response
                if (status != null && status.isOnline()) {
                    // Device is online - continue processing
                    InboundMessage enrichedMsg = enrichWithDeviceStatus(msg, status);
                    ctx.propagateInboundMessage(enrichedMsg);
                } else {
                    // Device is offline - handle accordingly
                    handleOfflineDevice(ctx, msg, deviceId);
                }
            } else {
                LOGGER.warn("API call failed with status: {}", response.getStatusCode());
                ctx.propagateInboundFailure(new ApiException("API call failed"));
            }
            
        } catch (RestClientException e) {
            LOGGER.error("REST API call failed", e);
            ctx.propagateInboundFailure(e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in API handler", e);
            ctx.propagateInboundFailure(e);
        }
    }
    
    private void handleOfflineDevice(Context ctx, InboundMessage msg, String deviceId) {
        // Handle offline device scenario
        LOGGER.info("Device {} is offline, routing to offline handler", deviceId);
        // Could route to a different handler or send notification
        ctx.propagateInboundMessage(msg);
    }
    
    private String extractDeviceId(InboundMessage msg) {
        // Extract device ID from D-Bus message
        return "device-123"; // Placeholder
    }
    
    private InboundMessage enrichWithDeviceStatus(InboundMessage msg, DeviceStatus status) {
        // Enrich message with device status
        return msg; // Placeholder
    }
    
    // DTO for API response
    public static class DeviceStatus {
        private boolean online;
        private String lastSeen;
        
        public boolean isOnline() { return online; }
        public String getLastSeen() { return lastSeen; }
        // ... other getters/setters
    }
}
```

### File I/O Operations

File operations are safe and can be used for logging, configuration, or data processing:

```java
public class FileProcessingHandler extends AbstractInboundHandler {
    private final Path configDir;
    private final Path logFile;
    
    public FileProcessingHandler(Path configDir, Path logFile) {
        this.configDir = configDir;
        this.logFile = logFile;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // ✅ SAFE - File I/O operations are fine here
            
            // 1. Read configuration file
            Path configPath = configDir.resolve("message-config.json");
            if (Files.exists(configPath)) {
                String configJson = Files.readString(configPath);
                MessageConfig config = parseConfig(configJson);
                
                // Apply configuration to message processing
                if (!config.shouldProcessMessage(msg)) {
                    LOGGER.debug("Message filtered by configuration");
                    return; // Don't propagate filtered messages
                }
            }
            
            // 2. Write audit log
            String auditEntry = createAuditEntry(msg);
            Files.write(logFile, auditEntry.getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 3. Process message based on file data
            Path dataFile = configDir.resolve("processing-data.txt");
            if (Files.exists(dataFile)) {
                List<String> processingRules = Files.readAllLines(dataFile);
                InboundMessage processedMsg = applyProcessingRules(msg, processingRules);
                ctx.propagateInboundMessage(processedMsg);
            } else {
                // No processing rules - pass message through
                ctx.propagateInboundMessage(msg);
            }
            
        } catch (IOException e) {
            LOGGER.error("File operation failed", e);
            // Decide whether to fail the message or continue
            // For non-critical operations, you might want to continue:
            ctx.propagateInboundMessage(msg);
        }
    }
    
    private String createAuditEntry(InboundMessage msg) {
        return String.format("[%s] %s - %s%n", 
                           Instant.now().toString(), 
                           msg.getType(), 
                           msg.getSerial());
    }
    
    private MessageConfig parseConfig(String configJson) {
        // Parse JSON configuration
        return new MessageConfig(); // Placeholder
    }
    
    private InboundMessage applyProcessingRules(InboundMessage msg, List<String> rules) {
        // Apply file-based processing rules
        return msg; // Placeholder
    }
    
    // Configuration class
    public static class MessageConfig {
        public boolean shouldProcessMessage(InboundMessage msg) {
            // Implement filtering logic
            return true; // Placeholder
        }
    }
}
```

## Advanced Blocking Patterns

### Async-to-Blocking Bridge

Sometimes you need to integrate with async libraries but want to handle the result synchronously:

```java
public class AsyncServiceHandler extends AbstractInboundHandler {
    private final AsyncExternalService asyncService;
    
    public AsyncServiceHandler(AsyncExternalService asyncService) {
        this.asyncService = asyncService;
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // Start async operation
            CompletableFuture<ProcessingResult> future = asyncService.processAsync(
                extractDataFromMessage(msg));
            
            // ✅ SAFE - Block and wait for result
            ProcessingResult result = future.get(30, TimeUnit.SECONDS);
            
            // Handle result synchronously
            if (result.isSuccess()) {
                InboundMessage enrichedMsg = enrichWithResult(msg, result);
                ctx.propagateInboundMessage(enrichedMsg);
            } else {
                ctx.propagateInboundFailure(new ProcessingException(result.getError()));
            }
            
        } catch (TimeoutException e) {
            LOGGER.error("Async operation timed out", e);
            ctx.propagateInboundFailure(new DBusTimeoutException("Service timeout", e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ctx.propagateInboundFailure(new ProcessingException("Operation interrupted", e));
        } catch (ExecutionException e) {
            LOGGER.error("Async operation failed", e);
            ctx.propagateInboundFailure(e.getCause());
        }
    }
    
    private Object extractDataFromMessage(InboundMessage msg) {
        // Extract data for async processing
        return new Object(); // Placeholder
    }
    
    private InboundMessage enrichWithResult(InboundMessage msg, ProcessingResult result) {
        // Enrich message with processing result
        return msg; // Placeholder
    }
}
```

### Custom Thread Pool for Heavy Operations

For extremely heavy operations, you can use a custom thread pool:

```java
public class HeavyProcessingHandler extends AbstractInboundHandler {
    private final ExecutorService heavyWorkExecutor;
    
    public HeavyProcessingHandler() {
        // Custom thread pool for heavy operations
        this.heavyWorkExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "heavy-work-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // Delegate heavy work to specialized thread pool
            CompletableFuture<String> heavyWork = CompletableFuture.supplyAsync(() -> {
                // Perform CPU-intensive or very slow blocking operation
                return performHeavyComputation(msg);
            }, heavyWorkExecutor);
            
            // ✅ SAFE - Block and wait for result
            String result = heavyWork.get(60, TimeUnit.SECONDS);
            
            // Process result
            InboundMessage processedMsg = processHeavyResult(msg, result);
            ctx.propagateInboundMessage(processedMsg);
            
        } catch (TimeoutException e) {
            LOGGER.error("Heavy computation timed out", e);
            ctx.propagateInboundFailure(new ProcessingException("Computation timeout", e));
        } catch (Exception e) {
            LOGGER.error("Heavy computation failed", e);
            ctx.propagateInboundFailure(e);
        }
    }
    
    @Override
    public void handlerRemoved(Context ctx) {
        // Proper cleanup of custom thread pool
        heavyWorkExecutor.shutdown();
        try {
            if (!heavyWorkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heavyWorkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heavyWorkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        super.handlerRemoved(ctx);
    }
    
    private String performHeavyComputation(InboundMessage msg) {
        // Simulate heavy computation
        try {
            Thread.sleep(10000); // 10 seconds of heavy work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Computation interrupted", e);
        }
        return "heavy-result";
    }
    
    private InboundMessage processHeavyResult(InboundMessage msg, String result) {
        // Process the heavy computation result
        return msg; // Placeholder
    }
}
```

## Spring Boot Integration

The D-Bus client library integrates perfectly with Spring Boot for blocking operations:

### Service Layer Integration

```java
@Service
public class BusinessLogicService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ExternalApiClient externalApiClient;
    
    @Transactional
    public ProcessingResult processDBusMessage(InboundMessage msg) {
        try {
            // ✅ All Spring operations are safe - they run on ApplicationTaskExecutor
            
            // 1. Database operations through Spring Data JPA
            String username = extractUsername(msg);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
            
            // 2. External API calls
            ValidationResult validation = externalApiClient.validateUser(user);
            
            // 3. Business logic with database updates
            if (validation.isValid()) {
                user.setLastActivity(Instant.now());
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                
                // 4. Send notifications (potentially blocking)
                notificationService.sendUserActivationNotification(user);
                
                return ProcessingResult.success(user);
            } else {
                return ProcessingResult.failure("User validation failed: " + validation.getError());
            }
            
        } catch (Exception e) {
            LOGGER.error("Business logic processing failed", e);
            return ProcessingResult.failure("Processing error: " + e.getMessage());
        }
    }
    
    private String extractUsername(InboundMessage msg) {
        // Extract username from D-Bus message
        return "user123"; // Placeholder
    }
}

@Component
public class SpringDBusHandler extends AbstractInboundHandler {
    
    @Autowired
    private BusinessLogicService businessLogicService;
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // ✅ SAFE - Spring service methods can perform blocking operations
            ProcessingResult result = businessLogicService.processDBusMessage(msg);
            
            if (result.isSuccess()) {
                // Continue message processing with enriched data
                InboundMessage enrichedMsg = enrichWithProcessingResult(msg, result);
                ctx.propagateInboundMessage(enrichedMsg);
            } else {
                // Handle business logic failure
                ctx.propagateInboundFailure(new BusinessLogicException(result.getError()));
            }
            
        } catch (Exception e) {
            LOGGER.error("Spring service call failed", e);
            ctx.propagateInboundFailure(e);
        }
    }
    
    private InboundMessage enrichWithProcessingResult(InboundMessage msg, ProcessingResult result) {
        // Enrich message with processing result
        return msg; // Placeholder
    }
}

// Supporting classes
public class ProcessingResult {
    private final boolean success;
    private final String error;
    private final Object data;
    
    private ProcessingResult(boolean success, String error, Object data) {
        this.success = success;
        this.error = error;
        this.data = data;
    }
    
    public static ProcessingResult success(Object data) {
        return new ProcessingResult(true, null, data);
    }
    
    public static ProcessingResult failure(String error) {
        return new ProcessingResult(false, error, null);
    }
    
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public Object getData() { return data; }
}
```

### Configuration for Blocking Workloads

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.netty.NettyConnection;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
public class DBusConfiguration {
    
    @Bean
    public ConnectionConfig dbusConnectionConfig() {
        return ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(60))  // Longer timeouts for blocking operations
            .withHealthCheckEnabled(true)
            .withHealthCheckInterval(Duration.ofSeconds(30))
            .withAutoReconnectEnabled(true)
            .withReconnectInitialDelay(Duration.ofSeconds(1))
            .withMaxReconnectAttempts(10)
            .build();
    }
    
    @Bean
    public Connection dbusConnection(ConnectionConfig config) {
        try {
            Connection connection = NettyConnection.newSessionBusConnection(config);
            
            // Add your blocking handlers
            connection.getPipeline().addLast("database", new DatabaseHandler(dataSource()));
            connection.getPipeline().addLast("api", new ExternalApiHandler(restTemplate(), apiBaseUrl()));
            connection.getPipeline().addLast("business", new SpringDBusHandler());
            
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create D-Bus connection", e);
        }
    }
    
    @Bean
    public DataSource dataSource() {
        // Configure your database connection
        return new HikariDataSource(); // Placeholder
    }
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        
        // Configure timeouts for blocking operations
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 seconds
        factory.setReadTimeout(30000);     // 30 seconds
        template.setRequestFactory(factory);
        
        return template;
    }
    
    @Value("${external.api.base-url}")
    private String apiBaseUrl;
    
    private String apiBaseUrl() {
        return apiBaseUrl;
    }
}
```

## Best Practices for Blocking Operations

### 1. Implement Timeouts and Circuit Breakers

```java
public class ResilientHandler extends AbstractInboundHandler {
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    
    public ResilientHandler() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("externalService");
        this.timeLimiter = TimeLimiter.of(Duration.ofSeconds(30));
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // Combine circuit breaker and time limiter
            String result = circuitBreaker.executeSupplier(() -> 
                timeLimiter.executeFutureSupplier(() -> 
                    CompletableFuture.supplyAsync(() -> {
                        // ✅ SAFE - This blocking operation is protected
                        return performBlockingOperation(msg);
                    })
                )
            );
            
            ctx.propagateInboundMessage(enrichMessage(msg, result));
            
        } catch (CircuitBreakerOpenException e) {
            LOGGER.warn("Circuit breaker is open, service unavailable");
            ctx.propagateInboundFailure(new ServiceUnavailableException("External service unavailable", e));
        } catch (TimeoutException e) {
            LOGGER.error("Operation timed out", e);
            ctx.propagateInboundFailure(new DBusTimeoutException("Operation timeout", e));
        } catch (Exception e) {
            LOGGER.error("Resilient operation failed", e);
            ctx.propagateInboundFailure(e);
        }
    }
    
    private String performBlockingOperation(InboundMessage msg) {
        // Your blocking operation here
        return "result";
    }
    
    private InboundMessage enrichMessage(InboundMessage msg, String result) {
        // Enrich message with result
        return msg;
    }
}
```

### 2. Proper Resource Management

```java
public class ResourceManagedHandler extends AbstractInboundHandler {
    private final DataSource dataSource;
    private final ExecutorService customExecutor;
    
    public ResourceManagedHandler(DataSource dataSource) {
        this.dataSource = dataSource;
        this.customExecutor = Executors.newFixedThreadPool(2);
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // ✅ SAFE - Proper resource management with blocking operations
        try (Connection dbConn = dataSource.getConnection();
             PreparedStatement stmt = dbConn.prepareStatement("SELECT * FROM events WHERE id = ?")) {
            
            stmt.setInt(1, extractEventId(msg));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Process database result
                    processEvent(rs, msg);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.error("Database operation failed", e);
            ctx.propagateInboundFailure(e);
            return;
        }
        
        ctx.propagateInboundMessage(msg);
    }
    
    @Override
    public void handlerRemoved(Context ctx) {
        // Clean up custom resources
        if (customExecutor != null && !customExecutor.isShutdown()) {
            customExecutor.shutdown();
            try {
                if (!customExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    customExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                customExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.handlerRemoved(ctx);
    }
    
    private int extractEventId(InboundMessage msg) {
        // Extract event ID from message
        return 123; // Placeholder
    }
    
    private void processEvent(ResultSet rs, InboundMessage msg) throws SQLException {
        // Process database result
        String eventName = rs.getString("event_name");
        LOGGER.info("Processing event: {}", eventName);
    }
}
```

### 3. Performance Monitoring

```java
public class MonitoredBlockingHandler extends AbstractInboundHandler {
    private final Timer blockingOperationTimer;
    private final Counter operationCounter;
    
    public MonitoredBlockingHandler(MeterRegistry meterRegistry) {
        this.blockingOperationTimer = Timer.builder("dbus.blocking.operation")
            .description("Time spent in blocking operations")
            .register(meterRegistry);
        this.operationCounter = Counter.builder("dbus.blocking.operations.total")
            .description("Total number of blocking operations")
            .register(meterRegistry);
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        Timer.Sample sample = Timer.start();
        
        try {
            // ✅ SAFE - Monitor your blocking operations
            operationCounter.increment();
            
            String result = performBlockingOperation(msg);
            
            // Process result
            ctx.propagateInboundMessage(enrichMessage(msg, result));
            
        } catch (Exception e) {
            LOGGER.error("Monitored blocking operation failed", e);
            ctx.propagateInboundFailure(e);
        } finally {
            sample.stop(blockingOperationTimer);
        }
    }
    
    private String performBlockingOperation(InboundMessage msg) {
        // Your blocking operation with monitoring
        try {
            // Simulate database call
            Thread.sleep(100);
            return "operation-result";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted", e);
        }
    }
    
    private InboundMessage enrichMessage(InboundMessage msg, String result) {
        // Enrich message with result
        return msg;
    }
}
```

## What NOT to Do

### ❌ Don't Overcomplicate with Reactive Streams

```java
// DON'T DO THIS - Unnecessarily complex for blocking operations
public class OvercomplicatedHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // This is overly complex - you're already on a worker thread!
        Mono.fromCallable(() -> {
                // You're already on ApplicationTaskExecutor - just call directly!
                return blockingDatabaseCall(msg);
            })
            .subscribeOn(Schedulers.boundedElastic())  // Unnecessary thread switching
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> LOGGER.error("Error", e))
            .block();  // Why make it reactive if you're going to block anyway?
        
        ctx.propagateInboundMessage(msg);
    }
}
```

### ✅ Do This Instead

```java
// DO THIS - Simple and direct
public class SimpleBlockingHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        try {
            // Simple and direct - you're already on the right thread
            String result = blockingDatabaseCall(msg);
            ctx.propagateInboundMessage(enrichMessage(msg, result));
        } catch (Exception e) {
            LOGGER.error("Database call failed", e);
            ctx.propagateInboundFailure(e);
        }
    }
}
```

### ❌ Don't Create Unnecessary Thread Pools

```java
// DON'T DO THIS - Creating unnecessary threads
public class UnnecessaryThreadPoolHandler extends AbstractInboundHandler {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // You're already on a worker thread - just use it!
        executor.submit(() -> {
            String result = blockingOperation(msg);
            // Now you have to deal with async completion...
            ctx.propagateInboundMessage(enrichMessage(msg, result));
        });
    }
}
```

### ✅ Do This Instead

```java
// DO THIS - Use the thread you're already on
public class EfficientBlockingHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // Use the ApplicationTaskExecutor thread you're already on
        String result = blockingOperation(msg);
        ctx.propagateInboundMessage(enrichMessage(msg, result));
    }
}
```

## Configuration for Blocking Workloads

### Custom Application Executor

If you have many blocking operations, you might want to configure a larger thread pool:

```java
// Create connection with custom configuration for blocking workloads
ConnectionConfig config = ConnectionConfig.builder()
    .withMethodCallTimeout(Duration.ofSeconds(60))  // Longer timeouts for blocking operations
    .withHealthCheckEnabled(true)
    .withHealthCheckInterval(Duration.ofSeconds(30))
    .build();

Connection connection = NettyConnection.newSessionBusConnection(config);

// The ApplicationTaskExecutor is sized appropriately by default,
// but you can customize it if needed through the NettyConnectionContext
```

### Spring Boot Configuration

```java
@Configuration
public class BlockingDBusConfiguration {
    
    @Bean
    public ConnectionConfig dbusConnectionConfig() {
        return ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(60))
            .withHealthCheckEnabled(true)
            .withAutoReconnectEnabled(true)
            .build();
    }
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        config.setUsername("user");
        config.setPassword("password");
        
        // Configure connection pool for blocking operations
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        
        // Configure for blocking operations
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        template.setRequestFactory(factory);
        
        return template;
    }
}
```

## Debugging and Troubleshooting

### Thread Analysis

To verify your blocking operations are running on the correct threads:

```java
public class ThreadAnalysisHandler extends AbstractInboundHandler {
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        String threadName = Thread.currentThread().getName();
        
        // Should show "dbus-app-worker-*" - indicating ApplicationTaskExecutor
        LOGGER.info("Processing message on thread: {}", threadName);
        
        // Verify you're NOT on Netty event loop
        if (threadName.contains("nio-") || threadName.contains("epoll-")) {
            LOGGER.error("WARNING: Handler is running on Netty event loop thread!");
        }
        
        // Your blocking operation here
        String result = blockingOperation(msg);
        
        ctx.propagateInboundMessage(enrichMessage(msg, result));
    }
}
```

### Performance Monitoring

```java
public class PerformanceMonitoringHandler extends AbstractInboundHandler {
    private final AtomicLong totalOperations = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Your blocking operation
            String result = blockingOperation(msg);
            ctx.propagateInboundMessage(enrichMessage(msg, result));
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            long ops = totalOperations.incrementAndGet();
            long time = totalTime.addAndGet(duration);
            
            if (ops % 100 == 0) {
                LOGGER.info("Processed {} operations, average time: {}ms", 
                           ops, time / ops);
            }
        }
    }
}
```

## Summary

The D-Bus client library's dual-pipeline architecture makes blocking operations **safe and straightforward**:

### ✅ What You Can Do Safely

1. **Database Operations** - JDBC calls, JPA/Hibernate operations, connection pooling
2. **REST API Calls** - HTTP clients, web service calls, external API integration
3. **File I/O** - Reading/writing files, configuration loading, logging
4. **Heavy Computation** - CPU-intensive processing, complex algorithms
5. **Blocking Libraries** - Any traditional blocking Java library
6. **Spring Integration** - All Spring features work normally

### ✅ Key Benefits

1. **Thread Safety** - Your handlers run on `ApplicationTaskExecutor`, not Netty event loop
2. **Performance** - Netty event loop remains unblocked for network operations
3. **Simplicity** - Write straightforward blocking code without complex async patterns
4. **Reliability** - Proper resource management and shutdown handling
5. **Monitoring** - Easy to add metrics and monitoring to blocking operations

### ✅ Best Practices

1. **Use timeouts** - Always set reasonable timeouts for blocking operations
2. **Handle exceptions** - Properly catch and handle blocking operation failures
3. **Monitor performance** - Track timing and success rates of blocking operations
4. **Manage resources** - Use try-with-resources and proper cleanup
5. **Consider circuit breakers** - For external dependencies that might fail

The key insight is that the D-Bus library's **sophisticated architecture already handles the complexity** of thread management for you. You can focus on your business logic and write clean, readable blocking code without worrying about performance or threading issues.