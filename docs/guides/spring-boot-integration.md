# Spring Boot Integration Guide

This guide demonstrates how to integrate the D-Bus Client Java library with Spring Boot applications. We'll cover auto-configuration, dependency injection, and common patterns for building D-Bus-enabled Spring Boot applications.

## Quick Setup

### Dependencies

Add the D-Bus client dependency to your `build.gradle` or `pom.xml`:

```gradle
dependencies {
    implementation 'com.lucimber:lucimber-dbus-client:2.0.0'
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator' // For health checks
}
```

### Basic Auto-Configuration

Create a Spring Boot auto-configuration class:

```java
@Configuration
@EnableConfigurationProperties(DBusProperties.class)
@ConditionalOnClass(Connection.class)
public class DBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectionConfig dbusConnectionConfig(DBusProperties properties) {
        return ConnectionConfig.builder()
            .withAutoReconnectEnabled(properties.isAutoReconnect())
            .withReconnectInitialDelay(properties.getReconnectInitialDelay())
            .withMaxReconnectAttempts(properties.getMaxReconnectAttempts())
            .withHealthCheckEnabled(properties.isHealthCheckEnabled())
            .withHealthCheckInterval(properties.getHealthCheckInterval())
            .withConnectTimeout(properties.getConnectTimeout())
            .withMethodCallTimeout(properties.getMethodCallTimeout())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Connection dbusConnection(ConnectionConfig config) {
        return new LazyDBusConnection(config);
    }

    @Bean
    @ConditionalOnProperty(value = "dbus.metrics.enabled", matchIfMissing = true)
    public DBusMetricsHandler dbusMetricsHandler(MeterRegistry meterRegistry) {
        return new DBusMetricsHandler(meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(value = "dbus.health.enabled", matchIfMissing = true)
    public DBusHealthIndicator dbusHealthIndicator(Connection connection) {
        return new DBusHealthIndicator(connection);
    }
}
```

## Configuration Properties

### Properties Class

```java
@ConfigurationProperties(prefix = "dbus")
@Data
@Validated
public class DBusProperties {
    
    /**
     * D-Bus connection type (SYSTEM or SESSION)
     */
    private BusType busType = BusType.SYSTEM;
    
    /**
     * Enable automatic reconnection on connection loss
     */
    private boolean autoReconnect = true;
    
    /**
     * Initial delay before first reconnection attempt
     */
    @DurationMin(Duration.ofMillis(100))
    private Duration reconnectInitialDelay = Duration.ofSeconds(1);
    
    /**
     * Maximum number of reconnection attempts
     */
    @Min(1)
    private int maxReconnectAttempts = 10;
    
    /**
     * Enable periodic health checks
     */
    private boolean healthCheckEnabled = true;
    
    /**
     * Interval between health checks
     */
    @DurationMin(Duration.ofSeconds(5))
    private Duration healthCheckInterval = Duration.ofSeconds(30);
    
    /**
     * Connection establishment timeout
     */
    @DurationMin(Duration.ofSeconds(1))
    private Duration connectTimeout = Duration.ofSeconds(10);
    
    /**
     * Default timeout for method calls
     */
    @DurationMin(Duration.ofSeconds(1))
    private Duration methodCallTimeout = Duration.ofSeconds(30);
    
    /**
     * Metrics configuration
     */
    private Metrics metrics = new Metrics();
    
    /**
     * Health check configuration
     */
    private Health health = new Health();
    
    @Data
    public static class Metrics {
        /**
         * Enable D-Bus metrics collection
         */
        private boolean enabled = true;
        
        /**
         * Metrics prefix
         */
        private String prefix = "dbus";
    }
    
    @Data
    public static class Health {
        /**
         * Enable D-Bus health indicator
         */
        private boolean enabled = true;
        
        /**
         * Health check timeout
         */
        private Duration timeout = Duration.ofSeconds(5);
    }
    
    public enum BusType {
        SYSTEM, SESSION
    }
}
```

### Application Properties

