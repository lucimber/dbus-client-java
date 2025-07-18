# Integration Patterns

This guide covers common patterns for integrating the D-Bus client library into different types of applications and frameworks. Each pattern includes practical examples and best practices.

## Enterprise Application Integration

### Spring Boot Integration

#### Basic Configuration

```java
@Configuration
@EnableConfigurationProperties(DBusProperties.class)
public class DBusConfiguration {
    
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
        try {
            Connection connection = NettyConnection.newSystemBusConnection(config);
            connection.connect().toCompletableFuture().get();
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create D-Bus connection", e);
        }
    }
    
    @PreDestroy
    public void closeConnection(@Autowired Connection connection) {
        if (connection != null) {
            connection.close();
        }
    }
}
```

#### Configuration Properties

```java
@ConfigurationProperties(prefix = "dbus")
@Data
public class DBusProperties {
    private boolean autoReconnect = true;
    private Duration reconnectInitialDelay = Duration.ofSeconds(1);
    private int maxReconnectAttempts = 10;
    private boolean healthCheckEnabled = true;
    private Duration healthCheckInterval = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration methodCallTimeout = Duration.ofSeconds(30);
}
```

#### Application Properties

```yaml
dbus:
  auto-reconnect: true
  reconnect-initial-delay: PT1S
  max-reconnect-attempts: 10
  health-check-enabled: true
  health-check-interval: PT30S
  connect-timeout: PT10S
  method-call-timeout: PT30S
```

#### Service Layer Integration

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SystemMonitoringService {
    private final Connection dbusConnection;
    private final AtomicLong serialCounter = new AtomicLong(1);
    
    public SystemMonitoringService(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
        setupSignalHandlers();
    }
    
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        // Initialize D-Bus subscriptions after Spring context is ready
        subscribeToSystemSignals();
    }
    
    private void setupSignalHandlers() {
        dbusConnection.getPipeline().addLast("system-monitor", new SystemSignalHandler());
    }
    
    public CompletableFuture<SystemInfo> getSystemInfo() {
        OutboundMethodCall call = OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
            .withPath(DBusObjectPath.valueOf("/org/freedesktop/hostname1"))
            .withMember(DBusString.valueOf("Get"))
            .withDestination(DBusString.valueOf("org.freedesktop.hostname1"))
            .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
            .withReplyExpected(true)
            .build();
        
        return dbusConnection.sendRequest(call)
            .thenApply(this::parseSystemInfo);
    }
    
    private SystemInfo parseSystemInfo(InboundMessage response) {
        // Parse D-Bus response into domain object
        return new SystemInfo(/* ... */);
    }
    
    private void subscribeToSystemSignals() {
        // Implementation for signal subscription
    }
    
    // Custom signal handler
    private static class SystemSignalHandler extends AbstractInboundHandler {
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            // Handle system signals
            ctx.propagateInboundMessage(msg);
        }
    }
    
    // DTO class for system information
    public static class SystemInfo {
        // System information fields
    }
}
```

### Microservices Pattern

#### Health Check Integration

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DBusHealthIndicator implements HealthIndicator {
    private final Connection dbusConnection;
    private final AtomicLong serialCounter = new AtomicLong(1);
    
    public DBusHealthIndicator(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
    }
    
    @Override
    public Health health() {
        try {
            if (dbusConnection.isConnected()) {
                // Perform a quick ping to verify connectivity
                OutboundMethodCall ping = createPingCall();
                CompletableFuture<InboundMessage> response = dbusConnection.sendRequest(ping);
                response.get(5, TimeUnit.SECONDS);
                
                return Health.up()
                    .withDetail("connection", "active")
                    .withDetail("transport", dbusConnection.getClass().getSimpleName())
                    .build();
            } else {
                return Health.down()
                    .withDetail("connection", "inactive")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("connection", "error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private OutboundMethodCall createPingCall() {
        return OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
            .withMember(DBusString.valueOf("Ping"))
            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
            .withReplyExpected(true)
            .build();
    }
}
```

