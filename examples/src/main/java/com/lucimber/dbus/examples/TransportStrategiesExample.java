/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.sasl.SaslAnonymousAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslCookieAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslExternalAuthConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.netty.NettyTcpStrategy;
import com.lucimber.dbus.netty.NettyUnixSocketStrategy;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating different transport strategies and SASL configurations.
 * 
 * This example shows how to:
 * - Use Unix domain socket transport (default for system/session bus)
 * - Use TCP transport for remote D-Bus connections
 * - Configure different SASL authentication mechanisms
 * - Handle transport-specific connection options
 */
public class TransportStrategiesExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportStrategiesExample.class);
    
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 D-Bus Transport Strategies Example");
        System.out.println("====================================");
        
        // Example 1: Standard Unix socket connections
        demonstrateUnixSocketConnections();
        
        // Example 2: TCP transport for remote connections
        demonstrateTcpTransport();
        
        // Example 3: Custom SASL configurations
        demonstrateSaslConfigurations();
        
        System.out.println("🏁 Transport strategies example completed!");
    }
    
    /**
     * Demonstrates Unix domain socket connections (standard D-Bus transport).
     */
    private static void demonstrateUnixSocketConnections() throws Exception {
        System.out.println("\n📋 Example 1: Unix Domain Socket Connections");
        System.out.println("============================================");
        
        // Session bus connection (most common)
        System.out.println("🔗 Connecting to session bus...");
        Connection sessionConnection = NettyConnection.newSessionBusConnection();
        
        try {
            sessionConnection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
            System.out.println("✅ Session bus connected successfully!");
            
            testBasicDbusCall(sessionConnection, "Session Bus");
            
        } catch (Exception e) {
            System.out.println("⚠️  Session bus connection failed: " + e.getMessage());
            LOGGER.debug("Session bus connection error", e);
        } finally {
            sessionConnection.close();
        }
        
        // System bus connection (requires appropriate permissions)
        System.out.println("\n🔗 Connecting to system bus...");
        Connection systemConnection = NettyConnection.newSystemBusConnection();
        
        try {
            systemConnection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
            System.out.println("✅ System bus connected successfully!");
            
            testBasicDbusCall(systemConnection, "System Bus");
            
        } catch (Exception e) {
            System.out.println("⚠️  System bus connection failed: " + e.getMessage());
            System.out.println("   (This is normal if you don't have system bus permissions)");
            LOGGER.debug("System bus connection error", e);
        } finally {
            systemConnection.close();
        }
        
        // Custom Unix socket path
        System.out.println("\n🔗 Custom Unix socket configuration...");
        ConnectionConfig customUnixConfig = ConnectionConfig.builder()
            .withTransportStrategy(new NettyUnixSocketStrategy("/tmp/custom-dbus-socket"))
            .withSaslConfig(new SaslExternalAuthConfig())
            .withConnectionTimeout(Duration.ofSeconds(5))
            .build();
            
        Connection customUnixConnection = NettyConnection.create(customUnixConfig);
        
        try {
            customUnixConnection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);
            System.out.println("✅ Custom Unix socket connected!");
            
        } catch (Exception e) {
            System.out.println("⚠️  Custom Unix socket failed: " + e.getMessage());
            System.out.println("   (Expected - custom socket doesn't exist)");
        } finally {
            customUnixConnection.close();
        }
    }
    
    /**
     * Demonstrates TCP transport for remote D-Bus connections.
     */
    private static void demonstrateTcpTransport() throws Exception {
        System.out.println("\n📋 Example 2: TCP Transport");
        System.out.println("===========================");
        
        // TCP connection to localhost (for testing with D-Bus over TCP)
        System.out.println("🌐 Configuring TCP transport...");
        
        ConnectionConfig tcpConfig = ConnectionConfig.builder()
            .withTransportStrategy(new NettyTcpStrategy("localhost", 12345))
            .withSaslConfig(new SaslAnonymousAuthConfig())
            .withConnectionTimeout(Duration.ofSeconds(5))
            .withKeepAlive(true)
            .withTcpNoDelay(true)
            .build();
            
        Connection tcpConnection = NettyConnection.create(tcpConfig);
        
        try {
            System.out.println("🔗 Connecting to D-Bus over TCP (localhost:12345)...");
            tcpConnection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);
            System.out.println("✅ TCP connection established!");
            
            testBasicDbusCall(tcpConnection, "TCP Connection");
            
        } catch (Exception e) {
            System.out.println("⚠️  TCP connection failed: " + e.getMessage());
            System.out.println("   (Expected - no D-Bus daemon listening on TCP port 12345)");
            System.out.println("   To test TCP transport, start D-Bus daemon with:");
            System.out.println("   dbus-daemon --config-file=tcp-config.xml --print-address");
        } finally {
            tcpConnection.close();
        }
        
        // TCP with advanced configuration
        System.out.println("\n🌐 Advanced TCP configuration...");
        
        ConnectionConfig advancedTcpConfig = ConnectionConfig.builder()
            .withTransportStrategy(new NettyTcpStrategy("remote-host.example.com", 55555))
            .withSaslConfig(new SaslCookieAuthConfig("/path/to/dbus/cookies"))
            .withConnectionTimeout(Duration.ofSeconds(30))
            .withKeepAlive(true)
            .withTcpNoDelay(true)
            .withSoTimeout(Duration.ofSeconds(60))
            .withMaxReconnectAttempts(5)
            .withReconnectDelay(Duration.ofSeconds(2))
            .build();
            
        Connection advancedTcpConnection = NettyConnection.create(advancedTcpConfig);
        
        System.out.println("📝 Advanced TCP config created:");
        System.out.println("   Host: remote-host.example.com:55555");  
        System.out.println("   SASL: DBUS_COOKIE_SHA1");
        System.out.println("   Timeout: 30s, Reconnect attempts: 5");
        System.out.println("   (Not connecting - remote host doesn't exist)");
        
        advancedTcpConnection.close();
    }
    
    /**
     * Demonstrates different SASL authentication configurations.
     */
    private static void demonstrateSaslConfigurations() throws Exception {
        System.out.println("\n📋 Example 3: SASL Authentication Configurations");
        System.out.println("================================================");
        
        // EXTERNAL authentication (Unix credentials)
        System.out.println("🔐 SASL EXTERNAL Authentication:");
        ConnectionConfig externalConfig = ConnectionConfig.builder()
            .withAddress("unix:path=/var/run/dbus/system_bus_socket")
            .withSaslConfig(new SaslExternalAuthConfig())
            .withConnectionTimeout(Duration.ofSeconds(10))
            .build();
        System.out.println("   Uses Unix process credentials for authentication");
        System.out.println("   Best for local system/session bus connections");
        
        // DBUS_COOKIE_SHA1 authentication
        System.out.println("\n🔐 SASL DBUS_COOKIE_SHA1 Authentication:");
        ConnectionConfig cookieConfig = ConnectionConfig.builder()
            .withAddress("tcp:host=localhost,port=12345")
            .withSaslConfig(new SaslCookieAuthConfig("/home/user/.dbus-keyrings"))
            .withConnectionTimeout(Duration.ofSeconds(10))
            .build();
        System.out.println("   Uses cookie-based authentication with shared secret");
        System.out.println("   Good for TCP connections and cross-user authentication");
        
        // ANONYMOUS authentication
        System.out.println("\n🔐 SASL ANONYMOUS Authentication:");
        ConnectionConfig anonymousConfig = ConnectionConfig.builder()
            .withAddress("tcp:host=public-dbus.example.com,port=12345")
            .withSaslConfig(new SaslAnonymousAuthConfig())
            .withConnectionTimeout(Duration.ofSeconds(10))
            .build();
        System.out.println("   No authentication required");
        System.out.println("   Only for public/unrestricted D-Bus services");
        
        System.out.println("\n💡 SASL Configuration Tips:");
        System.out.println("   • EXTERNAL: Default for Unix sockets, uses process UID/GID");
        System.out.println("   • COOKIE: Requires shared keyring directory, good for TCP");
        System.out.println("   • ANONYMOUS: No security, use only for public services");
        System.out.println("   • Choose based on your security requirements and transport");
    }
    
    /**
     * Helper method to test basic D-Bus functionality.
     */
    private static void testBasicDbusCall(Connection connection, String connectionType) {
        try {
            System.out.printf("📞 Testing %s with basic D-Bus call...%n", connectionType);
            
            OutboundMethodCall call = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("GetId"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
            
            CompletableFuture<InboundMessage> response = connection.sendRequest(call);
            InboundMessage reply = response.get(5, TimeUnit.SECONDS);
            
            System.out.printf("✅ %s call successful! Reply type: %s%n", 
                connectionType, reply.getClass().getSimpleName());
            
        } catch (Exception e) {
            System.out.printf("⚠️  %s call failed: %s%n", connectionType, e.getMessage());
            LOGGER.debug(connectionType + " call error", e);
        }
    }
}