```yaml
dbus:
  bus-type: SYSTEM
  auto-reconnect: true
  reconnect-initial-delay: PT1S
  max-reconnect-attempts: 10
  health-check-enabled: true
  health-check-interval: PT30S
  connect-timeout: PT10S
  method-call-timeout: PT30S
  metrics:
    enabled: true
    prefix: "dbus"
  health:
    enabled: true
    timeout: PT5S

# Spring Boot Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

## Lazy Connection Management

Create a lazy connection wrapper that connects only when needed:

```java
public class LazyDBusConnection implements Connection {
    private final ConnectionConfig config;
    private final DBusProperties.BusType busType;
    private volatile Connection delegate;
    private final Object lock = new Object();

    public LazyDBusConnection(ConnectionConfig config, DBusProperties.BusType busType) {
        this.config = config;
        this.busType = busType;
    }

    private Connection getConnection() {
        if (delegate == null) {
            synchronized (lock) {
                if (delegate == null) {
                    try {
                        Connection conn = busType == DBusProperties.BusType.SYSTEM 
                            ? NettyConnection.newSystemBusConnection(config)
                            : NettyConnection.newSessionBusConnection(config);
                        
                        conn.connect().toCompletableFuture().get(
                            config.getConnectTimeout().toMillis(), 
                            TimeUnit.MILLISECONDS
                        );
                        
                        delegate = conn;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to establish D-Bus connection", e);
                    }
                }
            }
        }
        return delegate;
    }

    @Override
    public CompletionStage<Void> connect() {
        return getConnection().connect();
    }

    @Override
    public CompletionStage<InboundMessage> sendRequest(OutboundMessage message) {
        return getConnection().sendRequest(message);
    }

    @Override
    public boolean isConnected() {
        return delegate != null && delegate.isConnected();
    }

    @Override
    public Pipeline getPipeline() {
        return getConnection().getPipeline();
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }

    @PreDestroy
    public void destroy() {
        close();
    }
}
```

## Service Layer Integration

### D-Bus Service Template

```java
@Service
@Slf4j
public abstract class AbstractDBusService {
    
    protected final Connection dbusConnection;
    private final DBusProperties properties;

    protected AbstractDBusService(Connection dbusConnection, DBusProperties properties) {
        this.dbusConnection = dbusConnection;
        this.properties = properties;
    }

    @PostConstruct
    protected void initialize() {
        setupHandlers();
        subscribeToSignals();
    }

    protected void setupHandlers() {
        // Override in subclasses to add custom handlers
    }

    protected void subscribeToSignals() {
        // Override in subclasses to subscribe to specific signals
    }

    protected <T> CompletableFuture<T> executeWithTimeout(
            Supplier<CompletableFuture<T>> operation,
            Duration timeout) {
        
        CompletableFuture<T> future = operation.get();
        
        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    log.warn("D-Bus operation timed out after {}", timeout);
                } else {
                    log.error("D-Bus operation failed", throwable);
                }
                throw new DBusException("Operation failed", throwable);
            });
    }

    protected OutboundMethodCall.Builder createMethodCallBuilder() {
        return OutboundMethodCall.Builder.create()
            .withReplyExpected(true);
    }
}
```

### System Information Service Example

```java
@Service
@Slf4j
public class SystemInfoService extends AbstractDBusService {

    public SystemInfoService(Connection dbusConnection, DBusProperties properties) {
        super(dbusConnection, properties);
    }

    public CompletableFuture<String> getHostname() {
        OutboundMethodCall call = createMethodCallBuilder()
            .withPath(DBusObjectPath.valueOf("/org/freedesktop/hostname1"))
            .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
            .withMember(DBusString.valueOf("Get"))
            .withDestination(DBusString.valueOf("org.freedesktop.hostname1"))
            .withSignature(DBusSignature.valueOf("ss"))
            .withBody(List.of(
                DBusString.valueOf("org.freedesktop.hostname1"),
                DBusString.valueOf("Hostname")
            ))
            .build();

        return executeWithTimeout(
            () -> dbusConnection.sendRequest(call).thenApply(this::extractHostname),
            properties.getMethodCallTimeout()
        );
    }

    private String extractHostname(InboundMessage response) {
        if (response instanceof InboundMethodReturn) {
            // Parse the variant response and extract hostname
            // Implementation depends on response structure
            return "hostname"; // Simplified
        }
        throw new DBusException("Failed to get hostname");
    }

