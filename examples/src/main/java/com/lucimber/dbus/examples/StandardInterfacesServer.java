/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.connection.PipelineContext;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.netty.connection.NettyConnectionConfig;
import com.lucimber.dbus.standard.Introspectable;
import com.lucimber.dbus.standard.ObjectManager;
import com.lucimber.dbus.standard.Peer;
import com.lucimber.dbus.standard.Properties;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.type.TypeCode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Example showing how to implement standard D-Bus interfaces in a service.
 */
public class StandardInterfacesServer {
  
  private static final String SERVICE_NAME = "com.example.StandardInterfacesDemo";
  private static final String OBJECT_PATH = "/com/example/Demo";
  
  // Storage for properties
  private static final Map<String, Map<String, DBusVariant>> properties = new ConcurrentHashMap<>();
  
  // Storage for managed objects (for ObjectManager)
  private static final Map<DBusObjectPath, Map<String, Map<String, DBusVariant>>> managedObjects = new ConcurrentHashMap<>();
  
  public static void main(String[] args) throws Exception {
    // Initialize properties
    initializeProperties();
    
    // Create connection
    SocketAddress socketAddress = new DomainSocketAddress(
        System.getenv("DBUS_SESSION_BUS_ADDRESS").replace("unix:path=", "")
    );
    
    NettyConnectionConfig config = NettyConnectionConfig.builder()
        .withEventLoopGroup(new NioEventLoopGroup(1))
        .build();
    
    Connection connection = NettyConnection.newConnection(socketAddress, config);
    connection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);
    
    try {
      // Request service name
      requestServiceName(connection, SERVICE_NAME);
      
      // Add method call handler to pipeline
      Pipeline pipeline = connection.getPipeline();
      pipeline.addLast("method-handler", new StandardInterfacesHandler());
      
      System.out.println("Standard interfaces server started at " + SERVICE_NAME);
      System.out.println("Object path: " + OBJECT_PATH);
      System.out.println("Press Ctrl+C to stop...");
      
      // Keep running
      Thread.sleep(Long.MAX_VALUE);
      
    } finally {
      connection.disconnect().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }
  
  private static void initializeProperties() {
    // Initialize demo properties
    Map<String, DBusVariant> demoProps = new HashMap<>();
    demoProps.put("Version", DBusVariant.valueOf(DBusString.valueOf("1.0.0")));
    demoProps.put("Author", DBusVariant.valueOf(DBusString.valueOf("Example Author")));
    demoProps.put("Count", DBusVariant.valueOf(DBusUInt32.valueOf(42)));
    properties.put("com.example.Demo", demoProps);
    
    // Initialize managed objects
    Map<String, Map<String, DBusVariant>> interfaces = new HashMap<>();
    interfaces.put("com.example.Demo", new HashMap<>(demoProps));
    managedObjects.put(DBusObjectPath.valueOf(OBJECT_PATH), interfaces);
    
    // Add a child object
    Map<String, DBusVariant> childProps = new HashMap<>();
    childProps.put("Name", DBusVariant.valueOf(DBusString.valueOf("Child Object")));
    childProps.put("Active", DBusVariant.valueOf(DBusUInt32.valueOf(1))); // Using UInt32 for boolean
    Map<String, Map<String, DBusVariant>> childInterfaces = new HashMap<>();
    childInterfaces.put("com.example.Child", childProps);
    managedObjects.put(DBusObjectPath.valueOf(OBJECT_PATH + "/Child"), childInterfaces);
  }
  
  private static void requestServiceName(Connection connection, String serviceName) throws Exception {
    OutboundMethodCall requestName = OutboundMethodCall.Builder.create()
        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
        .withMember(DBusString.valueOf("RequestName"))
        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
        .withBody(Arrays.asList(
            DBusString.valueOf(serviceName),
            DBusUInt32.valueOf(0) // No flags
        ))
        .withReplyExpected(true)
        .build();
    
    connection.sendRequest(requestName).get(5, TimeUnit.SECONDS);
  }
  
  /**
   * Handler for standard interface method calls.
   */
  static class StandardInterfacesHandler implements com.lucimber.dbus.connection.InboundHandler {
    
