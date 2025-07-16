/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * Simple Authentication and Security Layer (SASL) implementation for secure D-Bus authentication.
 * 
 * <p>This package provides a complete SASL framework for D-Bus authentication,
 * supporting multiple authentication mechanisms and secure credential exchange.
 * Authentication is performed automatically during connection establishment.
 * 
 * <h2>Supported Authentication Mechanisms</h2>
 * 
 * <h3>EXTERNAL Authentication</h3>
 * <p>Unix credential-based authentication using process credentials:
 * 
 * <pre>{@code
 * // EXTERNAL authentication (default on Unix systems)
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanism("EXTERNAL")
 *     .build();
 * 
 * Connection connection = NettyConnection.newConnection(socketAddress, config);
 * 
 * // Authentication happens automatically during connect()
 * connection.connect().toCompletableFuture().get();
 * }</pre>
 * 
 * <h3>DBUS_COOKIE_SHA1 Authentication</h3>
 * <p>Cookie-based authentication using SHA1 challenge-response:
 * 
 * <pre>{@code
 * // DBUS_COOKIE_SHA1 authentication
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanism("DBUS_COOKIE_SHA1")
 *     .withCookieDirectory(Paths.get(System.getProperty("user.home"), ".dbus-keyrings"))
 *     .build();
 * 
 * Connection connection = NettyConnection.newConnection(socketAddress, config);
 * connection.connect().toCompletableFuture().get();
 * }</pre>
 * 
 * <h3>ANONYMOUS Authentication</h3>
 * <p>Anonymous authentication for public services:
 * 
 * <pre>{@code
 * // ANONYMOUS authentication
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanism("ANONYMOUS")
 *     .build();
 * 
 * Connection connection = NettyConnection.newConnection(socketAddress, config);
 * connection.connect().toCompletableFuture().get();
 * }</pre>
 * 
 * <h2>Authentication Process</h2>
 * 
 * <p>The SASL authentication process follows these steps:
 * 
 * <ol>
 * <li><strong>Mechanism Selection:</strong> Client and server negotiate authentication mechanism</li>
 * <li><strong>Challenge Exchange:</strong> Multi-step challenge-response authentication</li>
 * <li><strong>Credential Validation:</strong> Server validates client credentials</li>
 * <li><strong>Authentication Success:</strong> Secure communication channel established</li>
 * </ol>
 * 
 * <pre>{@code
 * // Authentication process flow
 * // 1. Client sends AUTH command with mechanism
 * // 2. Server responds with challenge or acceptance
 * // 3. Client sends response to challenge
 * // 4. Server validates and responds with OK or ERROR
 * // 5. Client sends BEGIN to start message exchange
 * }</pre>
 * 
 * <h2>Configuration Options</h2>
 * 
 * <h3>Authentication Mechanism Selection</h3>
 * <pre>{@code
 * // Configure preferred authentication mechanisms
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanisms(Arrays.asList(
 *         "EXTERNAL",
 *         "DBUS_COOKIE_SHA1",
 *         "ANONYMOUS"
 *     ))
 *     .withAuthTimeout(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 * 
 * <h3>Credential Configuration</h3>
 * <pre>{@code
 * // Configure authentication credentials
 * SaslConfig saslConfig = SaslConfig.builder()
 *     .withUserId(System.getProperty("user.name"))
 *     .withCookieDirectory(Paths.get(System.getProperty("user.home"), ".dbus-keyrings"))
 *     .withAuthTimeout(Duration.ofSeconds(30))
 *     .withDebugEnabled(true)
 *     .build();
 * 
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withSaslConfig(saslConfig)
 *     .build();
 * }</pre>
 * 
 * <h2>Security Features</h2>
 * 
 * <h3>Secure Credential Handling</h3>
 * <p>All credentials are handled securely:
 * 
 * <pre>{@code
 * // Secure credential management
 * public class SecureCredentialManager {
 *     private final char[] password;
 *     
 *     public SecureCredentialManager(char[] password) {
 *         this.password = password.clone();
 *     }
 *     
 *     public char[] getPassword() {
 *         return password.clone();
 *     }
 *     
 *     public void clear() {
 *         Arrays.fill(password, '\0');
 *     }
 * }
 * }</pre>
 * 
 * <h3>Challenge-Response Security</h3>
 * <p>DBUS_COOKIE_SHA1 provides secure challenge-response authentication:
 * 
 * <pre>{@code
 * // Cookie-based authentication flow
 * // 1. Server generates random challenge
 * // 2. Client reads cookie from keyring
 * // 3. Client computes SHA1(challenge:cookie:client_challenge)
 * // 4. Server validates response
 * // 5. Secure session established
 * }</pre>
 * 
 * <h2>Error Handling</h2>
 * 
 * <p>Comprehensive error handling for authentication failures:
 * 
 * <pre>{@code
 * // Handle authentication errors
 * connection.connect().exceptionally(throwable -> {
 *     if (throwable instanceof AuthenticationException) {
 *         AuthenticationException authEx = (AuthenticationException) throwable;
 *         
 *         switch (authEx.getErrorType()) {
 *             case MECHANISM_NOT_SUPPORTED:
 *                 System.err.println("Authentication mechanism not supported");
 *                 break;
 *             case INVALID_CREDENTIALS:
 *                 System.err.println("Invalid credentials provided");
 *                 break;
 *             case AUTHENTICATION_TIMEOUT:
 *                 System.err.println("Authentication timeout");
 *                 break;
 *             case COOKIE_NOT_FOUND:
 *                 System.err.println("Authentication cookie not found");
 *                 break;
 *             default:
 *                 System.err.println("Authentication failed: " + authEx.getMessage());
 *         }
 *     }
 *     return null;
 * });
 * }</pre>
 * 
 * <h2>Custom Authentication Mechanisms</h2>
 * 
 * <p>Extend the framework with custom authentication mechanisms:
 * 
 * <pre>{@code
 * // Custom authentication mechanism
 * public class CustomAuthMechanism implements SaslMechanism {
 *     @Override
 *     public String getMechanismName() {
 *         return "CUSTOM";
 *     }
 *     
 *     @Override
 *     public boolean requiresCredentials() {
 *         return true;
 *     }
 *     
 *     @Override
 *     public byte[] getInitialResponse() {
 *         // Return initial authentication data
 *         return generateInitialResponse();
 *     }
 *     
 *     @Override
 *     public byte[] evaluateChallenge(byte[] challenge) {
 *         // Process server challenge and return response
 *         return processChallenge(challenge);
 *     }
 *     
 *     @Override
 *     public boolean isComplete() {
 *         return authenticationComplete;
 *     }
 * }
 * 
 * // Register custom mechanism
 * SaslRegistry.registerMechanism(new CustomAuthMechanism());
 * }</pre>
 * 
 * <h2>Debugging and Logging</h2>
 * 
 * <p>Enable detailed authentication logging for troubleshooting:
 * 
 * <pre>{@code
 * // Enable SASL debugging
 * SaslConfig saslConfig = SaslConfig.builder()
 *     .withDebugEnabled(true)
 *     .withLogLevel(LogLevel.DEBUG)
 *     .build();
 * 
 * // Authentication events are logged at DEBUG level
 * Logger logger = LoggerFactory.getLogger(SaslHandler.class);
 * logger.debug("Authentication mechanism: {}", mechanism);
 * logger.debug("Challenge received: {}", Base64.getEncoder().encodeToString(challenge));
 * logger.debug("Response sent: {}", Base64.getEncoder().encodeToString(response));
 * }</pre>
 * 
 * <h2>Platform-Specific Considerations</h2>
 * 
 * <h3>Unix Systems</h3>
 * <p>EXTERNAL authentication using Unix credentials:
 * 
 * <pre>{@code
 * // Unix-specific configuration
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanism("EXTERNAL")
 *     .withUnixCredentialsEnabled(true)
 *     .build();
 * 
 * // Process UID/GID are used for authentication
 * }</pre>
 * 
 * <h3>Windows Systems</h3>
 * <p>TCP-based authentication for Windows:
 * 
 * <pre>{@code
 * // Windows-specific configuration
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withPreferredAuthMechanism("DBUS_COOKIE_SHA1")
 *     .withCookieDirectory(Paths.get(System.getProperty("user.home"), ".dbus-keyrings"))
 *     .build();
 * }</pre>
 * 
 * <h2>Cookie Management</h2>
 * 
 * <p>DBUS_COOKIE_SHA1 uses cookie files for authentication:
 * 
 * <pre>{@code
 * // Cookie file management
 * Path cookieDir = Paths.get(System.getProperty("user.home"), ".dbus-keyrings");
 * Path cookieFile = cookieDir.resolve("org_freedesktop_general");
 * 
 * // Cookie file format:
 * // ID timestamp cookie
 * // 1 1234567890 abcdef0123456789
 * 
 * // Automatic cookie management
 * CookieManager cookieManager = new CookieManager(cookieDir);
 * String cookie = cookieManager.getCookie("org_freedesktop_general", 1);
 * }</pre>
 * 
 * <h2>Performance Considerations</h2>
 * 
 * <p>Authentication performance optimizations:
 * 
 * <ul>
 * <li><strong>Mechanism Caching:</strong> Authentication mechanisms are cached after first use</li>
 * <li><strong>Credential Caching:</strong> Credentials are cached for connection reuse</li>
 * <li><strong>Timeout Optimization:</strong> Authentication timeouts are configurable</li>
 * <li><strong>Async Processing:</strong> Authentication is performed asynchronously</li>
 * </ul>
 * 
 * <pre>{@code
 * // Performance-optimized configuration
 * SaslConfig saslConfig = SaslConfig.builder()
 *     .withAuthTimeout(Duration.ofSeconds(10))
 *     .withCredentialCacheEnabled(true)
 *     .withMechanismCacheEnabled(true)
 *     .build();
 * }</pre>
 * 
 * <h2>Best Practices</h2>
 * 
 * <ul>
 * <li><strong>Mechanism Selection:</strong> Use EXTERNAL for local connections, DBUS_COOKIE_SHA1 for remote</li>
 * <li><strong>Credential Security:</strong> Clear credentials from memory after use</li>
 * <li><strong>Error Handling:</strong> Handle authentication failures gracefully</li>
 * <li><strong>Timeout Configuration:</strong> Set appropriate authentication timeouts</li>
 * <li><strong>Debugging:</strong> Enable SASL debugging for troubleshooting</li>
 * <li><strong>Cookie Management:</strong> Ensure proper cookie file permissions</li>
 * </ul>
 * 
 * <h2>Compatibility</h2>
 * 
 * <p>This SASL implementation is compatible with:
 * 
 * <ul>
 * <li>D-Bus specification SASL requirements</li>
 * <li>Standard D-Bus daemons (dbus-daemon, systemd)</li>
 * <li>Cross-platform D-Bus implementations</li>
 * <li>Both system and session bus authentication</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.connection.Connection
 * @see com.lucimber.dbus.connection.ConnectionConfig
 * @see com.lucimber.dbus.netty.NettyConnection
 * @since 1.0
 */
package com.lucimber.dbus.connection.sasl;