#### Metrics Integration

```java
import com.lucimber.dbus.connection.AbstractDuplexHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class DBusMetricsHandler extends AbstractDuplexHandler {
    private final MeterRegistry meterRegistry;
    private final Counter inboundMessages;
    private final Counter outboundMessages;
    private final Timer methodCallTimer;
    
    public DBusMetricsHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.inboundMessages = Counter.builder("dbus.messages.inbound")
            .description("Number of inbound D-Bus messages")
            .register(meterRegistry);
        this.outboundMessages = Counter.builder("dbus.messages.outbound")
            .description("Number of outbound D-Bus messages")
            .register(meterRegistry);
        this.methodCallTimer = Timer.builder("dbus.method.call")
            .description("D-Bus method call duration")
            .register(meterRegistry);
    }
    
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        inboundMessages.increment(
            Tags.of("type", msg.getType().toString())
        );
        ctx.propagateInboundMessage(msg);
    }
    
    @Override
    public void handleOutboundMessage(Context ctx, OutboundMessage msg, 
                                    CompletableFuture<Void> writeFuture) {
        outboundMessages.increment(
            Tags.of("type", msg.getType().toString())
        );
        
        if (msg instanceof OutboundMethodCall) {
            Timer.Sample sample = Timer.start(meterRegistry);
            writeFuture.whenComplete((result, throwable) -> {
                sample.stop(methodCallTimer);
            });
        }
        
        ctx.propagateOutboundMessage(msg, writeFuture);
    }
}
```

## Reactive Programming Patterns

### Project Reactor Integration

```java
import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
public class ReactiveDBusService {
    private final Connection dbusConnection;
    private final Scheduler scheduler;
    
    public ReactiveDBusService(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
        this.scheduler = Schedulers.boundedElastic();
    }
    
    public Mono<InboundMessage> sendMethodCall(OutboundMethodCall call) {
        return Mono.fromFuture(dbusConnection.sendRequest(call))
            .subscribeOn(scheduler)
            .timeout(Duration.ofSeconds(30))
            .onErrorMap(TimeoutException.class, 
                ex -> new RuntimeException("Method call timed out", ex));
    }
    
    public Flux<InboundSignal> signalStream(String interfaceName) {
        return Flux.create(sink -> {
            SignalHandler handler = new SignalHandler(interfaceName, sink);
            dbusConnection.getPipeline().addLast("signal-stream", handler);
            
            sink.onDispose(() -> {
                dbusConnection.getPipeline().remove("signal-stream");
            });
        });
    }
    
    private static class SignalHandler extends AbstractInboundHandler {
        private final String interfaceName;
        private final FluxSink<InboundSignal> sink;
        
        SignalHandler(String interfaceName, FluxSink<InboundSignal> sink) {
            this.interfaceName = interfaceName;
            this.sink = sink;
        }
        
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal) {
                InboundSignal signal = (InboundSignal) msg;
                if (signal.getInterfaceName()
                         .map(DBusString::toString)
                         .orElse("")
                         .equals(interfaceName)) {
                    sink.next(signal);
                }
            }
            ctx.propagateInboundMessage(msg);
        }
        
        @Override
        public void handleExceptionCaught(Context ctx, Throwable cause) {
            sink.error(cause);
        }
    }
}
```

### RxJava Integration

```java
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundMethodCall;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RxDBusService {
    private final Connection dbusConnection;
    
    public Single<InboundMessage> sendMethodCall(OutboundMethodCall call) {
        return Single.fromFuture(dbusConnection.sendRequest(call))
            .subscribeOn(Schedulers.io())
            .timeout(30, TimeUnit.SECONDS);
    }
    
    public Observable<InboundSignal> observeSignals(String interfaceName) {
        return Observable.create(emitter -> {
            SignalHandler handler = new SignalHandler(interfaceName, emitter);
            dbusConnection.getPipeline().addLast("rx-signals", handler);
            
            emitter.setCancellable(() -> {
                dbusConnection.getPipeline().remove("rx-signals");
            });
        });
    }
}
```