    @Override
    public void handleInboundMessage(PipelineContext ctx, InboundMessage msg) {
      if (msg instanceof InboundMethodCall) {
        InboundMethodCall call = (InboundMethodCall) msg;
        
        // Check if this call is for our object
        if (!OBJECT_PATH.equals(call.getPath().toString()) && 
            !call.getPath().toString().startsWith(OBJECT_PATH + "/")) {
          ctx.propagateInboundMessage(msg);
          return;
        }
        
        String interfaceName = call.getInterfaceName()
            .map(DBusString::toString)
            .orElse("");
        String memberName = call.getMember().toString();
        
        try {
          switch (interfaceName) {
            case "org.freedesktop.DBus.Introspectable":
              handleIntrospectable(ctx, call, memberName);
              break;
            case "org.freedesktop.DBus.Properties":
              handleProperties(ctx, call, memberName);
              break;
            case "org.freedesktop.DBus.Peer":
              handlePeer(ctx, call, memberName);
              break;
            case "org.freedesktop.DBus.ObjectManager":
              handleObjectManager(ctx, call, memberName);
              break;
            default:
              // Not handled by this handler
              ctx.propagateInboundMessage(msg);
          }
        } catch (Exception e) {
          sendError(ctx, call, "org.freedesktop.DBus.Error.Failed", e.getMessage());
        }
      } else {
        ctx.propagateInboundMessage(msg);
      }
    }
    
    private void handleIntrospectable(PipelineContext ctx, InboundMethodCall call, String memberName) {
      if ("Introspect".equals(memberName)) {
        String xml = generateIntrospectionXml(call.getPath().toString());
        
        OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
            .withReplySerial(call.getSerial())
            .withBody(Arrays.asList(DBusString.valueOf(xml)))
            .build();
        
        ctx.sendOutboundMessage(reply);
      } else {
        sendError(ctx, call, "org.freedesktop.DBus.Error.UnknownMethod", 
                  "Unknown method: " + memberName);
      }
    }
    
    private void handleProperties(PipelineContext ctx, InboundMethodCall call, String memberName) {
      List<DBusType> args = call.getBody();
      
      switch (memberName) {
        case "Get":
          if (args.size() >= 2) {
            String iface = ((DBusString) args.get(0)).toString();
            String prop = ((DBusString) args.get(1)).toString();
            
            Map<String, DBusVariant> ifaceProps = properties.get(iface);
            if (ifaceProps != null && ifaceProps.containsKey(prop)) {
              DBusVariant value = ifaceProps.get(prop);
              
              OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
                  .withReplySerial(call.getSerial())
                  .withBody(Arrays.asList(value))
                  .build();
              
              ctx.sendOutboundMessage(reply);
            } else {
              sendError(ctx, call, "org.freedesktop.DBus.Error.UnknownProperty",
                        "Unknown property: " + prop);
            }
          }
          break;
          
        case "Set":
          if (args.size() >= 3) {
            String iface = ((DBusString) args.get(0)).toString();
            String prop = ((DBusString) args.get(1)).toString();
            DBusVariant value = (DBusVariant) args.get(2);
            
            Map<String, DBusVariant> ifaceProps = properties.computeIfAbsent(iface, k -> new ConcurrentHashMap<>());
            DBusVariant oldValue = ifaceProps.put(prop, value);
            
            // Send reply
            OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
                .withReplySerial(call.getSerial())
                .build();
            
            ctx.sendOutboundMessage(reply);
            
            // Send PropertiesChanged signal if value changed
            if (!value.equals(oldValue)) {
              sendPropertiesChangedSignal(ctx, call.getPath(), iface, prop, value);
            }
          }
          break;
          
        case "GetAll":
          if (!args.isEmpty()) {
            String iface = ((DBusString) args.get(0)).toString();
            Map<String, DBusVariant> ifaceProps = properties.getOrDefault(iface, new HashMap<>());
            
            DBusDict<DBusString, DBusVariant> propsDict = new DBusDict<>(
                TypeCode.STRING, TypeCode.VARIANT);
            ifaceProps.forEach((k, v) -> propsDict.put(DBusString.valueOf(k), v));
            
            OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
                .withReplySerial(call.getSerial())
                .withBody(Arrays.asList(propsDict))
                .build();
            
            ctx.sendOutboundMessage(reply);
          }
          break;
          
        default:
          sendError(ctx, call, "org.freedesktop.DBus.Error.UnknownMethod",
                    "Unknown method: " + memberName);
      }
    }
    
