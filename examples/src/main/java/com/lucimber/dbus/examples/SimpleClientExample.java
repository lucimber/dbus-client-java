/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simple client example demonstrating how to interact with D-Bus services.
 * 
 * This example shows how to:
 * 1. Connect to the D-Bus session bus
 * 2. Call standard D-Bus methods (introspection, properties, ping)
 * 3. Handle responses and errors
 */
public class SimpleClientExample {
  
  public static void main(String[] args) {
    Connection connection = null;
    
    try {
      // Create connection configuration
      ConnectionConfig config = ConnectionConfig.builder()
          .withConnectTimeout(Duration.ofSeconds(10))
          .withMethodCallTimeout(Duration.ofSeconds(30))
          .build();
      
      // Connect to session bus
      connection = NettyConnection.newSessionBusConnection(config);
      connection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
      
      System.out.println("🔗 Connected to D-Bus session bus");
      System.out.println();
      
      // Test with the D-Bus daemon itself
      testIntrospection(connection, "org.freedesktop.DBus", "/org/freedesktop/DBus");
      testPing(connection, "org.freedesktop.DBus");
      listServices(connection);
      
      // If SimpleAnnotationExample is running, test it too
      System.out.println("🔍 Testing annotation-based service (if running)...");
      testAnnotationService(connection);
      
    } catch (Exception e) {
      System.err.println("❌ Error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (connection != null) {
        try {
          connection.close();
          System.out.println("🔌 Disconnected from D-Bus");
        } catch (Exception e) {
          System.err.println("Error closing connection: " + e.getMessage());
        }
      }
    }
  }
  
  private static void testIntrospection(Connection connection, String service, String objectPath) {
    try {
      System.out.println("🔍 Testing introspection on " + service + objectPath);
      
      OutboundMethodCall introspectCall = OutboundMethodCall.Builder
          .create()
          .withPath(DBusObjectPath.valueOf(objectPath))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
          .withMember(DBusString.valueOf("Introspect"))
          .withDestination(DBusString.valueOf(service))
          .withReplyExpected(true)
          .build();
      
      InboundMessage response = connection.sendRequest(introspectCall)
          .toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
      
      if (response instanceof InboundMethodReturn methodReturn) {
        List<DBusType> payload = methodReturn.getPayload();
        if (!payload.isEmpty() && payload.get(0) instanceof DBusString xmlData) {
          String xml = xmlData.getDelegate();
          System.out.println("✅ Introspection successful!");
          System.out.println("   XML length: " + xml.length() + " characters");
          // Show first few lines
          String[] lines = xml.split("\n");
          for (int i = 0; i < Math.min(5, lines.length); i++) {
            System.out.println("   " + lines[i]);
          }
          if (lines.length > 5) {
            System.out.println("   ... (" + (lines.length - 5) + " more lines)");
          }
        }
      } else if (response instanceof InboundError error) {
        System.err.println("❌ Introspection failed: " + error.getErrorName().getDelegate());
      }
    } catch (Exception e) {
      System.err.println("❌ Introspection error: " + e.getMessage());
    }
    System.out.println();
  }
  
  private static void testPing(Connection connection, String service) {
    try {
      System.out.println("🏓 Testing ping to " + service);
      
      OutboundMethodCall pingCall = OutboundMethodCall.Builder
          .create()
          .withPath(DBusObjectPath.valueOf("/"))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
          .withMember(DBusString.valueOf("Ping"))
          .withDestination(DBusString.valueOf(service))
          .withReplyExpected(true)
          .build();
      
      long startTime = System.currentTimeMillis();
      InboundMessage response = connection.sendRequest(pingCall)
          .toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
      long responseTime = System.currentTimeMillis() - startTime;
      
      if (response instanceof InboundMethodReturn) {
        System.out.println("✅ Ping successful! Response time: " + responseTime + "ms");
      } else if (response instanceof InboundError error) {
        System.err.println("❌ Ping failed: " + error.getErrorName().getDelegate());
      }
    } catch (Exception e) {
      System.err.println("❌ Ping error: " + e.getMessage());
    }
    System.out.println();
  }
  
