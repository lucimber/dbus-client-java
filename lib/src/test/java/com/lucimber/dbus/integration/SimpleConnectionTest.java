package com.lucimber.dbus.integration;

import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.netty.NettyConnection;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple connection test to debug D-Bus connection issues
 */
public class SimpleConnectionTest {
    public static void main(String[] args) {
        System.out.println("=== Simple D-Bus Connection Test ===");
        
        // Enable detailed debugging
        System.setProperty("org.slf4j.simpleLogger.log.com.lucimber.dbus", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.io.netty", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.root", "debug");
        
        try {
            // Create configuration with shorter timeout for faster feedback
            ConnectionConfig config = ConnectionConfig.builder()
                .withConnectTimeout(Duration.ofSeconds(10))
                .build();
            
            System.out.println("Creating NettyConnection...");
            NettyConnection connection = new NettyConnection(
                new InetSocketAddress("127.0.0.1", 12345),
                config
            );
            
            System.out.println("Starting connection...");
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();
            
            try {
                connectFuture.get(15, TimeUnit.SECONDS);
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ Connection successful after " + duration + "ms");
                System.out.println("Connection state: " + connection.getState());
                System.out.println("Is connected: " + connection.isConnected());
                
                // Clean up
                connection.close();
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("❌ Connection failed after " + duration + "ms");
                System.out.println("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (e.getCause() != null) {
                    System.out.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
                }
                
                // Print stack trace for debugging
                System.out.println("\nFull stack trace:");
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.out.println("❌ Failed to create connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}