    private void handlePeer(PipelineContext ctx, InboundMethodCall call, String memberName) {
      switch (memberName) {
        case "Ping":
          // Simply return empty response
          OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
              .withReplySerial(call.getSerial())
              .build();
          
          ctx.sendOutboundMessage(reply);
          break;
          
        case "GetMachineId":
          String machineId = getMachineId();
          
          OutboundMethodReturn idReply = OutboundMethodReturn.Builder.create()
              .withReplySerial(call.getSerial())
              .withBody(Arrays.asList(DBusString.valueOf(machineId)))
              .build();
          
          ctx.sendOutboundMessage(idReply);
          break;
          
        default:
          sendError(ctx, call, "org.freedesktop.DBus.Error.UnknownMethod",
                    "Unknown method: " + memberName);
      }
    }
    
    private void handleObjectManager(PipelineContext ctx, InboundMethodCall call, String memberName) {
      if ("GetManagedObjects".equals(memberName)) {
        // Create the complex nested dictionary structure
        DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>> result =
            new DBusDict<>(TypeCode.OBJECT_PATH, TypeCode.ARRAY);
        
        managedObjects.forEach((path, interfaces) -> {
          DBusDict<DBusString, DBusDict<DBusString, DBusVariant>> ifacesDict =
              new DBusDict<>(TypeCode.STRING, TypeCode.ARRAY);
          
          interfaces.forEach((iface, props) -> {
            DBusDict<DBusString, DBusVariant> propsDict =
                new DBusDict<>(TypeCode.STRING, TypeCode.VARIANT);
            
            props.forEach((propName, propValue) -> {
              propsDict.put(DBusString.valueOf(propName), propValue);
            });
            
            ifacesDict.put(DBusString.valueOf(iface), propsDict);
          });
          
          result.put(path, ifacesDict);
        });
        
        OutboundMethodReturn reply = OutboundMethodReturn.Builder.create()
            .withReplySerial(call.getSerial())
            .withBody(Arrays.asList(result))
            .build();
        
        ctx.sendOutboundMessage(reply);
      } else {
        sendError(ctx, call, "org.freedesktop.DBus.Error.UnknownMethod",
                  "Unknown method: " + memberName);
      }
    }
    
