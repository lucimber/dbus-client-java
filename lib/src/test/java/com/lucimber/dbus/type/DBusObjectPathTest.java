/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

final class DBusObjectPathTest {

    @Test
    void createValidRootPath() {
        DBusObjectPath path = DBusObjectPath.valueOf("/");
        assertEquals("/", path.toString());
        assertEquals("/", path.getWrappedValue());
        assertEquals(Type.OBJECT_PATH, path.getType());
    }

    @Test
    void createValidSimplePath() {
        DBusObjectPath path = DBusObjectPath.valueOf("/com/example/MyObject");
        assertEquals("/com/example/MyObject", path.toString());
        assertEquals("/com/example/MyObject", path.getWrappedValue());
        assertEquals(Type.OBJECT_PATH, path.getType());
    }

    @Test
    void createValidPathWithNumbers() {
        DBusObjectPath path = DBusObjectPath.valueOf("/org/freedesktop/DBus123");
        assertEquals("/org/freedesktop/DBus123", path.toString());
        assertEquals("/org/freedesktop/DBus123", path.getWrappedValue());
    }

    @Test
    void createValidPathWithUnderscores() {
        DBusObjectPath path = DBusObjectPath.valueOf("/com/example/My_Object_123");
        assertEquals("/com/example/My_Object_123", path.toString());
        assertEquals("/com/example/My_Object_123", path.getWrappedValue());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/com/example/MyObject",
                "/org/freedesktop/DBus",
                "/com/example/test123",
                "/path/with_underscore",
                "/single",
                "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z",
                "/A/B/C/D/E/F/G/H/I/J/K/L/M/N/O/P/Q/R/S/T/U/V/W/X/Y/Z",
                "/0/1/2/3/4/5/6/7/8/9",
                "/element_with_underscores",
                "/MixedCase123_elements",
                "/very_long_path_with_many_elements/to/test/deep/nesting/levels/that/should/still/be/valid",
                "/single_element",
                "/a",
                "/Z",
                "/9",
                "/_"
            })
    void createValidPaths(String validPath) {
        assertDoesNotThrow(() -> DBusObjectPath.valueOf(validPath));
        DBusObjectPath path = DBusObjectPath.valueOf(validPath);
        assertEquals(validPath, path.toString());
    }

    @Test
    void failWithNullInput() {
        assertThrows(NullPointerException.class, () -> DBusObjectPath.valueOf(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "",
                "invalid",
                "/invalid-character",
                "/path/with space",
                "/path/with.dot",
                "/path/with@symbol",
                "/path/with:colon",
                "/path//double/slash",
                "/path/trailing/",
                "//double/leading/slash",
                "/path-with-dash",
                "/path/with%percent",
                "/path/with#hash",
                "/path/with$dollar",
                "/path/with+plus",
                "/path/with=equals",
                "/path/with?question",
                "/path/with&ampersand",
                "/path/with|pipe",
                "/path/with<less",
                "/path/with>greater",
                "/path/with[bracket",
                "/path/with]bracket",
                "/path/with{brace",
                "/path/with}brace",
                "/path/with\"quote",
                "/path/with'apostrophe",
                "/path/with\\backslash",
                "/path/with;semicolon",
                "/path/with,comma",
                "/path/with(paren",
                "/path/with)paren",
                "/path/with*asterisk",
                "/path/with^caret",
                "/path/with~tilde",
                "/path/with`backtick"
            })
    void failWithInvalidPaths(String invalidPath) {
        if (invalidPath == null) {
            assertThrows(NullPointerException.class, () -> DBusObjectPath.valueOf(invalidPath));
        } else {
            assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf(invalidPath));
        }
    }

    @Test
    void testEquals() {
        DBusObjectPath path1 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath path2 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath path3 = DBusObjectPath.valueOf("/com/example/OtherObject");

        assertEquals(path1, path2);
        assertNotEquals(path1, path3);
        assertNotEquals(path1, null);
        assertNotEquals(path1, "not an ObjectPath");
        assertEquals(path1, path1); // self-equality
    }

    @Test
    void testHashCode() {
        DBusObjectPath path1 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath path2 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath path3 = DBusObjectPath.valueOf("/com/example/OtherObject");

        assertEquals(path1.hashCode(), path2.hashCode());
        assertNotEquals(path1.hashCode(), path3.hashCode());
    }

    @Test
    void testStartsWith() {
        DBusObjectPath path = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath prefix1 = DBusObjectPath.valueOf("/com");
        DBusObjectPath prefix2 = DBusObjectPath.valueOf("/com/example");
        DBusObjectPath prefix3 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath nonPrefix = DBusObjectPath.valueOf("/org");

        assertTrue(path.startsWith(prefix1));
        assertTrue(path.startsWith(prefix2));
        assertTrue(path.startsWith(prefix3));
        assertFalse(path.startsWith(nonPrefix));
    }

    @Test
    void testEndsWith() {
        DBusObjectPath path = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath suffix1 = DBusObjectPath.valueOf("/MyObject");
        DBusObjectPath suffix2 = DBusObjectPath.valueOf("/example/MyObject");
        DBusObjectPath suffix3 = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusObjectPath nonSuffix = DBusObjectPath.valueOf("/OtherObject");

        assertTrue(path.endsWith(suffix1));
        assertTrue(path.endsWith(suffix2));
        assertTrue(path.endsWith(suffix3));
        assertFalse(path.endsWith(nonSuffix));
    }

    @Test
    void testRootPathSpecialCases() {
        DBusObjectPath root = DBusObjectPath.valueOf("/");

        assertTrue(root.startsWith(root));
        assertTrue(root.endsWith(root));
        assertEquals("/", root.toString());
        assertEquals("/", root.getWrappedValue());
    }

    @Test
    void testDelegate() {
        DBusObjectPath path = DBusObjectPath.valueOf("/com/example/MyObject");
        assertEquals("/com/example/MyObject", path.getDelegate());
    }

    @Test
    void testWrappedValue() {
        String pathString = "/com/example/MyObject";
        DBusObjectPath path = DBusObjectPath.valueOf(pathString);
        CharSequence wrapped = path.getWrappedValue();

        assertEquals(pathString, wrapped.toString());
        assertSame(String.class, wrapped.getClass());
    }

    @Test
    void testLongPath() {
        // Test with a very long but valid path
        String longPath =
                "/very/long/path/that/contains/many/segments/and/should/still/be/valid/according/to/the/dbus/specification/even/though/it/is/quite/lengthy/and/contains/many/path/segments/that/are/all/valid/characters";
        DBusObjectPath path = DBusObjectPath.valueOf(longPath);
        assertEquals(longPath, path.toString());
    }

    @Test
    void testPathWithAllValidCharacters() {
        // Test path containing all valid characters: letters, numbers, underscores
        String validPath = "/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        DBusObjectPath path = DBusObjectPath.valueOf(validPath);
        assertEquals(validPath, path.toString());
    }

    @Test
    void testSingleCharacterSegments() {
        DBusObjectPath path = DBusObjectPath.valueOf("/a/b/c/d/e/f");
        assertEquals("/a/b/c/d/e/f", path.toString());
    }

    @Test
    void testNumericSegments() {
        DBusObjectPath path = DBusObjectPath.valueOf("/0/1/2/3/456/789");
        assertEquals("/0/1/2/3/456/789", path.toString());
    }

    @Test
    void testUnderscoreSegments() {
        DBusObjectPath path = DBusObjectPath.valueOf("/_/__/___/____");
        assertEquals("/_/__/___/____", path.toString());
    }

    @Test
    void testSpecificationRequirements() {
        // Test D-Bus specification requirements

        // 1. Root path is valid
        DBusObjectPath root = DBusObjectPath.valueOf("/");
        assertEquals("/", root.toString());

        // 2. Must begin with '/'
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("com/example"));

        // 3. No empty elements (double slashes)
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("//"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com//example"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example//"));

        // 4. No trailing slash except for root
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example/"));

        // 5. Only ASCII [A-Z][a-z][0-9]_ allowed in elements
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example-dash"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example.dot"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example space"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example@symbol"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example:colon"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example+plus"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example=equals"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example?question"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example&ampersand"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example|pipe"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example<less"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example>greater"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example[bracket"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example]bracket"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example{brace"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example}brace"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example\"quote"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example'apostrophe"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example\\backslash"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example;semicolon"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example,comma"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example(paren"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example)paren"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example*asterisk"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example^caret"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example~tilde"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example`backtick"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example%percent"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example#hash"));
        assertThrows(
                ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example$dollar"));
    }

    @Test
    void testAllValidCharacters() {
        // Test that all valid characters are accepted
        DBusObjectPath path =
                DBusObjectPath.valueOf(
                        "/ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_");
        assertEquals(
                "/ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_",
                path.toString());
    }

    @Test
    void testEmptyElements() {
        // Test that empty elements are rejected
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf(""));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("//"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("///"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/a//b"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/a/b//"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("//a/b"));
    }

    @Test
    void testLeadingSlashRequirement() {
        // Test that object paths must begin with '/'
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("com"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("com/example"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("a"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("123"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("_"));
    }

    @Test
    void testTrailingSlashRestriction() {
        // Test that trailing slashes are only allowed for root path
        assertDoesNotThrow(() -> DBusObjectPath.valueOf("/")); // Root path is valid
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/com/example/"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/a/"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/123/"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/_/"));
    }

    @Test
    void testObjectPathLength() {
        // Test with various path lengths
        // Very short paths
        assertDoesNotThrow(() -> DBusObjectPath.valueOf("/a"));
        assertDoesNotThrow(() -> DBusObjectPath.valueOf("/1"));
        assertDoesNotThrow(() -> DBusObjectPath.valueOf("/_"));

        // Long but valid path
        StringBuilder longPath = new StringBuilder("/");
        for (int i = 0; i < 100; i++) {
            longPath.append("element").append(i);
            if (i < 99) longPath.append("/");
        }
        String longPathStr = longPath.toString();
        assertDoesNotThrow(() -> DBusObjectPath.valueOf(longPathStr));
    }

    @Test
    void testUnicodeRejection() {
        // Test that non-ASCII characters are rejected
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/café"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/münchen"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/🌍"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/元素"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/тест"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/محتوى"));
    }

    @Test
    void testControlCharacterRejection() {
        // Test that control characters are rejected
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\n"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\t"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\r"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\0"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\b"));
        assertThrows(ObjectPathException.class, () -> DBusObjectPath.valueOf("/test\f"));
    }
}
