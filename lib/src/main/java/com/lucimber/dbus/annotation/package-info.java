/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Annotations for defining D-Bus interfaces, methods, properties, and signals.
 * 
 * <p>This package provides annotations that simplify the implementation of D-Bus
 * services by allowing you to annotate Java classes and have the framework
 * automatically handle D-Bus introspection, property access, and method calls.</p>
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> Start by annotating your service class
 * with {@link DBusInterface} and its methods/properties with the appropriate annotations.
 * Then use {@link com.lucimber.dbus.annotation.StandardInterfaceHandler} to handle incoming calls.</p>
 * 
 * <h2>Available Annotations</h2>
 * 
 * <h3>{@link DBusInterface}</h3>
 * <p>Marks a class or interface as representing a D-Bus interface:</p>
 * <pre>{@code
 * @DBusInterface("com.example.MyService")
 * public class MyService {
 *     // Methods and properties
 * }
 * }</pre>
 * 
 * <h3>{@link DBusMethod}</h3>
 * <p>Exposes a method as a D-Bus method:</p>
 * <pre>{@code
 * @DBusMethod(name = "Echo")
 * public String echo(String message) {
 *     return message;
 * }
 * 
 * @DBusMethod  // Uses Java method name
 * public void doSomething() {
 *     // Implementation
 * }
 * }</pre>
 * 
 * <h3>{@link DBusProperty}</h3>
 * <p>Exposes fields or getter/setter methods as D-Bus properties:</p>
 * <pre>{@code
 * // Field-based property
 * @DBusProperty(name = "Version")
 * private String version = "1.0.0";
 * 
 * // Method-based property (read-only)
 * @DBusProperty
 * public String getStatus() {
 *     return currentStatus;
 * }
 * 
 * // Method-based property (read-write)
 * @DBusProperty(name = "Count")
 * public int getCount() {
 *     return count;
 * }
 * 
 * @DBusProperty(name = "Count")
 * public void setCount(int count) {
 *     this.count = count;
 * }
 * }</pre>
 * 
 * <h3>{@link DBusSignal}</h3>
 * <p>Marks methods that emit D-Bus signals:</p>
 * <pre>{@code
 * @DBusSignal(name = "StatusChanged")
 * public void emitStatusChanged(String oldStatus, String newStatus) {
 *     // Framework handles signal emission
 * }
 * }</pre>
 * 
 * <h2>Complete Example</h2>
 * 
 * <pre>{@code
 * @DBusInterface("com.example.Calculator")
 * public class CalculatorService {
 *     
 *     @DBusProperty
 *     private String version = "1.0.0";
 *     
 *     @DBusProperty(access = DBusProperty.Access.READ)
 *     private int calculationCount = 0;
 *     
 *     @DBusMethod
 *     public double add(double a, double b) {
 *         calculationCount++;
 *         emitCalculationPerformed("add", a, b);
 *         return a + b;
 *     }
 *     
 *     @DBusMethod(name = "Multiply")
 *     public double multiply(double a, double b) {
 *         calculationCount++;
 *         emitCalculationPerformed("multiply", a, b);
 *         return a * b;
 *     }
 *     
 *     @DBusSignal
 *     public void emitCalculationPerformed(String operation, double a, double b) {
 *         // Signal emission handled by framework
 *     }
 * }
 * 
 * // Register the service
 * CalculatorService calculator = new CalculatorService();
 * StandardInterfaceHandler handler = new StandardInterfaceHandler(
 *     "/com/example/Calculator", calculator);
 * connection.getPipeline().addLast("calculator", handler);
 * }</pre>
 * 
 * <h2>Integration with Standard Interfaces</h2>
 * 
 * <p>When using these annotations with {@link com.lucimber.dbus.annotation.StandardInterfaceHandler},
 * the following standard D-Bus interfaces are automatically implemented:</p>
 * 
 * <ul>
 *   <li><strong>org.freedesktop.DBus.Introspectable</strong> - Generates introspection
 *       XML from annotations</li>
 *   <li><strong>org.freedesktop.DBus.Properties</strong> - Provides Get/Set/GetAll
 *       access to annotated properties</li>
 *   <li><strong>org.freedesktop.DBus.Peer</strong> - Standard Ping and GetMachineId
 *       implementation</li>
 * </ul>
 * 
 * <h2>Property Access Modes</h2>
 * 
 * <p>The {@link DBusProperty.Access} enum controls property accessibility:</p>
 * 
 * <ul>
 *   <li><strong>AUTO</strong> - Automatically determined based on field/methods</li>
 *   <li><strong>READ</strong> - Read-only property</li>
 *   <li><strong>WRITE</strong> - Write-only property</li>
 *   <li><strong>READWRITE</strong> - Read-write property</li>
 * </ul>
 * 
 * <h2>Type Mapping</h2>
 * 
 * <p>Java types are automatically mapped to D-Bus types:</p>
 * 
 * <table>
 *   <tr><th>Java Type</th><th>D-Bus Type</th><th>Notes</th></tr>
 *   <tr><td>String</td><td>s (STRING)</td><td>UTF-8 strings</td></tr>
 *   <tr><td>boolean</td><td>b (BOOLEAN)</td><td></td></tr>
 *   <tr><td>byte</td><td>y (BYTE)</td><td></td></tr>
 *   <tr><td>short</td><td>n (INT16)</td><td></td></tr>
 *   <tr><td>int</td><td>i (INT32)</td><td></td></tr>
 *   <tr><td>long</td><td>x (INT64)</td><td></td></tr>
 *   <tr><td>double</td><td>d (DOUBLE)</td><td></td></tr>
 *   <tr><td>List&lt;T&gt;</td><td>a* (ARRAY)</td><td>Type parameter determines element type</td></tr>
 *   <tr><td>Map&lt;K,V&gt;</td><td>a{**} (DICT)</td><td>Key must be basic type</td></tr>
 * </table>
 * 
 * <h2>Best Practices</h2>
 * 
 * <ul>
 *   <li><strong>Interface Naming:</strong> Use reverse domain notation (com.example.Service)</li>
 *   <li><strong>Method Naming:</strong> Use PascalCase for D-Bus names in annotations</li>
 *   <li><strong>Property Naming:</strong> Use PascalCase for D-Bus property names</li>
 *   <li><strong>Signal Naming:</strong> Use descriptive names indicating what changed</li>
 *   <li><strong>Thread Safety:</strong> Ensure annotated methods are thread-safe</li>
 *   <li><strong>Error Handling:</strong> Throw appropriate exceptions for D-Bus errors</li>
 * </ul>
 * 
 * <h2>Limitations</h2>
 * 
 * <p>Current limitations of the annotation framework:</p>
 * 
 * <ul>
 *   <li>Complex type mapping may require manual conversion</li>
 *   <li>Generic type information may be lost at runtime</li>
 *   <li>Signal emission requires framework support</li>
 *   <li>Method overloading is not supported (D-Bus limitation)</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.annotation.StandardInterfaceHandler
 * @see com.lucimber.dbus.standard
 * @since 2.0
 */
package com.lucimber.dbus.annotation;