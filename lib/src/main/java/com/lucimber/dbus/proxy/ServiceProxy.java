/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.proxy;

import com.lucimber.dbus.annotation.DBusInterface;
import com.lucimber.dbus.annotation.DBusMethod;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating dynamic proxies for D-Bus service clients.
 * <p>
 * This class simplifies D-Bus method calls by allowing you to define a Java interface
 * with annotations and automatically handling the underlying D-Bus communication.
 * 
 * <p><strong>Note:</strong> ServiceProxy is designed for simple client-side request/response 
 * scenarios only. It does not support:
 * <ul>
 *   <li>Receiving D-Bus signals
 *   <li>Implementing D-Bus services (use {@link com.lucimber.dbus.annotation.StandardInterfaceHandler} instead)
 *   <li>Complex argument marshalling (currently limited to methods without arguments)
 * </ul>
 * 
 * <p><strong>Relationship to StandardInterfaceHandler:</strong>
 * <ul>
 *   <li>{@code ServiceProxy} - Client-side proxy for calling remote D-Bus services
 *   <li>{@code StandardInterfaceHandler} - Server-side handler for implementing D-Bus services
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * @DBusInterface("org.freedesktop.DBus")
 * public interface DBusService {
 *     @DBusMethod("ListNames")
 *     CompletableFuture<String[]> listNames();
 *     
 *     @DBusMethod("GetId")
 *     String getId();
 * }
 * 
 * // Create proxy
 * DBusService service = ServiceProxy.create(
 *     connection,
 *     "org.freedesktop.DBus",
 *     "/org/freedesktop/DBus",
 *     DBusService.class
 * );
 * 
 * // Use it - synchronous
 * String id = service.getId();
 * 
 * // Or asynchronous
 * service.listNames().thenAccept(names -> {
 *     for (String name : names) {
 *         System.out.println(name);
 *     }
 * });
 * }</pre>
 */
public final class ServiceProxy {
    
    private ServiceProxy() {
        // Factory class
    }
    
    /**
     * Creates a proxy instance for the specified D-Bus service interface.
     * 
     * @param connection the D-Bus connection
     * @param destination the D-Bus service name (e.g., "org.freedesktop.DBus")
     * @param objectPath the object path (e.g., "/org/freedesktop/DBus")
     * @param interfaceClass the Java interface class with D-Bus annotations
     * @param <T> the interface type
     * @return a proxy instance implementing the interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(
            final Connection connection,
            final String destination,
            final String objectPath,
            final Class<T> interfaceClass) {
        
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface");
        }
        
        DBusInterface dbusInterface = interfaceClass.getAnnotation(DBusInterface.class);
        if (dbusInterface == null) {
            throw new IllegalArgumentException(
                "Interface must be annotated with @DBusInterface");
        }
        
        InvocationHandler handler = new DBusInvocationHandler(
            connection,
            destination,
            objectPath,
            dbusInterface.value()
        );
        
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            handler
        );
    }
    
    /**
     * Creates a proxy with automatic interface name detection.
     * Uses the @DBusInterface annotation value as the destination.
     * 
     * @param connection the D-Bus connection
     * @param objectPath the object path
     * @param interfaceClass the Java interface class with D-Bus annotations
     * @param <T> the interface type
     * @return a proxy instance implementing the interface
     */
    public static <T> T create(
            final Connection connection,
            final String objectPath,
            final Class<T> interfaceClass) {
        
        DBusInterface dbusInterface = interfaceClass.getAnnotation(DBusInterface.class);
        if (dbusInterface == null) {
            throw new IllegalArgumentException(
                "Interface must be annotated with @DBusInterface");
        }
        
        return create(connection, dbusInterface.value(), objectPath, interfaceClass);
    }
    
    private static class DBusInvocationHandler implements InvocationHandler {
        private final Connection connection;
        private final String destination;
        private final String objectPath;
        private final String interfaceName;
        
        DBusInvocationHandler(
                final Connection connection,
                final String destination,
                final String objectPath,
                final String interfaceName) {
            this.connection = connection;
            this.destination = destination;
            this.objectPath = objectPath;
            this.interfaceName = interfaceName;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Handle Object methods
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            DBusMethod dbusMethod = method.getAnnotation(DBusMethod.class);
            if (dbusMethod == null) {
                throw new UnsupportedOperationException(
                    "Method " + method.getName() + " is not annotated with @DBusMethod");
            }
            
            String methodName = dbusMethod.value().isEmpty() 
                ? method.getName() 
                : dbusMethod.value();
            
            // Build the D-Bus method call
            OutboundMethodCall.Builder callBuilder = OutboundMethodCall.Builder.create()
                .withPath(DBusObjectPath.valueOf(objectPath))
                .withInterface(DBusString.valueOf(interfaceName))
                .withMember(DBusString.valueOf(methodName))
                .withDestination(DBusString.valueOf(destination))
                .withReplyExpected(true);
            
            // TODO: Add argument marshalling based on method parameters
            // For now, this handles methods without arguments
            
            OutboundMethodCall call = callBuilder.build();
            
            // Determine if method returns CompletableFuture/CompletionStage
            Class<?> returnType = method.getReturnType();
            if (CompletableFuture.class.isAssignableFrom(returnType) ||
                CompletionStage.class.isAssignableFrom(returnType)) {
                
                // Return the future directly
                return connection.sendRequest(call)
                    .thenApply(response -> unmarshalResponse(response, method))
                    .toCompletableFuture();
            } else {
                // Synchronous call - block and wait
                InboundMessage response = connection.sendRequest(call)
                    .toCompletableFuture()
                    .get(30, TimeUnit.SECONDS);
                
                return unmarshalResponse(response, method);
            }
        }
        
        private Object unmarshalResponse(InboundMessage response, Method method) {
            if (response instanceof InboundError) {
                InboundError error = (InboundError) response;
                throw new DBusException(
                    "D-Bus error: " + error.getErrorName() + 
                    " - " + error.getErrorMessage()
                );
            }
            
            if (response instanceof InboundMethodReturn) {
                InboundMethodReturn methodReturn = (InboundMethodReturn) response;
                List<DBusType> payload = methodReturn.getPayload();
                
                // TODO: Implement proper unmarshalling based on method return type
                // For now, return null for void methods
                if (method.getReturnType() == void.class || 
                    method.getReturnType() == Void.class) {
                    return null;
                }
                
                // Simple case: single return value
                if (!payload.isEmpty()) {
                    DBusType value = payload.get(0);
                    // TODO: Convert D-Bus type to Java type based on method signature
                    return value;
                }
            }
            
            return null;
        }
    }
    
    /**
     * Exception thrown when D-Bus operations fail.
     */
    public static class DBusException extends RuntimeException {
        public DBusException(String message) {
            super(message);
        }
        
        public DBusException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}