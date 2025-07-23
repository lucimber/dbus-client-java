/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.annotation;

import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that implements standard D-Bus interfaces using reflection and annotations.
 *
 * <p>This handler automatically provides implementations for:
 *
 * <ul>
 *   <li>org.freedesktop.DBus.Introspectable - generates XML from annotations
 *   <li>org.freedesktop.DBus.Properties - accesses annotated properties
 *   <li>org.freedesktop.DBus.Peer - standard implementation
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @DBusInterface("com.example.MyService")
 * public class MyService {
 *     @DBusProperty
 *     private String version = "1.0.0";
 *
 *     @DBusMethod
 *     public String echo(String message) {
 *         return message;
 *     }
 * }
 *
 * // Register the handler
 * MyService service = new MyService();
 * StandardInterfaceHandler handler = new StandardInterfaceHandler(
 *     "/com/example/MyService", service);
 * connection.getPipeline().addLast("standard", handler);
 * }</pre>
 */
public class StandardInterfaceHandler extends AbstractInboundHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardInterfaceHandler.class);

    private final String objectPath;
    private final Object targetObject;
    private final Map<String, Map<String, Field>> properties = new ConcurrentHashMap<>();
    private final AtomicInteger serialCounter = new AtomicInteger(1);
    private String interfaceName;

    /**
     * Creates a handler for the given object at the specified path.
     *
     * @param objectPath the D-Bus object path
     * @param targetObject the object implementing the interfaces
     */
    public StandardInterfaceHandler(String objectPath, Object targetObject) {
        this.objectPath = objectPath;
        this.targetObject = targetObject;

        // Scan annotations
        scanAnnotations(targetObject.getClass());
    }

    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        if (!(msg instanceof InboundMethodCall)) {
            ctx.propagateInboundMessage(msg);
            return;
        }

        InboundMethodCall call = (InboundMethodCall) msg;

        // Check if this call is for our object
        if (!objectPath.equals(call.getObjectPath().toString())) {
            ctx.propagateInboundMessage(msg);
            return;
        }

        String interfaceName = call.getInterfaceName().map(DBusString::toString).orElse("");
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
                default:
                    // Not handled by this handler
                    ctx.propagateInboundMessage(msg);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling method call", e);
            sendError(ctx, call, "org.freedesktop.DBus.Error.Failed", e.getMessage());
        }
    }

    private void handleIntrospectable(Context ctx, InboundMethodCall call, String memberName) {
        if ("Introspect".equals(memberName)) {
            String xml = generateIntrospectionXml();

            OutboundMethodReturn reply =
                    OutboundMethodReturn.Builder.create()
                            .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                            .withReplySerial(call.getSerial())
                            .withBody(
                                    DBusSignature.valueOf("s"),
                                    Arrays.asList(DBusString.valueOf(xml)))
                            .build();

            sendReply(ctx, reply);
        } else {
            sendError(
                    ctx,
                    call,
                    "org.freedesktop.DBus.Error.UnknownMethod",
                    "Unknown method: " + memberName);
        }
    }

    private void handleProperties(Context ctx, InboundMethodCall call, String memberName) {
        List<DBusType> args = call.getPayload() != null ? call.getPayload() : Arrays.asList();

        switch (memberName) {
            case "Get":
                if (args.size() >= 2) {
                    String iface = ((DBusString) args.get(0)).toString();
                    String prop = ((DBusString) args.get(1)).toString();

                    try {
                        DBusVariant value = getPropertyValue(iface, prop);

                        OutboundMethodReturn reply =
                                OutboundMethodReturn.Builder.create()
                                        .withSerial(
                                                DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                                        .withReplySerial(call.getSerial())
                                        .withBody(DBusSignature.valueOf("v"), Arrays.asList(value))
                                        .build();

                        sendReply(ctx, reply);
                    } catch (Exception e) {
                        sendError(
                                ctx,
                                call,
                                "org.freedesktop.DBus.Error.UnknownProperty",
                                "Unknown property: " + prop);
                    }
                } else {
                    sendError(
                            ctx,
                            call,
                            "org.freedesktop.DBus.Error.InvalidArgs",
                            "Get requires interface and property name");
                }
                break;

            case "GetAll":
                if (!args.isEmpty()) {
                    String iface = ((DBusString) args.get(0)).toString();

                    try {
                        Map<String, DBusVariant> allProps = getAllProperties(iface);
                        DBusDict<DBusString, DBusVariant> result =
                                new DBusDict<>(DBusSignature.valueOf("a{sv}"));

                        for (Map.Entry<String, DBusVariant> entry : allProps.entrySet()) {
                            result.put(DBusString.valueOf(entry.getKey()), entry.getValue());
                        }

                        OutboundMethodReturn reply =
                                OutboundMethodReturn.Builder.create()
                                        .withSerial(
                                                DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                                        .withReplySerial(call.getSerial())
                                        .withBody(
                                                DBusSignature.valueOf("a{sv}"),
                                                Arrays.asList(result))
                                        .build();

                        sendReply(ctx, reply);
                    } catch (Exception e) {
                        sendError(
                                ctx,
                                call,
                                "org.freedesktop.DBus.Error.Failed",
                                "Failed to get properties: " + e.getMessage());
                    }
                } else {
                    sendError(
                            ctx,
                            call,
                            "org.freedesktop.DBus.Error.InvalidArgs",
                            "GetAll requires interface name");
                }
                break;

            default:
                sendError(
                        ctx,
                        call,
                        "org.freedesktop.DBus.Error.UnknownMethod",
                        "Unknown method: " + memberName);
        }
    }

    private void handlePeer(Context ctx, InboundMethodCall call, String memberName) {
        switch (memberName) {
            case "Ping":
                OutboundMethodReturn reply =
                        OutboundMethodReturn.Builder.create()
                                .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                                .withReplySerial(call.getSerial())
                                .build();

                sendReply(ctx, reply);
                break;

            case "GetMachineId":
                String machineId = getMachineId();

                OutboundMethodReturn idReply =
                        OutboundMethodReturn.Builder.create()
                                .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                                .withReplySerial(call.getSerial())
                                .withBody(
                                        DBusSignature.valueOf("s"),
                                        Arrays.asList(DBusString.valueOf(machineId)))
                                .build();

                sendReply(ctx, idReply);
                break;

            default:
                sendError(
                        ctx,
                        call,
                        "org.freedesktop.DBus.Error.UnknownMethod",
                        "Unknown method: " + memberName);
        }
    }

    private void scanAnnotations(Class<?> clazz) {
        DBusInterface ifaceAnnotation = clazz.getAnnotation(DBusInterface.class);
        if (ifaceAnnotation != null) {
            this.interfaceName = ifaceAnnotation.value();

            Map<String, Field> ifaceProps = new HashMap<>();
            properties.put(this.interfaceName, ifaceProps);

            // Scan fields for properties
            for (Field field : clazz.getDeclaredFields()) {
                DBusProperty propAnnotation = field.getAnnotation(DBusProperty.class);
                if (propAnnotation != null) {
                    String propName =
                            propAnnotation.name().isEmpty()
                                    ? field.getName()
                                    : propAnnotation.name();
                    field.setAccessible(true);
                    ifaceProps.put(propName, field);
                }
            }
        }
    }

    private DBusVariant getPropertyValue(String iface, String prop) throws Exception {
        Map<String, Field> ifaceProps = properties.get(iface);
        if (ifaceProps == null) {
            throw new IllegalArgumentException("Unknown interface: " + iface);
        }

        Field field = ifaceProps.get(prop);
        if (field == null) {
            throw new IllegalArgumentException("Unknown property: " + prop);
        }

        Object value = field.get(targetObject);

        // Simple type conversion - extend as needed
        if (value instanceof String) {
            return DBusVariant.valueOf(DBusString.valueOf((String) value));
        } else if (value instanceof Integer) {
            return DBusVariant.valueOf(com.lucimber.dbus.type.DBusInt32.valueOf((Integer) value));
        } else if (value instanceof Boolean) {
            return DBusVariant.valueOf(com.lucimber.dbus.type.DBusBoolean.valueOf((Boolean) value));
        } else if (value != null) {
            // Convert to string as fallback
            return DBusVariant.valueOf(DBusString.valueOf(value.toString()));
        }

        throw new UnsupportedOperationException(
                "Type conversion not implemented for: "
                        + (value != null ? value.getClass() : "null"));
    }

    private Map<String, DBusVariant> getAllProperties(String iface) throws Exception {
        Map<String, Field> ifaceProps = properties.get(iface);
        if (ifaceProps == null) {
            return new HashMap<>();
        }

        Map<String, DBusVariant> result = new HashMap<>();
        for (Map.Entry<String, Field> entry : ifaceProps.entrySet()) {
            try {
                result.put(entry.getKey(), getPropertyValue(iface, entry.getKey()));
            } catch (Exception e) {
                // Skip properties that fail to read
                LOGGER.debug("Failed to read property: " + entry.getKey(), e);
            }
        }

        return result;
    }

    private String generateIntrospectionXml() {
        StringBuilder xml = new StringBuilder();
        xml.append(
                "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n");
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
        xml.append("    <method name=\"GetAll\">\n");
        xml.append("      <arg name=\"interface_name\" type=\"s\" direction=\"in\"/>\n");
        xml.append("      <arg name=\"properties\" type=\"a{sv}\" direction=\"out\"/>\n");
        xml.append("    </method>\n");
        xml.append("  </interface>\n");

        xml.append("  <interface name=\"org.freedesktop.DBus.Peer\">\n");
        xml.append("    <method name=\"Ping\"/>\n");
        xml.append("    <method name=\"GetMachineId\">\n");
        xml.append("      <arg name=\"machine_uuid\" type=\"s\" direction=\"out\"/>\n");
        xml.append("    </method>\n");
        xml.append("  </interface>\n");

        // Add custom interface if available
        if (interfaceName != null) {
            xml.append("  <interface name=\"").append(interfaceName).append("\">\n");

            // Add properties
            Map<String, Field> ifaceProps = properties.get(interfaceName);
            if (ifaceProps != null) {
                for (String propName : ifaceProps.keySet()) {
                    xml.append("    <property name=\"")
                            .append(propName)
                            .append("\" type=\"v\" access=\"read\"/>\n");
                }
            }

            xml.append("  </interface>\n");
        }

        xml.append("</node>\n");
        return xml.toString();
    }

    private String getMachineId() {
        try {
            if (Files.exists(Paths.get("/etc/machine-id"))) {
                return Files.readString(Paths.get("/etc/machine-id")).trim();
            } else if (Files.exists(Paths.get("/var/lib/dbus/machine-id"))) {
                return Files.readString(Paths.get("/var/lib/dbus/machine-id")).trim();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "0123456789abcdef0123456789abcdef";
    }

    private void sendReply(Context ctx, OutboundMethodReturn reply) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.propagateOutboundMessage(reply, future);
    }

    private void sendError(
            Context ctx, InboundMethodCall call, String errorName, String errorMessage) {
        OutboundError error =
                OutboundError.Builder.create()
                        .withSerial(DBusUInt32.valueOf(serialCounter.getAndIncrement()))
                        .withReplySerial(call.getSerial())
                        .withErrorName(DBusString.valueOf(errorName))
                        .withBody(
                                DBusSignature.valueOf("s"),
                                Arrays.asList(DBusString.valueOf(errorMessage)))
                        .build();

        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.propagateOutboundMessage(error, future);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
