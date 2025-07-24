# Docker Development Environment

This directory contains Docker configurations for cross-platform D-Bus development.

## Contents

- `docker-compose.yml` - Main Docker Compose configuration for running D-Bus daemon
- `Dockerfile.dev` - Lightweight container for running tests during development
- `test-policies/` - D-Bus policy files for development/testing

## Important Note

This directory contains Docker files for **local development only**. The integration test Docker configuration is separate and located at:
- `lib/src/test/docker/Dockerfile.integration` - Used by `./gradlew integrationTestContainer`

These are intentionally kept separate to avoid confusion between development and CI/CD environments.

## Quick Start

From the project root directory:

```bash
# Start D-Bus daemon
docker-compose -f docker/docker-compose.yml up -d

# Run tests
./gradlew test

# Stop D-Bus daemon
docker-compose -f docker/docker-compose.yml down
```

## Services

### dbus
- Ubuntu-based D-Bus daemon
- Exposes both Unix socket and TCP (port 6667)
- Includes permissive policies for testing

### test-runner (optional)
- Runs tests inside container with D-Bus access
- Use with: `docker-compose -f docker/docker-compose.yml run --rm test-runner`

## Platform Notes

- **Linux**: Optional, can use system D-Bus instead
- **macOS/Windows**: Required for D-Bus functionality
- **CI/CD**: Ensures consistent test environment

See [Platform Setup Guide](../docs/guides/platform-setup.md) for detailed instructions.