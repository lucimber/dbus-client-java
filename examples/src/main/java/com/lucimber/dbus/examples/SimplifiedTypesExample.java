/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.DBusInt32;
import com.lucimber.dbus.type.DBusInt64;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusVariant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified D-Bus Types Example
 * <p>
 * This example demonstrates the new factory methods that simplify
 * creating common D-Bus container types without verbose signature handling.
 */
public class SimplifiedTypesExample {
    
    public static void main(String[] args) {
        System.out.println("=== Simplified D-Bus Types Example ===\n");
        
        demonstrateArrayFactoryMethods();
        demonstrateDictionaryFactoryMethods();
        demonstrateJavaConversions();
    }
    
    private static void demonstrateArrayFactoryMethods() {
        System.out.println("1. Array Factory Methods:");
        System.out.println("------------------------");
        
        // OLD WAY - Verbose with signatures
        System.out.println("Old way (verbose):");
        DBusArray<DBusString> oldArray = new DBusArray<>(DBusSignature.valueOf("as"));
        oldArray.add(DBusString.valueOf("first"));
        oldArray.add(DBusString.valueOf("second"));
        System.out.println("  Created string array with " + oldArray.size() + " elements");
        
        // NEW WAY - Simple factory methods
        System.out.println("\nNew way (simplified):");
        
        // String array
        DBusArray<DBusString> strings = DBusArray.ofStrings("hello", "world", "from", "dbus");
        System.out.println("  String array: " + strings.size() + " elements");
        
        // Integer array
        DBusArray<DBusInt32> integers = DBusArray.ofInt32s(1, 2, 3, 4, 5);
        System.out.println("  Integer array: " + integers.size() + " elements");
        
        // Boolean array
        DBusArray<DBusBoolean> booleans = DBusArray.ofBooleans(true, false, true);
        System.out.println("  Boolean array: " + booleans.size() + " elements");
        
        // Object path array
        DBusArray<DBusObjectPath> paths = DBusArray.ofObjectPaths(
            "/org/example/Object1",
            "/org/example/Object2"
        );
        System.out.println("  Object path array: " + paths.size() + " elements");
        
        // Empty typed array
        DBusArray<DBusString> emptyStrings = DBusArray.empty("s");
        System.out.println("  Empty string array created");
        
        System.out.println();
    }
    
    private static void demonstrateDictionaryFactoryMethods() {
        System.out.println("2. Dictionary Factory Methods:");
        System.out.println("-----------------------------");
        
        // OLD WAY - Verbose with signatures
        System.out.println("Old way (verbose):");
        DBusDict<DBusString, DBusInt32> oldDict = new DBusDict<>(DBusSignature.valueOf("a{si}"));
        oldDict.put(DBusString.valueOf("key1"), DBusInt32.valueOf(100));
        System.out.println("  Created string->int dictionary");
        
        // NEW WAY - Simple factory methods
        System.out.println("\nNew way (simplified):");
        
        // String to String dictionary
        DBusDict<DBusString, DBusString> stringDict = DBusDict.ofStringToString();
        stringDict.put(DBusString.valueOf("name"), DBusString.valueOf("D-Bus Client"));
        stringDict.put(DBusString.valueOf("version"), DBusString.valueOf("2.0"));
        System.out.println("  String->String dict with " + stringDict.size() + " entries");
        
        // String to Variant dictionary (common for properties)
        DBusDict<DBusString, DBusVariant> properties = DBusDict.ofStringToVariant();
        properties.put(DBusString.valueOf("Active"), DBusVariant.valueOf(DBusBoolean.valueOf(true)));
        properties.put(DBusString.valueOf("Count"), DBusVariant.valueOf(DBusInt32.valueOf(42)));
        properties.put(DBusString.valueOf("Name"), DBusVariant.valueOf(DBusString.valueOf("Example")));
        System.out.println("  String->Variant dict with " + properties.size() + " properties");
        
        // Other common patterns
        DBusDict<DBusString, DBusInt32> counters = DBusDict.ofStringToInt32();
        DBusDict<DBusString, DBusBoolean> flags = DBusDict.ofStringToBoolean();
        System.out.println("  Created specialized dictionaries");
        
        System.out.println();
    }
    
    private static void demonstrateJavaConversions() {
        System.out.println("3. Java Map Conversions:");
        System.out.println("------------------------");
        
        // Convert from Java Map<String, String>
        Map<String, String> javaStringMap = new HashMap<>();
        javaStringMap.put("author", "Lucimber");
        javaStringMap.put("license", "Apache-2.0");
        javaStringMap.put("language", "Java");
        
        DBusDict<DBusString, DBusString> dbusStringDict = DBusDict.fromStringMap(javaStringMap);
        System.out.println("  Converted Java String map to D-Bus dict: " + dbusStringDict.size() + " entries");
        
        // Convert from Java Map<String, Object> to variant dictionary
        Map<String, Object> javaObjectMap = new HashMap<>();
        javaObjectMap.put("enabled", true);
        javaObjectMap.put("port", 8080);
        javaObjectMap.put("host", "localhost");
        javaObjectMap.put("timeout", 30L);
        javaObjectMap.put("rate", 0.95);
        
        DBusDict<DBusString, DBusVariant> dbusVariantDict = DBusDict.fromVariantMap(javaObjectMap);
        System.out.println("  Converted Java Object map to D-Bus variant dict: " + dbusVariantDict.size() + " entries");
        
        System.out.println("\n✅ Factory methods make D-Bus type creation much simpler!");
    }
}