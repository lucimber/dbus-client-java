# Platform Setup Guide

This guide helps developers set up D-Bus development environments on different platforms.

## Platform Overview

| Platform | Native D-Bus | Recommended Approach |
|----------|--------------|---------------------|
| Linux | ✅ Yes | Use system D-Bus directly |
| macOS | ❌ No | Use Docker or VM |
| Windows | ❌ No | Use Docker or WSL2 |

## Linux Setup

D-Bus is native to Linux and typically pre-installed.

### Check D-Bus Status

```bash
# Check if D-Bus is running
systemctl status dbus

# Check D-Bus version
dbus-daemon --version

# List available services
dbus-send --system --dest=org.freedesktop.DBus \
  --type=method_call --print-reply \
  /org/freedesktop/DBus org.freedesktop.DBus.ListNames
```

### Session vs System Bus

```bash
# System bus socket (requires permissions)
ls -la /var/run/dbus/system_bus_socket

# Session bus (user-specific)
echo $DBUS_SESSION_BUS_ADDRESS
```

### Troubleshooting Linux

If D-Bus is not running:

```bash
# Install D-Bus (Ubuntu/Debian)
sudo apt-get install dbus

# Install D-Bus (Fedora/RHEL)
sudo dnf install dbus

# Start D-Bus
sudo systemctl start dbus
sudo systemctl enable dbus
```

## macOS Setup

D-Bus is not native to macOS. Use Docker for development.

### Option 1: Docker (Recommended)

Use the provided Docker Compose configuration in the `docker` directory:

```yaml
version: '3.8'

services:
  dbus:
    image: ubuntu:22.04
    container_name: dbus-dev
    command: |
      bash -c "
        apt-get update && apt-get install -y dbus && 
        service dbus start &&
        dbus-daemon --session --fork --print-address > /tmp/session_address &&
        echo 'D-Bus ready. Session address:' &&
        cat /tmp/session_address &&
        tail -f /dev/null
      "
    volumes:
      - dbus-socket:/var/run/dbus
      - ./your-app:/app
    environment:
      - DISPLAY=:0
    networks:
      - dbus-net

  # Your Java application container
  app:
    build: .
    depends_on:
      - dbus
    volumes:
      - dbus-socket:/var/run/dbus:ro
      - ./your-app:/app
    environment:
      - DBUS_SYSTEM_BUS_ADDRESS=unix:path=/var/run/dbus/system_bus_socket
    networks:
      - dbus-net

volumes:
  dbus-socket:

networks:
  dbus-net:
```

Run with:

```bash
# Start D-Bus container
docker-compose -f docker/docker-compose.yml up -d dbus

# Run your tests
docker-compose -f docker/docker-compose.yml run app ./gradlew test

# View logs
docker-compose -f docker/docker-compose.yml logs -f dbus
```

### Option 2: Homebrew (Limited Support)

```bash
# Install D-Bus (not recommended for development)
brew install dbus

# Start D-Bus service
brew services start dbus

# Note: This provides limited functionality compared to Linux
```

### Option 3: Virtual Machine

Use a Linux VM for full D-Bus functionality:

1. Install VirtualBox or UTM
2. Create Ubuntu/Fedora VM
3. Share project folder with VM
4. Develop inside VM

## Windows Setup

D-Bus is not native to Windows. Multiple options available:

### Option 1: WSL2 (Recommended for Windows 10/11)

```powershell
# Install WSL2
wsl --install -d Ubuntu

# Inside WSL2
sudo apt-get update
sudo apt-get install dbus

# Start D-Bus
sudo service dbus start

# Set up X11 for GUI apps (optional)
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0
```

### Option 2: Docker Desktop

Same `docker-compose.yml` as macOS, then:

```powershell
# Start containers
docker-compose up -d

# Run tests
docker-compose run app gradle test
```

### Option 3: Native Windows (Experimental)

