# D-Bus Client Java Examples

This directory contains practical examples demonstrating how to use the D-Bus Client Java library in real-world scenarios. All examples are implemented as a Gradle subproject with proper dependency management and executable tasks.

## Available Examples

### 1. Basic Client
**Level**: Beginner  
**Focus**: Core D-Bus operations

Learn the fundamentals of D-Bus communication:
- Establishing connections to system and session buses
- Creating and sending method calls
- Handling responses and errors
- Connection lifecycle management
- Basic configuration options

### 2. Signal Handling
**Level**: Intermediate  
**Focus**: Event-driven programming

Master D-Bus signal handling:
- Subscribing to signals
- Filtering signals by interface and member
- Creating custom signal handlers
- Building reactive applications
- Signal routing and aggregation

### 3. Service Discovery
**Level**: Intermediate  
**Focus**: Service introspection

Discover and interact with D-Bus services:
- Listing available services
- Introspecting service interfaces
- Querying service properties
- Monitoring service lifecycle
- Building service browsers

### 4. Authentication
**Level**: Advanced  
**Focus**: Security and authentication

Explore D-Bus authentication mechanisms:
- EXTERNAL authentication (Unix credentials)
- DBUS_COOKIE_SHA1 authentication
- ANONYMOUS authentication
- Custom authentication scenarios
- Security best practices

## Quick Start

### List Available Examples

```bash
# From project root
./gradlew :examples:listExamples
```

### Run Individual Examples

```bash
# Run basic client example
./gradlew :examples:runBasicClient

# Run signal handling example
./gradlew :examples:runSignalHandling

# Run service discovery example
./gradlew :examples:runServiceDiscovery

# Run authentication example
./gradlew :examples:runAuthentication
```

### Configuration Options

All examples support command-line arguments:

```bash
# Show help for any example
./gradlew :examples:runBasicClient -Dargs="--help"

# Use system bus instead of session bus
./gradlew :examples:runBasicClient -Dargs="--system-bus"

# Set custom timeout
./gradlew :examples:runSignalHandling -Dargs="--timeout 60"

# Enable verbose logging
./gradlew :examples:runServiceDiscovery -Dargs="--verbose"

# Enable debug logging at framework level
./gradlew :examples:runAuthentication -Dlogging.level.com.lucimber.dbus=DEBUG
```

### Demo Mode

Each example includes a demo mode for quick demonstrations:

```bash
# Run all examples in demo mode
./gradlew :examples:runAllExamples

# Run individual examples in demo mode
./gradlew :examples:runBasicClient -Dargs="--mode demo"
./gradlew :examples:runSignalHandling -Dargs="--mode demo"
./gradlew :examples:runServiceDiscovery -Dargs="--mode demo"
./gradlew :examples:runAuthentication -Dargs="--mode demo"
```

## Example-Specific Options

### Basic Client Example
```bash
# Interactive mode with custom timeout
./gradlew :examples:runBasicClient -Dargs="--timeout 30 --mode interactive"

# Batch mode for automated testing
./gradlew :examples:runBasicClient -Dargs="--mode batch"
```

### Signal Handling Example
```bash
# Monitor specific interfaces
./gradlew :examples:runSignalHandling -Dargs="--interfaces org.freedesktop.NetworkManager,org.freedesktop.UPower"

# Monitor all interfaces with verbose output
./gradlew :examples:runSignalHandling -Dargs="--verbose --timeout 120"
```

### Service Discovery Example
```bash
# Filter services by pattern
./gradlew :examples:runServiceDiscovery -Dargs="--filter freedesktop --verbose"

# Enable introspection
./gradlew :examples:runServiceDiscovery -Dargs="--introspect --timeout 60"
```

### Authentication Example
```bash
# Test different authentication mechanisms
./gradlew :examples:runAuthentication -Dargs="--mechanism EXTERNAL"
./gradlew :examples:runAuthentication -Dargs="--mechanism DBUS_COOKIE_SHA1"
./gradlew :examples:runAuthentication -Dargs="--mechanism ANONYMOUS"

# Test with system bus
./gradlew :examples:runAuthentication -Dargs="--mechanism EXTERNAL --system-bus"
```

## Project Structure