    @EventListener
    public void handleHostnameChanged(HostnameChangedEvent event) {
        log.info("Hostname changed to: {}", event.getNewHostname());
        // Handle hostname change
    }
}
```

## Health Check Integration

### D-Bus Health Indicator

```java
@Component
@ConditionalOnProperty(value = "dbus.health.enabled", matchIfMissing = true)
public class DBusHealthIndicator implements HealthIndicator {
    
    private final Connection dbusConnection;
    private final DBusProperties properties;

    public DBusHealthIndicator(Connection dbusConnection, DBusProperties properties) {
        this.dbusConnection = dbusConnection;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            if (!dbusConnection.isConnected()) {
                return Health.down()
                    .withDetail("status", "disconnected")
                    .withDetail("message", "D-Bus connection is not active")
                    .build();
            }

            // Perform a quick ping to verify connectivity
            OutboundMethodCall ping = OutboundMethodCall.Builder.create()
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withMember(DBusString.valueOf("Ping"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();

            CompletableFuture<InboundMessage> response = dbusConnection.sendRequest(ping);
            InboundMessage reply = response.get(
                properties.getHealth().getTimeout().toMillis(), 
                TimeUnit.MILLISECONDS
            );

            if (reply instanceof InboundMethodReturn) {
                return Health.up()
                    .withDetail("status", "connected")
                    .withDetail("transport", dbusConnection.getClass().getSimpleName())
                    .withDetail("bus-type", properties.getBusType())
                    .withDetail("ping-response-time", "< " + properties.getHealth().getTimeout())
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "ping-failed")
                    .withDetail("response-type", reply.getClass().getSimpleName())
                    .build();
            }

        } catch (TimeoutException e) {
            return Health.down()
                .withDetail("status", "timeout")
                .withDetail("message", "Health check timed out")
                .withDetail("timeout", properties.getHealth().getTimeout())
                .build();
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("status", "error")
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
```

## Metrics Integration

### D-Bus Metrics Handler

```java
@Component
@ConditionalOnProperty(value = "dbus.metrics.enabled", matchIfMissing = true)
public class DBusMetricsHandler extends AbstractDuplexHandler {
    
    private final Counter inboundMessages;
    private final Counter outboundMessages;
    private final Timer methodCallTimer;
    private final Counter connectionEvents;
    private final Gauge connectionStatus;

    public DBusMetricsHandler(MeterRegistry meterRegistry, DBusProperties properties) {
        String prefix = properties.getMetrics().getPrefix();
        
        this.inboundMessages = Counter.builder(prefix + ".messages.inbound")
            .description("Number of inbound D-Bus messages")
            .register(meterRegistry);
            
        this.outboundMessages = Counter.builder(prefix + ".messages.outbound")
            .description("Number of outbound D-Bus messages")
            .register(meterRegistry);
            
        this.methodCallTimer = Timer.builder(prefix + ".method.call.duration")
            .description("D-Bus method call duration")
            .register(meterRegistry);
            
        this.connectionEvents = Counter.builder(prefix + ".connection.events")
            .description("D-Bus connection events")
            .register(meterRegistry);
            
        this.connectionStatus = Gauge.builder(prefix + ".connection.status")
            .description("D-Bus connection status (1=connected, 0=disconnected)")
            .register(meterRegistry, this, handler -> handler.getConnectionStatus());
    }

    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        inboundMessages.increment(
            Tags.of(
                "type", msg.getType().toString(),
                "has_sender", String.valueOf(msg.getSender().isPresent())
            )
        );
        ctx.propagateInboundMessage(msg);
    }

    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg, 
                                    CompletableFuture<Void> writeFuture) {
        outboundMessages.increment(
            Tags.of(
                "type", msg.getType().toString(),
                "has_destination", String.valueOf(msg.getDestination().isPresent())
            )
        );

        if (msg instanceof OutboundMethodCall) {
            Timer.Sample sample = Timer.start();
            writeFuture.whenComplete((result, throwable) -> {
                Tags tags = Tags.of(
                    "success", String.valueOf(throwable == null)
                );
                sample.stop(Timer.builder("dbus.method.call.duration")
                    .tags(tags)
                    .register(Metrics.globalRegistry));
            });
        }

        ctx.propagateOutboundMessage(msg, writeFuture);
    }

    @Override
    public void channelActive(Context ctx) {
        connectionEvents.increment(Tags.of("event", "connected"));
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(Context ctx) {
        connectionEvents.increment(Tags.of("event", "disconnected"));
        super.channelInactive(ctx);
    }

    private double getConnectionStatus() {
        // This would need access to the connection - simplified for example
        return 1.0; // Connected
    }
}
```

## Event-Driven Architecture

### D-Bus Signal to Spring Events

```java
@Component
public class DBusSignalEventPublisher extends AbstractInboundHandler {
    
    private final ApplicationEventPublisher eventPublisher;

    public DBusSignalEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (msg instanceof InboundSignal) {
            InboundSignal signal = (InboundSignal) msg;
            
            // Convert D-Bus signals to Spring application events
            String interfaceName = signal.getInterfaceName()
                .map(DBusString::toString)
                .orElse("");
            String member = signal.getMember().toString();
            
            switch (interfaceName) {
                case "org.freedesktop.hostname1":
                    if ("PropertiesChanged".equals(member)) {
                        eventPublisher.publishEvent(new HostnameChangedEvent(signal));
                    }
                    break;
                case "org.freedesktop.NetworkManager":
                    eventPublisher.publishEvent(new NetworkManagerEvent(signal));
                    break;
                default:
                    eventPublisher.publishEvent(new GenericDBusSignalEvent(signal));
            }
        }
        
        ctx.propagateInboundMessage(msg);
    }
}

// Custom event classes
public class HostnameChangedEvent extends ApplicationEvent {
    private final InboundSignal signal;
    
