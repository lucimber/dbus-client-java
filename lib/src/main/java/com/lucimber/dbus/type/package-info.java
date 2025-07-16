/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * Type-safe D-Bus data types and containers providing compile-time marshalling safety.
 * 
 * <p>This package implements the complete D-Bus type system as Java wrapper classes
 * that ensure type safety and prevent marshalling errors. All D-Bus types are
 * represented as immutable Java objects that can be safely used across threads.
 * 
 * <h2>Type Hierarchy</h2>
 * 
 * <p>All D-Bus types implement the {@link DBusType} interface and are organized
 * into two main categories:
 * 
 * <h3>Basic Types ({@link DBusBasicType})</h3>
 * <p>Fixed-size, atomic data types:
 * <ul>
 * <li>{@link DBusByte} - 8-bit unsigned integer (0-255)</li>
 * <li>{@link DBusBoolean} - Boolean value (true/false)</li>
 * <li>{@link DBusInt16} - 16-bit signed integer</li>
 * <li>{@link DBusUInt16} - 16-bit unsigned integer</li>
 * <li>{@link DBusInt32} - 32-bit signed integer</li>
 * <li>{@link DBusUInt32} - 32-bit unsigned integer</li>
 * <li>{@link DBusInt64} - 64-bit signed integer</li>
 * <li>{@link DBusUInt64} - 64-bit unsigned integer</li>
 * <li>{@link DBusDouble} - IEEE 754 double-precision floating point</li>
 * <li>{@link DBusString} - UTF-8 text string</li>
 * <li>{@link DBusObjectPath} - D-Bus object path</li>
 * <li>{@link DBusSignature} - D-Bus type signature</li>
 * <li>{@link DBusUnixFD} - Unix file descriptor</li>
 * </ul>
 * 
 * <h3>Container Types ({@link DBusContainerType})</h3>
 * <p>Variable-size, composite data types:
 * <ul>
 * <li>{@link DBusArray} - Homogeneous array of elements</li>
 * <li>{@link DBusStruct} - Heterogeneous structure (like a tuple)</li>
 * <li>{@link DBusDict} - Key-value dictionary/map</li>
 * <li>{@link DBusDictEntry} - Single dictionary entry</li>
 * <li>{@link DBusVariant} - Dynamically typed value</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic Types</h3>
 * <pre>{@code
 * // Creating basic types
 * DBusString text = DBusString.valueOf("Hello, D-Bus!");
 * DBusInt32 number = DBusInt32.valueOf(42);
 * DBusBoolean flag = DBusBoolean.valueOf(true);
 * DBusObjectPath path = DBusObjectPath.valueOf("/org/example/Object");
 * 
 * // Type safety at compile time
 * // This would be a compilation error:
 * // DBusString text = DBusInt32.valueOf(42); // Won't compile!
 * }</pre>
 * 
 * <h3>Arrays</h3>
 * <pre>{@code
 * // Create a string array
 * DBusSignature signature = DBusSignature.valueOf("as");
 * DBusArray<DBusString> stringArray = new DBusArray<>(signature);
 * stringArray.add(DBusString.valueOf("first"));
 * stringArray.add(DBusString.valueOf("second"));
 * stringArray.add(DBusString.valueOf("third"));
 * 
 * // Type safety ensures only strings can be added
 * // stringArray.add(DBusInt32.valueOf(123)); // Compilation error!
 * }</pre>
 * 
 * <h3>Dictionaries</h3>
 * <pre>{@code
 * // Create a string-to-integer dictionary
 * DBusSignature dictSignature = DBusSignature.valueOf("a{si}");
 * DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(dictSignature);
 * dict.put(DBusString.valueOf("count"), DBusInt32.valueOf(10));
 * dict.put(DBusString.valueOf("limit"), DBusInt32.valueOf(100));
 * 
 * // Access values safely
 * DBusInt32 count = dict.get(DBusString.valueOf("count"));
 * if (count != null) {
 *     System.out.println("Count: " + count.getDelegate());
 * }
 * }</pre>
 * 
 * <h3>Structures</h3>
 * <pre>{@code
 * // Create a structure (string, int32, boolean)
 * DBusSignature structSig = DBusSignature.valueOf("(sib)");
 * DBusStruct struct = new DBusStruct(structSig,
 *     DBusString.valueOf("example"),
 *     DBusInt32.valueOf(42),
 *     DBusBoolean.valueOf(true)
 * );
 * 
 * // Access struct elements
 * List<DBusType> elements = struct.getElements();
 * DBusString firstElement = (DBusString) elements.get(0);
 * }</pre>
 * 
 * <h3>Variants</h3>
 * <pre>{@code
 * // Variants can hold any D-Bus type
 * DBusVariant stringVariant = DBusVariant.valueOf(DBusString.valueOf("text"));
 * DBusVariant numberVariant = DBusVariant.valueOf(DBusInt32.valueOf(123));
 * 
 * // Type checking when extracting values
 * if (stringVariant.getValue() instanceof DBusString) {
 *     DBusString text = (DBusString) stringVariant.getValue();
 *     System.out.println("Text: " + text.toString());
 * }
 * }</pre>
 * 
 * <h2>Type Signatures</h2>
 * 
 * <p>D-Bus type signatures describe the structure of data using a compact string format:
 * 
 * <table border="1">
 * <caption>D-Bus Type Signatures</caption>
 * <tr><th>Type</th><th>Signature</th><th>Java Type</th></tr>
 * <tr><td>Byte</td><td>y</td><td>{@link DBusByte}</td></tr>
 * <tr><td>Boolean</td><td>b</td><td>{@link DBusBoolean}</td></tr>
 * <tr><td>Int32</td><td>i</td><td>{@link DBusInt32}</td></tr>
 * <tr><td>String</td><td>s</td><td>{@link DBusString}</td></tr>
 * <tr><td>Array of strings</td><td>as</td><td>{@link DBusArray}&lt;{@link DBusString}&gt;</td></tr>
 * <tr><td>Dictionary</td><td>a{si}</td><td>{@link DBusDict}&lt;{@link DBusString}, {@link DBusInt32}&gt;</td></tr>
 * <tr><td>Struct</td><td>(si)</td><td>{@link DBusStruct}</td></tr>
 * <tr><td>Variant</td><td>v</td><td>{@link DBusVariant}</td></tr>
 * </table>
 * 
 * <h2>Validation and Constraints</h2>
 * 
 * <p>All types enforce D-Bus specification constraints:
 * 
 * <ul>
 * <li><strong>String validation:</strong> Strings must be valid UTF-8 and not contain NUL characters</li>
 * <li><strong>Object path validation:</strong> Object paths must follow D-Bus naming conventions</li>
 * <li><strong>Signature validation:</strong> Type signatures must be syntactically correct</li>
 * <li><strong>Size limits:</strong> Arrays and strings have maximum size limits (64MB for arrays, 256MB for strings)</li>
 * <li><strong>Alignment requirements:</strong> Data is properly aligned according to D-Bus specification</li>
 * </ul>
 * 
 * <h2>Immutability and Thread Safety</h2>
 * 
 * <p>All D-Bus types are immutable and thread-safe:
 * 
 * <ul>
 * <li>Once created, type instances cannot be modified</li>
 * <li>Container types provide defensive copies when needed</li>
 * <li>All operations are safe for concurrent access</li>
 * <li>No synchronization is required when sharing instances between threads</li>
 * </ul>
 * 
 * <h2>Memory Efficiency</h2>
 * 
 * <p>The type system is designed for memory efficiency:
 * 
 * <ul>
 * <li>Basic types wrap primitive values with minimal overhead</li>
 * <li>Container types use efficient underlying collections</li>
 * <li>Large objects (arrays, strings) are handled with care for memory usage</li>
 * <li>Type signatures are validated once and cached</li>
 * </ul>
 * 
 * @see DBusType
 * @see DBusBasicType
 * @see DBusContainerType
 * @see DBusSignature
 * @since 1.0
 */
package com.lucimber.dbus.type;