```
examples/
├── build.gradle.kts              # Gradle build configuration
└── src/main/java/com/lucimber/dbus/examples/
    ├── BasicClientExample.java   # Basic D-Bus client operations
    ├── SignalHandlingExample.java # Signal handling patterns
    ├── ServiceDiscoveryExample.java # Service discovery and introspection
    └── AuthenticationExample.java # Authentication mechanisms
```

## Learning Path

**For beginners:**
1. Start with Basic Client to understand core concepts
2. Move to Service Discovery to learn about D-Bus services
3. Explore Signal Handling for event-driven patterns

**For experienced developers:**
1. Review Authentication for security considerations
2. Combine patterns from multiple examples for complex scenarios
3. Refer to [Integration Patterns Guide](../guides/integration-patterns.md) for framework integration

## Common Use Cases

### Desktop Applications
- **System Integration**: Monitor system events through signals
- **Service Communication**: Interact with desktop services
- **Hardware Access**: Access hardware through D-Bus services

### System Services
- **Service Discovery**: Find and interact with system services
- **Event Monitoring**: React to system state changes
- **IPC Communication**: Inter-process communication

### Development Tools
- **Debugging**: Monitor D-Bus traffic and service behavior
- **Testing**: Automated testing of D-Bus services
- **Monitoring**: System and service monitoring

## Integration Examples

### Spring Boot Integration
```java
@Service
public class DBusService {
    private final Connection connection;
    
    public DBusService() {
        this.connection = NettyConnection.newSessionBusConnection();
    }
    
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        connection.connect().thenRun(() -> {
            // Set up signal handlers
            connection.getPipeline().addLast("signals", new MySignalHandler());
        });
    }
}
```

### Reactive Programming
```java
public class ReactiveDBusClient {
    public Flux<InboundSignal> signalStream() {
        return Flux.create(sink -> {
            connection.getPipeline().addLast("reactive", new AbstractInboundHandler() {
                @Override
                public void handleInboundMessage(Context ctx, InboundMessage msg) {
                    if (msg instanceof InboundSignal) {
                        sink.next((InboundSignal) msg);
                    }
                    ctx.propagateInboundMessage(msg);
                }
            });
        });
    }
}
```

## Development and Testing

### Building Examples

```bash
# Build all examples
./gradlew :examples:build

# Run tests (if any)
./gradlew :examples:test

# Clean build artifacts
./gradlew :examples:clean
```

### IDE Integration

The examples can be imported into any IDE that supports Gradle:
- IntelliJ IDEA: Open the project root directory
- Eclipse: Import as Gradle project
- VS Code: Use the Java Extension Pack

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure proper permissions for D-Bus socket access
2. **Connection Timeout**: Check D-Bus daemon is running and accessible
3. **Authentication Failed**: Verify authentication mechanism compatibility
4. **Service Not Found**: Ensure target service is running and accessible

### Debug Techniques

1. **Enable Logging**: Use debug logging with `-Dlogging.level.com.lucimber.dbus=DEBUG`
2. **Monitor Traffic**: Use D-Bus monitoring tools like `dbus-monitor`
3. **Test Connectivity**: Use `dbus-send` to test basic connectivity
4. **Check Permissions**: Verify file permissions for D-Bus sockets

### Example Debug Commands

```bash
# Enable full debug logging
./gradlew :examples:runBasicClient -Dlogging.level.com.lucimber.dbus=DEBUG

# Monitor D-Bus traffic (in separate terminal)
dbus-monitor --session

# Test basic connectivity
dbus-send --session --dest=org.freedesktop.DBus --type=method_call --print-reply /org/freedesktop/DBus org.freedesktop.DBus.GetId
```

## Contributing

To add new examples:
1. Create a new Java class in `examples/src/main/java/com/lucimber/dbus/examples/`
2. Add a corresponding Gradle task in `examples/build.gradle.kts`
3. Update the `listExamples` task to include the new example
4. Update this README with documentation

## Next Steps

After working through these examples:
1. Explore the [Architecture Documentation](../architecture/)
2. Review [Integration Patterns](../guides/integration-patterns.md)
3. Check out [Spring Boot Integration](../guides/spring-boot-integration.md)
4. Read the [Error Handling Guide](../guides/error-handling.md)