## Desktop Application Patterns

### Swing Integration

```java
public class SwingDBusApplication extends JFrame {
    private final Connection dbusConnection;
    private final JTextArea logArea;
    
    public SwingDBusApplication() throws Exception {
        this.dbusConnection = NettyConnection.newSessionBusConnection();
        this.logArea = new JTextArea(20, 50);
        
        setupUI();
        setupDBusHandlers();
        connectToDBus();
    }
    
    private void setupDBusHandlers() {
        dbusConnection.getPipeline().addLast("swing-handler", new SwingSignalHandler());
    }
    
    private void connectToDBus() throws Exception {
        dbusConnection.connect().toCompletableFuture().get();
    }
    
    private class SwingSignalHandler extends AbstractInboundHandler {
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal) {
                // Use SwingUtilities to update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Signal received: " + msg + "\n");
                });
            }
            ctx.propagateInboundMessage(msg);
        }
    }
    
    @Override
    public void dispose() {
        if (dbusConnection != null) {
            dbusConnection.close();
        }
        super.dispose();
    }
}
```

### JavaFX Integration

```java
public class JavaFXDBusApplication extends Application {
    private Connection dbusConnection;
    private TextArea logArea;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.dbusConnection = NettyConnection.newSessionBusConnection();
        this.logArea = new TextArea();
        
        setupUI(primaryStage);
        setupDBusHandlers();
        connectToDBus();
    }
    
    private void setupDBusHandlers() {
        dbusConnection.getPipeline().addLast("javafx-handler", new JavaFXSignalHandler());
    }
    
    private class JavaFXSignalHandler extends AbstractInboundHandler {
        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal) {
                // Use Platform.runLater to update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    logArea.appendText("Signal received: " + msg + "\n");
                });
            }
            ctx.propagateInboundMessage(msg);
        }
    }
    
    @Override
    public void stop() throws Exception {
        if (dbusConnection != null) {
            dbusConnection.close();
        }
        super.stop();
    }
}
```

## Background Service Patterns

### Daemon Service

```java
public class DBusDaemonService {
    private final Connection dbusConnection;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    public DBusDaemonService() throws Exception {
        this.dbusConnection = NettyConnection.newSystemBusConnection();
        this.scheduler = Executors.newScheduledThreadPool(2);
        setupHandlers();
    }
    
    public void start() throws Exception {
        dbusConnection.connect().toCompletableFuture().get();
        running = true;
        
        // Schedule periodic tasks
        scheduler.scheduleAtFixedRate(
            this::performPeriodicTask, 
            0, 60, TimeUnit.SECONDS
        );
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        
        System.out.println("D-Bus daemon service started");
    }
    
    public void stop() {
        running = false;
        scheduler.shutdown();
        if (dbusConnection != null) {
            dbusConnection.close();
        }
        System.out.println("D-Bus daemon service stopped");
    }
    
    private void performPeriodicTask() {
        if (!running) return;
        
        try {
            // Perform regular D-Bus operations
            OutboundMethodCall healthCheck = createHealthCheckCall();
            dbusConnection.sendRequest(healthCheck).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
        }
    }
}
```

### SystemD Service Integration

```java
@Component
public class SystemDNotificationService {
    private final Connection dbusConnection;
    
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        notifySystemD("READY=1");
    }
    
    @EventListener
    public void handleApplicationFailed(ApplicationFailedEvent event) {
        notifySystemD("STATUS=Application failed: " + event.getException().getMessage());
    }
    
    private void notifySystemD(String status) {
        try {
            // Send notification to systemd
            ProcessBuilder pb = new ProcessBuilder("systemd-notify", status);
            pb.start().waitFor();
        } catch (Exception e) {
            // Log error but don't fail the application
            System.err.println("Failed to notify systemd: " + e.getMessage());
        }
    }
}
```

## Error Handling Patterns

### Circuit Breaker Pattern

