/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.netty.connection.NettyConnectionConfig;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Example demonstrating how to use standard D-Bus interfaces.
 */
public class StandardInterfacesExample {

  public static void main(String[] args) throws Exception {
    // Create connection to session bus
    SocketAddress socketAddress = new DomainSocketAddress(
        System.getenv("DBUS_SESSION_BUS_ADDRESS").replace("unix:path=", "")
    );
    
    NettyConnectionConfig config = NettyConnectionConfig.builder()
        .withEventLoopGroup(new NioEventLoopGroup(1))
        .build();
    
    Connection connection = NettyConnection.newConnection(socketAddress, config);
    connection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);

    try {
      // Example 1: Introspection
      System.out.println("=== Introspection Example ===");
      introspectObject(connection, "org.freedesktop.DBus", "/org/freedesktop/DBus");
      
      // Example 2: Properties
      System.out.println("\n=== Properties Example ===");
      // Get a property from NetworkManager (if available)
      getProperty(connection, "org.freedesktop.NetworkManager", 
                  "/org/freedesktop/NetworkManager",
                  "org.freedesktop.NetworkManager", 
                  "Version");
      
      // Example 3: Peer
      System.out.println("\n=== Peer Example ===");
      pingPeer(connection, "org.freedesktop.DBus");
      getMachineId(connection, "org.freedesktop.DBus");
      
      // Example 4: ObjectManager
      System.out.println("\n=== ObjectManager Example ===");
      // Get managed objects from NetworkManager (if available)
      getManagedObjects(connection, "org.freedesktop.NetworkManager", 
                        "/org/freedesktop");
      
      // Example 5: List available services
      System.out.println("\n=== List Services Example ===");
      listAvailableServices(connection);
      
    } finally {
      connection.disconnect().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }
  
  /**
   * Introspect a D-Bus object to discover its interfaces.
   */
  private static void introspectObject(Connection connection, String service, String objectPath) 
      throws Exception {
    OutboundMethodCall introspectCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf(objectPath))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
        .withMember(DBusString.valueOf("Introspect"))
        .withDestination(DBusString.valueOf(service))
        .withReplyExpected(true)
        .build();
    
    CompletableFuture<InboundMessage> future = connection.sendRequest(introspectCall);
    InboundMessage response = future.get(5, TimeUnit.SECONDS);
    