  private static void listServices(Connection connection) {
    try {
      System.out.println("📋 Listing available D-Bus services...");
      
      OutboundMethodCall listNamesCall = OutboundMethodCall.Builder
          .create()
          .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
          .withMember(DBusString.valueOf("ListNames"))
          .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
          .withReplyExpected(true)
          .build();
      
      InboundMessage response = connection.sendRequest(listNamesCall)
          .toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
      
      if (response instanceof InboundMethodReturn methodReturn) {
        List<DBusType> payload = methodReturn.getPayload();
        if (!payload.isEmpty() && payload.get(0) instanceof DBusArray) {
          @SuppressWarnings("unchecked")
          DBusArray<DBusString> serviceArray = (DBusArray<DBusString>) payload.get(0);
          
          System.out.println("✅ Found " + serviceArray.size() + " services:");
          int count = 0;
          for (DBusString serviceName : serviceArray) {
            String name = serviceName.getDelegate();
            // Show only well-known names (not unique names starting with :)
            if (!name.startsWith(":")) {
              System.out.println("   • " + name);
              count++;
              if (count >= 10) {  // Limit output
                System.out.println("   ... (and " + (serviceArray.size() - 10) + " more)");
                break;
              }
            }
          }
        }
      } else if (response instanceof InboundError error) {
        System.err.println("❌ Failed to list services: " + error.getErrorName().getDelegate());
      }
    } catch (Exception e) {
      System.err.println("❌ List services error: " + e.getMessage());
    }
    System.out.println();
  }
  
  private static void testAnnotationService(Connection connection) {
    try {
      String service = "com.example.SimpleService";
      String objectPath = "/com/example/Simple";
      
      // Test if the annotation service is available by pinging it
      System.out.println("🏓 Checking if annotation service is running...");
      
      OutboundMethodCall pingCall = OutboundMethodCall.Builder
          .create()
          .withPath(DBusObjectPath.valueOf(objectPath))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
          .withMember(DBusString.valueOf("Ping"))
          .withDestination(DBusString.valueOf(service))
          .withReplyExpected(true)
          .build();
      
      InboundMessage response = connection.sendRequest(pingCall)
          .toCompletableFuture()
          .get(2, TimeUnit.SECONDS);  // Shorter timeout
      
      if (response instanceof InboundMethodReturn) {
        System.out.println("✅ Annotation service is running! Testing properties...");
        testAnnotationServiceProperties(connection, service, objectPath);
      }
      
    } catch (Exception e) {
      System.out.println("ℹ️  Annotation service not available (run SimpleAnnotationExample first)");
    }
  }
  
  private static void testAnnotationServiceProperties(Connection connection, String service, String objectPath) {
    try {
      System.out.println("📊 Getting all properties from annotation service...");
      
      OutboundMethodCall getAllPropsCall = OutboundMethodCall.Builder
          .create()
          .withPath(DBusObjectPath.valueOf(objectPath))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
          .withMember(DBusString.valueOf("GetAll"))
          .withDestination(DBusString.valueOf(service))
          .withBody(DBusSignature.valueOf("s"), Arrays.asList(
              DBusString.valueOf("com.example.Calculator")
          ))
          .withReplyExpected(true)
          .build();
      
      InboundMessage response = connection.sendRequest(getAllPropsCall)
          .toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
      
      if (response instanceof InboundMethodReturn methodReturn) {
        List<DBusType> payload = methodReturn.getPayload();
        if (!payload.isEmpty() && payload.get(0) instanceof DBusDict) {
          @SuppressWarnings("unchecked")
          DBusDict<DBusString, DBusVariant> properties = (DBusDict<DBusString, DBusVariant>) payload.get(0);
          
          System.out.println("✅ Properties retrieved successfully:");
          for (var entry : properties.entrySet()) {
            String propName = entry.getKey().getDelegate();
            DBusVariant variant = entry.getValue();
            DBusType value = variant.getDelegate();
            
            String valueStr = "";
            if (value instanceof DBusString) {
              valueStr = "\"" + ((DBusString) value).getDelegate() + "\"";
            } else {
              valueStr = value.toString();
            }
            
            System.out.println("   • " + propName + " = " + valueStr);
          }
        }
      } else if (response instanceof InboundError error) {
        System.err.println("❌ Failed to get properties: " + error.getErrorName().getDelegate());
      }
      
    } catch (Exception e) {
      System.err.println("❌ Property test error: " + e.getMessage());
    }
  }
}