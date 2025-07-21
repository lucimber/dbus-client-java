/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for D-Bus object path validation compliance.
 */
final class DBusObjectPathValidationTest {

  @Test
  void createValidObjectPaths() {
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/test"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/com/example/test"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/org/freedesktop/DBus"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/path/with_underscores"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/path123/with456/numbers789"));
  }

  @Test
  void rejectNullPath() {
  NullPointerException ex = assertThrows(NullPointerException.class, 
    () -> DBusObjectPath.valueOf(null));
  assertEquals("sequence must not be null", ex.getMessage());
  }

  @Test
  void rejectPathWithNulCharacters() {
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/test\u0000path"));
  assertEquals("Object path must not contain NUL characters", ex.getMessage());
  }

  @Test
  void rejectInvalidCharacters() {
  // Spaces
  ObjectPathException ex1 = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/invalid path"));
  assertEquals("Invalid object path syntax: /invalid path", ex1.getMessage());
  
  // Special characters
  ObjectPathException ex2 = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/invalid@path"));
  assertEquals("Invalid object path syntax: /invalid@path", ex2.getMessage());
  
  // Hash symbol
  ObjectPathException ex3 = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/invalid#path"));
  assertEquals("Invalid object path syntax: /invalid#path", ex3.getMessage());
  }

  @Test
  void acceptPathsWithDigitsInComponents() {
  // D-Bus allows digits in component names, including at the start
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/123valid"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/component123"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/0000_00_1f_2"));
  assertDoesNotThrow(() -> DBusObjectPath.valueOf("/pci0000_00/0000_00_1f_2"));
  }

  @Test
  void rejectPathsWithTrailingSlash() {
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/valid/path/"));
  assertEquals("Invalid object path syntax: /valid/path/", ex.getMessage());
  }

  @Test
  void rejectPathsWithConsecutiveSlashes() {
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/invalid//path"));
  assertEquals("Invalid object path syntax: /invalid//path", ex.getMessage());
  }

  @Test
  void rejectPathsWithEmptyComponents() {
  // This is already covered by consecutive slashes, but let's be explicit
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("/path//component"));
  assertEquals("Invalid object path syntax: /path//component", ex.getMessage());
  }

  @Test
  void rejectPathsNotStartingWithSlash() {
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf("invalid"));
  assertEquals("Invalid object path syntax: invalid", ex.getMessage());
  }

  @Test
  void rejectEmptyPath() {
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf(""));
  assertEquals("Invalid object path syntax: ", ex.getMessage());
  }

  @Test
  @Tag("memory-intensive")
  void acceptLargeButValidPath() {
  // Create a large but valid path
  StringBuilder sb = new StringBuilder("/");
  for (int i = 0; i < 1000; i++) {
      sb.append("component").append(i);
      if (i < 999) {
    sb.append("/");
      }
  }
  assertDoesNotThrow(() -> DBusObjectPath.valueOf(sb.toString()));
  }

  @Test
  @Tag("memory-intensive")
  void rejectOversizedPath() {
  // Create a path that exceeds the maximum size
  StringBuilder sb = new StringBuilder("/");
  // Create components that will exceed 256MB when encoded as UTF-8
  for (int i = 0; i < 20000000; i++) {
      sb.append("very_long_component_name_").append(i).append("/");
  }
  sb.append("final");
  
  ObjectPathException ex = assertThrows(ObjectPathException.class, 
    () -> DBusObjectPath.valueOf(sb.toString()));
  String message = ex.getMessage();
  assertTrue(message.startsWith("Object path too long:"));
  assertTrue(message.contains("maximum 268435455"));
  assertTrue(message.contains("bytes"));
  }

  @Test
  void validateComplexValidPaths() {
  String[] validPaths = {
    "/",
    "/a",
    "/a/b",
    "/a/b/c",
    "/com/example/MyApp",
    "/org/freedesktop/NetworkManager",
    "/system/devices/pci0000_00/0000_00_1f_2",
    "/com/company/app_v2/module_123"
  };
  
  for (String path : validPaths) {
      assertDoesNotThrow(() -> DBusObjectPath.valueOf(path), 
          "Failed for valid path: " + path);
  }
  }

  @Test
  void validateComplexInvalidPaths() {
  String[] invalidPaths = {
    "",                           // Empty
    "relative/path",              // No leading slash
    "/path/",                     // Trailing slash (except root)
    "/path//double",              // Consecutive slashes
    "/invalid-dash",              // Contains dash
    "/invalid.dot",               // Contains dot
    "/invalid space",             // Contains space
    "/invalid@symbol",            // Contains special character
    "/.hidden"                    // Hidden component (starts with dot)
  };
  
  for (String path : invalidPaths) {
      assertThrows(ObjectPathException.class, 
          () -> DBusObjectPath.valueOf(path), 
          "Should have failed for invalid path: " + path);
  }
  }
}