    if (response instanceof InboundMethodReturn) {
      InboundMethodReturn returnMsg = (InboundMethodReturn) response;
      List<DBusType> body = returnMsg.getBody();
      if (!body.isEmpty() && body.get(0) instanceof DBusString) {
        DBusString xmlData = (DBusString) body.get(0);
        System.out.println("Introspection data for " + objectPath + ":");
        // Print first 500 chars of XML
        String xml = xmlData.toString();
        System.out.println(xml.substring(0, Math.min(xml.length(), 500)) + "...");
      }
    } else if (response instanceof InboundError) {
      InboundError error = (InboundError) response;
      System.err.println("Introspection failed: " + error.getErrorName());
    }
  }
  
  /**
   * Get a property value using the Properties interface.
   */
  private static void getProperty(Connection connection, String service, String objectPath,
                                  String interfaceName, String propertyName) throws Exception {
    OutboundMethodCall getPropertyCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf(objectPath))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
        .withMember(DBusString.valueOf("Get"))
        .withDestination(DBusString.valueOf(service))
        .withBody(Arrays.asList(
            DBusString.valueOf(interfaceName),
            DBusString.valueOf(propertyName)
        ))
        .withReplyExpected(true)
        .build();
    
    try {
      CompletableFuture<InboundMessage> future = connection.sendRequest(getPropertyCall);
      InboundMessage response = future.get(5, TimeUnit.SECONDS);
      
      if (response instanceof InboundMethodReturn) {
        InboundMethodReturn returnMsg = (InboundMethodReturn) response;
        List<DBusType> body = returnMsg.getBody();
        if (!body.isEmpty() && body.get(0) instanceof DBusVariant) {
          DBusVariant variant = (DBusVariant) body.get(0);
          System.out.println("Property " + propertyName + " = " + variant.getValue());
        }
      } else if (response instanceof InboundError) {
        InboundError error = (InboundError) response;
        System.err.println("Get property failed: " + error.getErrorName());
      }
    } catch (Exception e) {
      System.err.println("Property access failed (service might not be available): " + e.getMessage());
    }
  }
  
  /**
   * Ping a D-Bus peer to test connectivity.
   */
  private static void pingPeer(Connection connection, String service) throws Exception {
    OutboundMethodCall pingCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf("/"))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
        .withMember(DBusString.valueOf("Ping"))
        .withDestination(DBusString.valueOf(service))
        .withReplyExpected(true)
        .build();
    
    CompletableFuture<InboundMessage> future = connection.sendRequest(pingCall);
    InboundMessage response = future.get(5, TimeUnit.SECONDS);
    
    if (response instanceof InboundMethodReturn) {
      System.out.println("Ping successful for " + service);
    } else if (response instanceof InboundError) {
      InboundError error = (InboundError) response;
      System.err.println("Ping failed: " + error.getErrorName());
    }
  }
  
  /**
   * Get the machine ID from a D-Bus peer.
   */
  private static void getMachineId(Connection connection, String service) throws Exception {
    OutboundMethodCall getMachineIdCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf("/"))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
        .withMember(DBusString.valueOf("GetMachineId"))
        .withDestination(DBusString.valueOf(service))
        .withReplyExpected(true)
        .build();
    
    CompletableFuture<InboundMessage> future = connection.sendRequest(getMachineIdCall);
    InboundMessage response = future.get(5, TimeUnit.SECONDS);
    
    if (response instanceof InboundMethodReturn) {
      InboundMethodReturn returnMsg = (InboundMethodReturn) response;
      List<DBusType> body = returnMsg.getBody();
      if (!body.isEmpty() && body.get(0) instanceof DBusString) {
        DBusString machineId = (DBusString) body.get(0);
        System.out.println("Machine ID: " + machineId);
      }
    } else if (response instanceof InboundError) {
      InboundError error = (InboundError) response;
      System.err.println("GetMachineId failed: " + error.getErrorName());
    }
  }
  
  /**
   * Get managed objects using the ObjectManager interface.
   */
  @SuppressWarnings("unchecked")
  private static void getManagedObjects(Connection connection, String service, String objectPath) 
      throws Exception {
    OutboundMethodCall getManagedObjectsCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf(objectPath))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus.ObjectManager"))
        .withMember(DBusString.valueOf("GetManagedObjects"))
        .withDestination(DBusString.valueOf(service))
        .withReplyExpected(true)
        .build();
    
    try {
      CompletableFuture<InboundMessage> future = connection.sendRequest(getManagedObjectsCall);
      InboundMessage response = future.get(5, TimeUnit.SECONDS);
      
      if (response instanceof InboundMethodReturn) {
        InboundMethodReturn returnMsg = (InboundMethodReturn) response;
        List<DBusType> body = returnMsg.getBody();
        if (!body.isEmpty() && body.get(0) instanceof DBusDict) {
          DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>> 
              managedObjects = (DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>>) body.get(0);
          
          System.out.println("Managed objects under " + objectPath + ":");
          managedObjects.forEach((path, interfaces) -> {
            System.out.println("  Object: " + path);
            interfaces.forEach((iface, properties) -> {
              System.out.println("    Interface: " + iface);
              properties.forEach((prop, value) -> {
                System.out.println("      " + prop + " = " + value.getValue());
              });
            });
          });
        }
      } else if (response instanceof InboundError) {
        InboundError error = (InboundError) response;
        System.err.println("GetManagedObjects failed: " + error.getErrorName());
      }
    } catch (Exception e) {
      System.err.println("ObjectManager access failed (interface might not be supported): " + e.getMessage());
    }
  }
  
  /**
   * List all available D-Bus services.
   */
  @SuppressWarnings("unchecked")
  private static void listAvailableServices(Connection connection) throws Exception {
    OutboundMethodCall listNamesCall = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
        .withMember(DBusString.valueOf("ListNames"))
        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
        .withReplyExpected(true)
        .build();
    
    CompletableFuture<InboundMessage> future = connection.sendRequest(listNamesCall);
    InboundMessage response = future.get(5, TimeUnit.SECONDS);
    
    if (response instanceof InboundMethodReturn) {
      InboundMethodReturn returnMsg = (InboundMethodReturn) response;
      List<DBusType> body = returnMsg.getBody();
      if (!body.isEmpty() && body.get(0) instanceof DBusArray) {
        DBusArray<DBusString> names = (DBusArray<DBusString>) body.get(0);
        System.out.println("Available D-Bus services:");
        names.stream()
            .map(DBusString::toString)
            .filter(name -> !name.startsWith(":"))  // Filter out unique names
            .sorted()
            .forEach(name -> System.out.println("  - " + name));
      }
    }
  }
}