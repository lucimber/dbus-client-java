# Security Policy

## Supported Versions

We provide security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| 1.x.x   | :x:                |

## Reporting a Vulnerability

The D-Bus Client Java team takes security seriously. If you discover a security vulnerability, please report it to us responsibly.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please send an email to security@lucimber.com with the following information:

- A description of the vulnerability
- Steps to reproduce the issue
- Potential impact assessment
- Any suggested fixes (if available)

### What to Include

Please include as much information as possible to help us understand and reproduce the issue:

1. **Vulnerability Type**: What type of security issue is this? (e.g., authentication bypass, code injection, etc.)
2. **Affected Components**: Which parts of the library are affected?
3. **Attack Vector**: How can this vulnerability be exploited?
4. **Impact**: What is the potential impact if exploited?
5. **Proof of Concept**: A minimal example demonstrating the vulnerability
6. **Environment**: Java version, operating system, D-Bus version
7. **Suggested Fix**: If you have ideas for fixing the issue

### Response Timeline

We aim to respond to security reports within the following timeframes:

- **Initial Response**: Within 48 hours
- **Triage and Assessment**: Within 1 week
- **Fix Development**: Within 2-4 weeks (depending on complexity)
- **Public Disclosure**: After fix is released and users have had time to update

### Security Update Process

1. **Verification**: We verify and reproduce the reported vulnerability
2. **Assessment**: We assess the impact and severity
3. **Fix Development**: We develop and test a fix
4. **Security Advisory**: We prepare a security advisory
5. **Release**: We release the fix in a new version
6. **Disclosure**: We publicly disclose the vulnerability details

### Coordinated Disclosure

We follow a coordinated disclosure process:

- We will work with you to understand the issue
- We will keep you informed of our progress
- We will credit you in the security advisory (if desired)
- We ask that you do not publicly disclose the issue until we have released a fix

## Security Best Practices

### For Users

When using the D-Bus Client Java library:

1. **Keep Updated**: Always use the latest version with security fixes
2. **Input Validation**: Validate all input from D-Bus messages
3. **Authentication**: Use appropriate D-Bus authentication mechanisms
4. **Network Security**: Secure your D-Bus connections appropriately
5. **Error Handling**: Don't expose sensitive information in error messages

### For Developers

When contributing to the project:

1. **Secure Coding**: Follow secure coding practices
2. **Input Validation**: Validate all input parameters
3. **Authentication**: Implement proper authentication checks
4. **Error Handling**: Handle errors securely without information leakage
5. **Testing**: Include security test cases

## Common Security Considerations

### D-Bus Authentication

The library supports multiple SASL authentication mechanisms:

- **EXTERNAL**: Uses system credentials (Unix domain sockets)
- **DBUS_COOKIE_SHA1**: Uses cookie-based authentication (TCP connections)

Always verify that the authentication mechanism is appropriate for your use case.

### Message Validation

The library performs extensive validation of D-Bus messages:

- Type signature validation
- Message format validation
- Bounds checking for all data types
- UTF-8 validation for strings

### Transport Security

- **Unix Domain Sockets**: Secure by default on local systems
- **TCP Connections**: Consider additional security measures for network connections
- **TLS**: Currently not supported natively, consider network-level encryption

## Security Architecture

### Thread Safety

The library is designed with thread safety in mind:

- **Immutable Message Objects**: All message objects are immutable
- **Thread Isolation**: Clear separation between transport and application threads
- **Concurrent Access**: Safe concurrent access to connection objects

### Memory Safety

- **Bounds Checking**: All array and buffer operations are bounds-checked
- **Resource Management**: Proper cleanup of resources and connections
- **Memory Leaks**: Comprehensive testing for memory leaks

### Error Handling

- **Fail-Safe Defaults**: Default to secure configurations
- **Exception Safety**: Proper exception handling without information leakage
- **Logging**: Secure logging without exposing sensitive information

## Vulnerability Categories

We consider the following types of vulnerabilities:

### High Severity
- Remote code execution
- Authentication bypass
- Privilege escalation
- Information disclosure (sensitive data)

### Medium Severity
- Denial of service
- Information disclosure (non-sensitive data)
- Input validation bypasses
- Memory corruption

### Low Severity
- Information leakage in error messages
- Timing attacks
- Resource exhaustion

## Security Tools

The project uses several security tools:

- **CodeQL**: Static analysis for security vulnerabilities
- **Dependabot**: Automated dependency vulnerability scanning
- **PMD**: Static code analysis including security rules
- **JaCoCo**: Code coverage to ensure security tests are comprehensive

## Contact Information

For security-related questions or concerns:

- **Email**: security@lucimber.com
- **GPG Key**: Available on request
- **Response Time**: 48 hours for initial response

## Acknowledgments

We thank the security researchers who have responsibly disclosed vulnerabilities to us. Contributors to our security will be acknowledged in release notes unless they prefer to remain anonymous.

## Legal

This security policy is provided under the same Apache 2.0 license as the rest of the project. By reporting security vulnerabilities, you agree to our coordinated disclosure process.