Some D-Bus ports exist for Windows but are not recommended for development:
- [windbus](https://github.com/windbus/windbus) - Experimental
- Use TCP transport instead of Unix sockets

## Docker Development Environment

### Complete Development Setup

Create a comprehensive development environment:

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  # D-Bus daemon with development tools
  dbus-dev:
    build:
      context: .
      dockerfile: Dockerfile.dbus
    container_name: dbus-dev
    volumes:
      - ./src:/workspace/src:ro
      - ./lib:/workspace/lib:ro
      - dbus-system:/var/run/dbus
      - dbus-session:/tmp/dbus
    environment:
      - DBUS_VERBOSE=1
    ports:
      - "6667:6667"  # TCP D-Bus port
    healthcheck:
      test: ["CMD", "dbus-send", "--system", "--print-reply", "--dest=org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus.GetId"]
      interval: 5s
      timeout: 3s
      retries: 5

  # Test runner
  test-runner:
    build:
      context: .
      dockerfile: Dockerfile
    depends_on:
      dbus-dev:
        condition: service_healthy
    volumes:
      - ./:/workspace
      - dbus-system:/var/run/dbus:ro
      - gradle-cache:/home/gradle/.gradle
    environment:
      - DBUS_SYSTEM_BUS_ADDRESS=unix:path=/var/run/dbus/system_bus_socket
      - GRADLE_USER_HOME=/home/gradle/.gradle
    command: ./gradlew test

volumes:
  dbus-system:
  dbus-session:
  gradle-cache:
```

Create `Dockerfile.dbus`:

```dockerfile
FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    dbus \
    dbus-x11 \
    libdbus-1-dev \
    python3-dbus \
    d-feet \
    bustle \
    && rm -rf /var/lib/apt/lists/*

# D-Bus configuration for development
COPY <<EOF /etc/dbus-1/system.d/dev.conf
<!DOCTYPE busconfig PUBLIC
 "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
<busconfig>
  <policy context="default">
    <allow own="*"/>
    <allow send_destination="*"/>
    <allow receive_sender="*"/>
  </policy>
</busconfig>
EOF

# Startup script
COPY <<'EOF' /usr/local/bin/start-dbus.sh
#!/bin/bash
set -e

# Start system bus
mkdir -p /var/run/dbus
dbus-daemon --system --fork

# Start session bus
eval $(dbus-launch --sh-syntax)
echo "DBUS_SESSION_BUS_ADDRESS=$DBUS_SESSION_BUS_ADDRESS" > /tmp/dbus-session

# Start TCP listener for remote connections
dbus-daemon --config-file=/usr/share/dbus-1/session.conf \
  --address=tcp:host=0.0.0.0,port=6667 \
  --print-address --fork

echo "D-Bus services started"
echo "System bus: unix:path=/var/run/dbus/system_bus_socket"
echo "Session bus: $DBUS_SESSION_BUS_ADDRESS"
echo "TCP bus: tcp:host=localhost,port=6667"

# Keep container running
tail -f /dev/null
EOF

RUN chmod +x /usr/local/bin/start-dbus.sh

CMD ["/usr/local/bin/start-dbus.sh"]
```

### Using the Docker Environment

```bash
# Start development environment
docker-compose -f docker-compose.dev.yml up -d

# Run tests
docker-compose -f docker-compose.dev.yml run test-runner

# Connect to D-Bus container for debugging
docker exec -it dbus-dev bash

# Inside container - monitor D-Bus traffic
dbus-monitor --system

# Test D-Bus connection
dbus-send --system --dest=org.freedesktop.DBus \
  --type=method_call --print-reply \
  /org/freedesktop/DBus org.freedesktop.DBus.ListNames
```

## TCP Transport for Cross-Platform

For maximum portability, use TCP transport:

```java
// Connect via TCP (works everywhere)
Connection connection = new NettyConnection(
    new InetSocketAddress("localhost", 6667)
);

// Configure for TCP
ConnectionConfig config = ConnectionConfig.builder()
    .withConnectTimeout(Duration.ofSeconds(10))
    .withTransportType(TransportType.TCP)
    .build();
```

Configure D-Bus for TCP:

```xml
<!-- /etc/dbus-1/session.conf -->
<busconfig>
  <listen>tcp:host=0.0.0.0,port=6667,family=ipv4</listen>
  <auth>ANONYMOUS</auth>
  <allow_anonymous/>
</busconfig>
```

## Platform-Specific Connection Code

```java
public class PlatformConnection {
    public static Connection createConnection() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("linux")) {
            // Native Unix socket on Linux
            return NettyConnection.newSystemBusConnection();
            
        } else if (os.contains("mac") || os.contains("win")) {
            // TCP connection for non-Linux
            ConnectionConfig config = ConnectionConfig.builder()
                .withConnectTimeout(Duration.ofSeconds(10))
                .build();
            
            // Try Docker TCP first
            try {
                Connection conn = new NettyConnection(
                    new InetSocketAddress("localhost", 6667),
                    config
                );
                conn.connect().toCompletableFuture().get();
                return conn;
            } catch (Exception e) {
                throw new RuntimeException(
                    "D-Bus not available. Please run: docker-compose up -d"
                );
            }
        }
        
        throw new UnsupportedOperationException(
            "Unsupported platform: " + os
        );
    }
}
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Cross-Platform Tests

on: [push, pull_request]

jobs:
  test-linux:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: |
          sudo apt-get update
          sudo apt-get install -y dbus
          sudo service dbus start
      - run: ./gradlew test

  test-macos:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: |
          docker-compose up -d dbus
          sleep 5
      - run: ./gradlew test

  test-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: |
          docker-compose up -d dbus
          Start-Sleep -Seconds 5
      - run: ./gradlew test
```

## Troubleshooting

### Common Issues

**"Cannot connect to D-Bus"**
- Linux: Check `systemctl status dbus`
- Docker: Check `docker-compose logs dbus`
- Permissions: Add user to `dbus` group

**"Address already in use"**
- TCP port conflict: Change port in docker-compose.yml
- Unix socket exists: `sudo rm /var/run/dbus/system_bus_socket`

**"Authentication failed"**
- Check D-Bus policy files in `/etc/dbus-1/system.d/`
- For development, use permissive policies
- TCP: Enable anonymous authentication

### Debug Commands

```bash
# List D-Bus services
dbus-send --print-reply --dest=org.freedesktop.DBus \
  /org/freedesktop/DBus org.freedesktop.DBus.ListNames

# Monitor D-Bus traffic
dbus-monitor

# Check D-Bus environment
env | grep DBUS

# Test connection
dbus-send --print-reply --dest=org.freedesktop.DBus \
  /org/freedesktop/DBus org.freedesktop.DBus.GetId
```

## Best Practices

1. **Use Docker for development** - Consistent across all platforms
2. **TCP for testing** - Works everywhere, easier to debug
3. **Unix sockets for production** - Better performance on Linux
4. **Container health checks** - Ensure D-Bus is ready before connecting
5. **Volume mounts** - Share sockets between containers
6. **Environment variables** - Configure D-Bus addresses properly