    private String generateIntrospectionXml(String objectPath) {
      StringBuilder xml = new StringBuilder();
      xml.append("<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n");
      xml.append("\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n");
      xml.append("<node>\n");
      
      // Add standard interfaces
      xml.append("  <interface name=\"org.freedesktop.DBus.Introspectable\">\n");
      xml.append("    <method name=\"Introspect\">\n");
      xml.append("      <arg name=\"xml_data\" type=\"s\" direction=\"out\"/>\n");
      xml.append("    </method>\n");
      xml.append("  </interface>\n");
      
      xml.append("  <interface name=\"org.freedesktop.DBus.Properties\">\n");
      xml.append("    <method name=\"Get\">\n");
      xml.append("      <arg name=\"interface_name\" type=\"s\" direction=\"in\"/>\n");
      xml.append("      <arg name=\"property_name\" type=\"s\" direction=\"in\"/>\n");
      xml.append("      <arg name=\"value\" type=\"v\" direction=\"out\"/>\n");
      xml.append("    </method>\n");
      xml.append("    <method name=\"Set\">\n");
      xml.append("      <arg name=\"interface_name\" type=\"s\" direction=\"in\"/>\n");
      xml.append("      <arg name=\"property_name\" type=\"s\" direction=\"in\"/>\n");
      xml.append("      <arg name=\"value\" type=\"v\" direction=\"in\"/>\n");
      xml.append("    </method>\n");
      xml.append("    <method name=\"GetAll\">\n");
      xml.append("      <arg name=\"interface_name\" type=\"s\" direction=\"in\"/>\n");
      xml.append("      <arg name=\"properties\" type=\"a{sv}\" direction=\"out\"/>\n");
      xml.append("    </method>\n");
      xml.append("    <signal name=\"PropertiesChanged\">\n");
      xml.append("      <arg name=\"interface_name\" type=\"s\"/>\n");
      xml.append("      <arg name=\"changed_properties\" type=\"a{sv}\"/>\n");
      xml.append("      <arg name=\"invalidated_properties\" type=\"as\"/>\n");
      xml.append("    </signal>\n");
      xml.append("  </interface>\n");
      
      xml.append("  <interface name=\"org.freedesktop.DBus.Peer\">\n");
      xml.append("    <method name=\"Ping\"/>\n");
      xml.append("    <method name=\"GetMachineId\">\n");
      xml.append("      <arg name=\"machine_uuid\" type=\"s\" direction=\"out\"/>\n");
      xml.append("    </method>\n");
      xml.append("  </interface>\n");
      
      if (OBJECT_PATH.equals(objectPath)) {
        xml.append("  <interface name=\"org.freedesktop.DBus.ObjectManager\">\n");
        xml.append("    <method name=\"GetManagedObjects\">\n");
        xml.append("      <arg name=\"object_paths_interfaces_and_properties\" type=\"a{oa{sa{sv}}}\" direction=\"out\"/>\n");
        xml.append("    </method>\n");
        xml.append("  </interface>\n");
      }
      
      // Add custom interface
      xml.append("  <interface name=\"com.example.Demo\">\n");
      xml.append("    <property name=\"Version\" type=\"s\" access=\"read\"/>\n");
      xml.append("    <property name=\"Author\" type=\"s\" access=\"readwrite\"/>\n");
      xml.append("    <property name=\"Count\" type=\"u\" access=\"readwrite\"/>\n");
      xml.append("  </interface>\n");
      
      // Add child nodes if this is the root
      if (OBJECT_PATH.equals(objectPath)) {
        xml.append("  <node name=\"Child\"/>\n");
      }
      
      xml.append("</node>\n");
      return xml.toString();
    }
    
    private String getMachineId() {
      try {
        // Try to read from standard locations
        if (Files.exists(Paths.get("/etc/machine-id"))) {
          return Files.readString(Paths.get("/etc/machine-id")).trim();
        } else if (Files.exists(Paths.get("/var/lib/dbus/machine-id"))) {
          return Files.readString(Paths.get("/var/lib/dbus/machine-id")).trim();
        }
      } catch (Exception e) {
        // Ignore
      }
      // Return a dummy ID if not found
      return "0123456789abcdef0123456789abcdef";
    }
    
    private void sendPropertiesChangedSignal(PipelineContext ctx, DBusObjectPath path,
                                             String interfaceName, String propertyName,
                                             DBusVariant newValue) {
      DBusDict<DBusString, DBusVariant> changedProps = new DBusDict<>(TypeCode.STRING, TypeCode.VARIANT);
      changedProps.put(DBusString.valueOf(propertyName), newValue);
      
      DBusArray<DBusString> invalidatedProps = new DBusArray<>(TypeCode.STRING);
      
      OutboundSignal signal = OutboundSignal.Builder.create()
          .withPath(path)
          .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
          .withMember(DBusString.valueOf("PropertiesChanged"))
          .withBody(Arrays.asList(
              DBusString.valueOf(interfaceName),
              changedProps,
              invalidatedProps
          ))
          .build();
      
      ctx.sendOutboundMessage(signal);
    }
    
    private void sendError(PipelineContext ctx, InboundMethodCall call, 
                          String errorName, String errorMessage) {
      OutboundError error = OutboundError.Builder.create()
          .withReplySerial(call.getSerial())
          .withErrorName(DBusString.valueOf(errorName))
          .withBody(Arrays.asList(DBusString.valueOf(errorMessage)))
          .build();
      
      ctx.sendOutboundMessage(error);
    }
  }
}