    public HostnameChangedEvent(InboundSignal signal) {
        super(signal);
        this.signal = signal;
    }
    
    public String getNewHostname() {
        // Extract hostname from signal body
        return "new-hostname"; // Simplified
    }
}
```

### Event Listeners

```java
@Component
@Slf4j
public class SystemEventHandler {

    @EventListener
    @Async
    public void handleHostnameChange(HostnameChangedEvent event) {
        log.info("Hostname changed to: {}", event.getNewHostname());
        // Update configuration, notify other services, etc.
    }

    @EventListener
    @Async
    public void handleNetworkChange(NetworkManagerEvent event) {
        log.info("Network configuration changed");
        // Handle network changes
    }

    @EventListener
    public void handleGenericDBusSignal(GenericDBusSignalEvent event) {
        log.debug("Received D-Bus signal: {}", event.getSignal().getMember());
    }
}
```

## Configuration Profiles

### Development Profile

```yaml
# application-dev.yml
dbus:
  bus-type: SESSION  # Use session bus for development
  connect-timeout: PT30S  # Longer timeout for debugging
  health:
    timeout: PT10S
  metrics:
    enabled: true

logging:
  level:
    com.lucimber.dbus: DEBUG
    org.springframework.boot.actuate: DEBUG
```

### Production Profile

```yaml
# application-prod.yml
dbus:
  bus-type: SYSTEM
  auto-reconnect: true
  max-reconnect-attempts: 20
  health-check-interval: PT60S
  metrics:
    enabled: true
    prefix: "myapp.dbus"

management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## Testing Configuration

### Test Configuration

```java
@TestConfiguration
public class DBusTestConfiguration {

    @Bean
    @Primary
    public Connection mockDBusConnection() {
        return Mockito.mock(Connection.class);
    }

    @Bean
    @Primary
    public DBusProperties testDBusProperties() {
        DBusProperties properties = new DBusProperties();
        properties.setBusType(DBusProperties.BusType.SESSION);
        properties.setConnectTimeout(Duration.ofSeconds(5));
        return properties;
    }
}
```

### Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DBusIntegrationTest {

    @Container
    static GenericContainer<?> dbusContainer = new GenericContainer<>("dbus-test-image")
        .withExposedPorts(12345);

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public Connection testDBusConnection() throws Exception {
            String address = String.format("tcp:host=localhost,port=%d",
                dbusContainer.getMappedPort(12345));
            
            // Create real connection to test container
            return NettyConnection.newSystemBusConnection();
        }
    }

    @Autowired
    private SystemInfoService systemInfoService;

    @Test
    void testGetHostname() throws Exception {
        String hostname = systemInfoService.getHostname().get();
        assertThat(hostname).isNotNull();
    }
}
```

This Spring Boot integration provides a complete foundation for building D-Bus-enabled applications with proper configuration management, health checks, metrics, and testing support.