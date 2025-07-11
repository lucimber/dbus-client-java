/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class ObjectPathTest {

    @Test
    void createValidRootPath() {
        ObjectPath path = ObjectPath.valueOf("/");
        assertEquals("/", path.toString());
        assertEquals("/", path.getWrappedValue());
        assertEquals(Type.OBJECT_PATH, path.getType());
    }

    @Test
    void createValidSimplePath() {
        ObjectPath path = ObjectPath.valueOf("/com/example/MyObject");
        assertEquals("/com/example/MyObject", path.toString());
        assertEquals("/com/example/MyObject", path.getWrappedValue());
        assertEquals(Type.OBJECT_PATH, path.getType());
    }

    @Test
    void createValidPathWithNumbers() {
        ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus123");
        assertEquals("/org/freedesktop/DBus123", path.toString());
        assertEquals("/org/freedesktop/DBus123", path.getWrappedValue());
    }

    @Test
    void createValidPathWithUnderscores() {
        ObjectPath path = ObjectPath.valueOf("/com/example/My_Object_123");
        assertEquals("/com/example/My_Object_123", path.toString());
        assertEquals("/com/example/My_Object_123", path.getWrappedValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
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
        assertDoesNotThrow(() -> ObjectPath.valueOf(validPath));
        ObjectPath path = ObjectPath.valueOf(validPath);
        assertEquals(validPath, path.toString());
    }

    @Test
    void failWithNullInput() {
        assertThrows(NullPointerException.class, () -> ObjectPath.valueOf(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
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
            assertThrows(NullPointerException.class, () -> ObjectPath.valueOf(invalidPath));
        } else {
            assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf(invalidPath));
        }
    }

    @Test
    void testEquals() {
        ObjectPath path1 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath path2 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath path3 = ObjectPath.valueOf("/com/example/OtherObject");

        assertEquals(path1, path2);
        assertNotEquals(path1, path3);
        assertNotEquals(path1, null);
        assertNotEquals(path1, "not an ObjectPath");
        assertEquals(path1, path1); // self-equality
    }

    @Test
    void testHashCode() {
        ObjectPath path1 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath path2 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath path3 = ObjectPath.valueOf("/com/example/OtherObject");

        assertEquals(path1.hashCode(), path2.hashCode());
        assertNotEquals(path1.hashCode(), path3.hashCode());
    }

    @Test
    void testStartsWith() {
        ObjectPath path = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath prefix1 = ObjectPath.valueOf("/com");
        ObjectPath prefix2 = ObjectPath.valueOf("/com/example");
        ObjectPath prefix3 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath nonPrefix = ObjectPath.valueOf("/org");

        assertTrue(path.startsWith(prefix1));
        assertTrue(path.startsWith(prefix2));
        assertTrue(path.startsWith(prefix3));
        assertFalse(path.startsWith(nonPrefix));
    }

    @Test
    void testEndsWith() {
        ObjectPath path = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath suffix1 = ObjectPath.valueOf("/MyObject");
        ObjectPath suffix2 = ObjectPath.valueOf("/example/MyObject");
        ObjectPath suffix3 = ObjectPath.valueOf("/com/example/MyObject");
        ObjectPath nonSuffix = ObjectPath.valueOf("/OtherObject");

        assertTrue(path.endsWith(suffix1));
        assertTrue(path.endsWith(suffix2));
        assertTrue(path.endsWith(suffix3));
        assertFalse(path.endsWith(nonSuffix));
    }

    @Test
    void testRootPathSpecialCases() {
        ObjectPath root = ObjectPath.valueOf("/");
        
        assertTrue(root.startsWith(root));
        assertTrue(root.endsWith(root));
        assertEquals("/", root.toString());
        assertEquals("/", root.getWrappedValue());
    }

    @Test
    void testDelegate() {
        ObjectPath path = ObjectPath.valueOf("/com/example/MyObject");
        assertEquals("/com/example/MyObject", path.getDelegate());
    }

    @Test
    void testWrappedValue() {
        String pathString = "/com/example/MyObject";
        ObjectPath path = ObjectPath.valueOf(pathString);
        CharSequence wrapped = path.getWrappedValue();
        
        assertEquals(pathString, wrapped.toString());
        assertSame(String.class, wrapped.getClass());
    }

    @Test
    void testLongPath() {
        // Test with a very long but valid path
        String longPath = "/very/long/path/that/contains/many/segments/and/should/still/be/valid/according/to/the/dbus/specification/even/though/it/is/quite/lengthy/and/contains/many/path/segments/that/are/all/valid/characters";
        ObjectPath path = ObjectPath.valueOf(longPath);
        assertEquals(longPath, path.toString());
    }

    @Test
    void testPathWithAllValidCharacters() {
        // Test path containing all valid characters: letters, numbers, underscores
        String validPath = "/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        ObjectPath path = ObjectPath.valueOf(validPath);
        assertEquals(validPath, path.toString());
    }

    @Test
    void testSingleCharacterSegments() {
        ObjectPath path = ObjectPath.valueOf("/a/b/c/d/e/f");
        assertEquals("/a/b/c/d/e/f", path.toString());
    }

    @Test
    void testNumericSegments() {
        ObjectPath path = ObjectPath.valueOf("/0/1/2/3/456/789");
        assertEquals("/0/1/2/3/456/789", path.toString());
    }

    @Test
    void testUnderscoreSegments() {
        ObjectPath path = ObjectPath.valueOf("/_/__/___/____");
        assertEquals("/_/__/___/____", path.toString());
    }

    @Test
    void testSpecificationRequirements() {
        // Test D-Bus specification requirements

        // 1. Root path is valid
        ObjectPath root = ObjectPath.valueOf("/");
        assertEquals("/", root.toString());

        // 2. Must begin with '/'
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("com/example"));

        // 3. No empty elements (double slashes)
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("//"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com//example"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example//"));

        // 4. No trailing slash except for root
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example/"));

        // 5. Only ASCII [A-Z][a-z][0-9]_ allowed in elements
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example-dash"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example.dot"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example space"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example@symbol"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example:colon"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example+plus"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example=equals"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example?question"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example&ampersand"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example|pipe"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example<less"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example>greater"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example[bracket"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example]bracket"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example{brace"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example}brace"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example\"quote"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example'apostrophe"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example\\backslash"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example;semicolon"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example,comma"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example(paren"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example)paren"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example*asterisk"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example^caret"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example~tilde"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example`backtick"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example%percent"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example#hash"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example$dollar"));
    }

    @Test
    void testAllValidCharacters() {
        // Test that all valid characters are accepted
        ObjectPath path = ObjectPath.valueOf("/ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_");
        assertEquals("/ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_", path.toString());
    }

    @Test
    void testEmptyElements() {
        // Test that empty elements are rejected
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf(""));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("//"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("///"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/a//b"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/a/b//"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("//a/b"));
    }

    @Test
    void testLeadingSlashRequirement() {
        // Test that object paths must begin with '/'
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("com"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("com/example"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("a"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("123"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("_"));
    }

    @Test
    void testTrailingSlashRestriction() {
        // Test that trailing slashes are only allowed for root path
        assertDoesNotThrow(() -> ObjectPath.valueOf("/"));  // Root path is valid
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/com/example/"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/a/"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/123/"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/_/"));
    }

    @Test
    void testObjectPathLength() {
        // Test with various path lengths
        // Very short paths
        assertDoesNotThrow(() -> ObjectPath.valueOf("/a"));
        assertDoesNotThrow(() -> ObjectPath.valueOf("/1"));
        assertDoesNotThrow(() -> ObjectPath.valueOf("/_"));
        
        // Long but valid path
        StringBuilder longPath = new StringBuilder("/");
        for (int i = 0; i < 100; i++) {
            longPath.append("element").append(i);
            if (i < 99) longPath.append("/");
        }
        String longPathStr = longPath.toString();
        assertDoesNotThrow(() -> ObjectPath.valueOf(longPathStr));
    }

    @Test
    void testUnicodeRejection() {
        // Test that non-ASCII characters are rejected
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/café"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/münchen"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/🌍"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/元素"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/тест"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/محتوى"));
    }

    @Test
    void testControlCharacterRejection() {
        // Test that control characters are rejected
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\n"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\t"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\r"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\0"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\b"));
        assertThrows(ObjectPathException.class, () -> ObjectPath.valueOf("/test\f"));
    }
}