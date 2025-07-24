# Integration Test Docker Configuration

This directory contains Docker configurations specifically for **integration testing** in CI/CD environments.

## Contents

- `Dockerfile.integration` - Comprehensive container setup for running integration tests with D-Bus

## Purpose

This Dockerfile is used by the Gradle `integrationTestContainer` task to:
1. Create a Linux environment with D-Bus daemon
2. Configure SASL authentication properly
3. Run integration tests in an isolated, reproducible environment
4. Handle test result collection and reporting

## Usage

This Dockerfile is automatically used by:
```bash
./gradlew integrationTestContainer
```

## Key Differences from Development Docker

| Aspect | Integration Test Docker | Development Docker (`/docker`) |
|--------|------------------------|--------------------------------|
| Purpose | CI/CD testing | Local development |
| Complexity | Comprehensive setup | Lightweight |
| D-Bus Config | Full test harness | Simple daemon |
| Test Runner | Built-in test execution | External test execution |
| Port | 12345 | 6667 |

## Do NOT Use for Development

For local development, use the Docker setup in `/docker` directory instead. This integration test container is specifically designed for automated testing and includes extensive logging and test harness setup that is unnecessary for development.