```java
@Component
public class CircuitBreakerDBusService {
    private final Connection dbusConnection;
    private final CircuitBreaker circuitBreaker;
    
    public CircuitBreakerDBusService(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
        this.circuitBreaker = CircuitBreaker.ofDefaults("dbus-service");
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                System.out.println("Circuit breaker state transition: " + event));
    }
    
    public CompletableFuture<InboundMessage> sendMethodCallWithCircuitBreaker(
            OutboundMethodCall call) {
        
        Supplier<CompletableFuture<InboundMessage>> decoratedSupplier = 
            CircuitBreaker.decorateSupplier(circuitBreaker, () -> 
                dbusConnection.sendRequest(call));
        
        return decoratedSupplier.get();
    }
}
```

### Retry Pattern

```java
@Component
public class RetryDBusService {
    private final Connection dbusConnection;
    private final Retry retry;
    
    public RetryDBusService(Connection dbusConnection) {
        this.dbusConnection = dbusConnection;
        this.retry = Retry.ofDefaults("dbus-retry");
        
        retry.getEventPublisher()
            .onRetry(event -> 
                System.out.println("Retry attempt: " + event.getNumberOfRetryAttempts()));
    }
    
    public CompletableFuture<InboundMessage> sendMethodCallWithRetry(
            OutboundMethodCall call) {
        
        Supplier<CompletableFuture<InboundMessage>> decoratedSupplier = 
            Retry.decorateSupplier(retry, () -> dbusConnection.sendRequest(call));
        
        return decoratedSupplier.get();
    }
}
```

## Testing Patterns

### Test Container Integration

```java
@Testcontainers
public class DBusIntegrationTest {
    
    @Container
    static GenericContainer<?> dbusContainer = new GenericContainer<>("dbus-test-image")
        .withExposedPorts(12345)
        .withEnv("DBUS_SYSTEM_BUS_ADDRESS", "tcp:host=0.0.0.0,port=12345");
    
    private Connection connection;
    
    @BeforeEach
    void setUp() throws Exception {
        String dbusAddress = String.format("tcp:host=localhost,port=%d", 
                                         dbusContainer.getMappedPort(12345));
        
        ConnectionConfig config = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(10))
            .build();
        
        connection = new NettyConnection(
            InetSocketAddress.createUnresolved("localhost", 
                                             dbusContainer.getMappedPort(12345)),
            config
        );
        
        connection.connect().toCompletableFuture().get();
    }
    
    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
    }
    
    @Test
    void testMethodCall() throws Exception {
        OutboundMethodCall call = createTestMethodCall();
        InboundMessage response = connection.sendRequest(call).get();
        
        assertThat(response).isNotNull();
    }
}
```

### Mock Testing

```java
public class DBusServiceTest {
    
    @Mock
    private Connection mockConnection;
    
    @InjectMocks
    private MyDBusService service;
    
    @Test
    void testServiceOperation() throws Exception {
        // Given
        InboundMessage mockResponse = createMockResponse();
        when(mockConnection.sendRequest(any(OutboundMethodCall.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        // When
        CompletableFuture<String> result = service.performOperation();
        
        // Then
        assertThat(result.get()).isEqualTo("expected-result");
        verify(mockConnection).sendRequest(any(OutboundMethodCall.class));
    }
}
```

## Best Practices Summary

1. **Connection Management**: Use dependency injection to manage connections
2. **Configuration**: Externalize configuration using properties files
3. **Error Handling**: Implement circuit breakers and retry mechanisms
4. **Monitoring**: Add metrics and health checks
5. **Testing**: Use test containers for integration testing
6. **Threading**: Be aware of threading models in UI frameworks
7. **Resource Cleanup**: Always close connections and remove handlers
8. **Signal Handling**: Use appropriate threading for UI updates
9. **Performance**: Monitor message throughput and latency
10. **Security**: Validate inputs and handle authentication properly

These patterns provide a solid foundation for integrating the D-Bus client library into various types of applications while maintaining good software engineering practices.