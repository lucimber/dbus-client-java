# Contributing to D-Bus Client Java

Thank you for your interest in contributing to the D-Bus Client Java library! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Code Style](#code-style)
- [Commit Messages](#commit-messages)
- [Developer Certificate of Origin](#developer-certificate-of-origin)

## Code of Conduct

By participating in this project, you are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started

### Prerequisites

- Java 17 or higher
- Docker (for integration tests)
- Git

### Development Setup

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/your-username/dbus-client-java.git
   cd dbus-client-java
   ```
3. **Set up the upstream remote**:
   ```bash
   git remote add upstream https://github.com/lucimber/dbus-client-java.git
   ```
4. **Build the project**:
   ```bash
   ./gradlew build
   ```

## Making Changes

### Before You Start

1. **Check existing issues** to see if someone is already working on what you want to do
2. **Create an issue** if one doesn't exist for your change
3. **Discuss your approach** in the issue before starting work on large changes

### Development Workflow

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Make your changes** following the [Code Style](#code-style) guidelines
3. **Write tests** for your changes
4. **Run tests** to ensure everything works:
   ```bash
   ./gradlew test
   ./gradlew integrationTest
   ```
5. **Commit your changes** with appropriate [commit messages](#commit-messages)
6. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

## Testing

### Unit Tests

Run unit tests with:
```bash
./gradlew test
```

For memory-intensive tests:
```bash
./gradlew test -PwithMemoryIntensiveTests
```

### Integration Tests

Run integration tests (containerized):
```bash
./gradlew integrationTest
```

With verbose output:
```bash
./gradlew integrationTest -PshowOutput
```

### Performance Tests

Run performance tests:
```bash
./gradlew performanceTest
```

### Code Coverage

Generate coverage reports:
```bash
./gradlew jacocoTestReport
```

## Submitting Changes

### Pull Request Process

1. **Update your branch** with the latest changes from upstream:
   ```bash
   git checkout main
   git pull upstream main
   git checkout feature/your-feature-name
   git rebase main
   ```

2. **Create a Pull Request** on GitHub with:
   - Clear description of the changes
   - Reference to related issues
   - Screenshots or examples if applicable

3. **Address review feedback** promptly and professionally

4. **Ensure all checks pass**:
   - CI builds successfully
   - All tests pass
   - Code coverage is maintained or improved

### Pull Request Guidelines

- **Keep PRs focused** - one feature or bug fix per PR
- **Write descriptive titles** and descriptions
- **Include tests** for new functionality
- **Update documentation** if needed
- **Follow the PR template** provided

## Code Style

### Java Code Style

- **Follow standard Java conventions**
- **Use 4 spaces for indentation** (no tabs)
- **Line length**: 120 characters maximum
- **Use meaningful variable and method names**
- **Add JavaDoc** for public APIs
- **No trailing whitespace**

### Code Quality Tools

The project uses several code quality tools:

- **Checkstyle**: Enforces coding standards
- **PMD**: Static code analysis
- **JaCoCo**: Code coverage measurement

Run quality checks:
```bash
./gradlew check
```

### Architecture Guidelines

- **Follow the existing patterns** in the codebase
- **Handlers can safely perform blocking operations** - they run on dedicated thread pools
- **Use the builder pattern** for complex message construction
- **Prefer immutable objects** where possible
- **Handle errors gracefully** and provide meaningful error messages

## Commit Messages

### Format

```
type(scope): brief description

Detailed description if needed

- Additional details
- More information

Signed-off-by: Your Name <your.email@example.com>
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code formatting changes
- **refactor**: Code restructuring without behavior changes
- **test**: Adding or modifying tests
- **ci**: Changes to CI configuration
- **perf**: Performance improvements

### Examples

```
feat(connection): add automatic reconnection support

Add exponential backoff retry logic for connection failures with
configurable max attempts and delay settings.

- Implement ReconnectionHandler with customizable strategy
- Add connection health monitoring
- Update documentation with reconnection examples

Signed-off-by: Jane Doe <jane.doe@example.com>
```

## Developer Certificate of Origin

By contributing to this project, you certify that:

> The contribution was created in whole or in part by me and I have the right to submit it under the open source license indicated in the file; or
> 
> The contribution is based upon previous work that, to the best of my knowledge, is covered under an appropriate open source license and I have the right under that license to submit that work with modifications; or
> 
> The contribution was provided directly to me by some other person who certified the above conditions and I have not modified it.

### Signing Off Commits

All commits must be signed off by adding the following line to your commit message:

```
Signed-off-by: Your Name <your.email@example.com>
```

Use the `-s` flag with git commit:
```bash
git commit -s -m "your commit message"
```

## Documentation

### What to Document

- **Public APIs**: All public methods and classes need JavaDoc
- **Architecture decisions**: Document why, not just what
- **Examples**: Provide working examples for new features
- **Configuration**: Document all configuration options

### Documentation Format

- Use **JavaDoc** for API documentation
- Use **Markdown** for user guides and tutorials
- Include **code examples** that actually work
- Keep documentation **up to date** with code changes

## Getting Help

- **Issues**: Use GitHub issues for bug reports and feature requests
- **Discussions**: Use GitHub discussions for questions and ideas
- **Documentation**: Check the [developer guide](docs/developer-guide.md) for detailed information

## Recognition

Contributors are recognized in:
- Git commit history
- GitHub contributor statistics
- Release notes (for significant contributions)

Thank you for contributing to D-Bus Client Java! 🚀