/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Implementation classes for D-Bus standard interfaces and annotation processing.
 * 
 * <p>This package provides concrete implementations that bridge between the
 * annotation framework and the D-Bus protocol, automatically handling
 * introspection, property access, and method dispatch.</p>
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> Use {@link StandardInterfaceHandler} with
 * your annotated service classes. The handler automatically implements standard D-Bus
 * interfaces based on annotations.</p>
 * 
 * <h2>Key Components</h2>
 * 
 * <h3>{@link StandardInterfaceHandler}</h3>
 * <p>The main handler that processes incoming D-Bus method calls and automatically
 * implements standard interfaces:</p>
 * 
 * <pre>{@code
 * // Create annotated service
 * @DBusInterface("com.example.MyService")
 * public class MyService {
 *     @DBusProperty
 *     private String status = "ready";
 *     
 *     @DBusMethod
 *     public String echo(String msg) {
 *         return msg;
 *     }
 * }
 * 
 * // Register with handler
 * MyService service = new MyService();
 * StandardInterfaceHandler handler = new StandardInterfaceHandler(
 *     "/com/example/MyService", service);
 * connection.getPipeline().addLast("service", handler);
 * }</pre>
 * 
 * <h2>Automatic Interface Implementation</h2>
 * 
 * <p>The handler automatically provides:</p>
 * 
 * <ul>
 *   <li><strong>Introspectable:</strong> Generates XML from annotations</li>
 *   <li><strong>Properties:</strong> Get/Set/GetAll for annotated properties</li>
 *   <li><strong>Peer:</strong> Ping and GetMachineId methods</li>
 *   <li><strong>Custom Methods:</strong> Dispatches to annotated methods</li>
 * </ul>
 * 
 * <h2>Future Enhancements</h2>
 * 
 * <p>Planned features for this package:</p>
 * 
 * <ul>
 *   <li>ObjectManager interface support</li>
 *   <li>Automatic signal emission</li>
 *   <li>Advanced type conversion</li>
 *   <li>Method parameter validation</li>
 *   <li>Async method support</li>
 * </ul>
 * 
 * 
 * @see com.lucimber.dbus.annotation
 * @see com.lucimber.dbus.standard
 * @since 2.0
 */
package com.lucimber.dbus.impl;