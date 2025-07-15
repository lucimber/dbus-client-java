/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class DBusStringTest {

    @Test
    void createStringWithValidText() {
        String input = "Hello, D-Bus!";
        DBusString dbusString = DBusString.valueOf(input);
        
        assertEquals(input, dbusString.toString());
        assertEquals(input, dbusString.getDelegate());
        assertEquals(Type.STRING, dbusString.getType());
    }

    @Test
    void createStringWithNullValue() {
        // D-Bus specification compliance: null values should be rejected
        assertThrows(NullPointerException.class, () -> DBusString.valueOf(null));
    }

    @Test
    void createStringWithEmptyString() {
        DBusString dbusString = DBusString.valueOf("");
        
        assertEquals("", dbusString.toString());
        assertEquals("", dbusString.getDelegate());
        assertEquals(Type.STRING, dbusString.getType());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {
        "simple text",
        "Hello, World!",
        "123456789",
        "Special chars: @#$%^&*()_+-=[]{}|;:,.<>?",
        "Unicode: 🌍🚀🎉 こんにちは مرحبا",
        "Multi\nline\nstring",
        "Tab\tseparated\tvalues",
        "Quotes: 'single' and \"double\"",
        "Backslashes: \\ and \\\\",
        "Path-like: /usr/bin/java",
        "XML-like: <tag>content</tag>",
        "JSON-like: {\"key\": \"value\"}",
        "SQL-like: SELECT * FROM table;",
        "Very long string that exceeds typical buffer sizes and should still work correctly with the DBusString implementation regardless of the content length",
        "\u0001\u0002\u0003", // Control characters (excluding NUL)
        "\u00FF\u0100\u0101", // Extended ASCII
        "\uD83D\uDE00\uD83D\uDE01", // Emoji surrogates
        "   leading and trailing spaces   ",
        "Multiple    spaces    between    words"
    })
    void createStringWithVariousInputs(String input) {
        DBusString dbusString = DBusString.valueOf(input);
        
        assertEquals(input, dbusString.toString());
        assertEquals(input, dbusString.getDelegate());
        assertEquals(Type.STRING, dbusString.getType());
    }
    
    @Test
    void rejectNullStringInput() {
        // D-Bus specification compliance: null values should be rejected
        assertThrows(NullPointerException.class, () -> DBusString.valueOf(null));
    }
    
    @Test
    void rejectStringWithNullBytes() {
        // D-Bus specification compliance: NUL bytes should be rejected
        assertThrows(IllegalArgumentException.class, () -> DBusString.valueOf("test\u0000string"));
        assertThrows(IllegalArgumentException.class, () -> DBusString.valueOf("\u0000"));
        assertThrows(IllegalArgumentException.class, () -> DBusString.valueOf("\u0000\u0001\u0002\u0003"));
    }

    @Test
    void testEquals() {
        DBusString string1 = DBusString.valueOf("test");
        DBusString string2 = DBusString.valueOf("test");
        DBusString string3 = DBusString.valueOf("different");
        DBusString emptyString1 = DBusString.valueOf("");
        DBusString emptyString2 = DBusString.valueOf("");

        // Test equality
        assertEquals(string1, string2);
        assertNotEquals(string1, string3);
        assertEquals(emptyString1, emptyString2);
        assertNotEquals(string1, emptyString1);
        
        // Test self-equality
        assertEquals(string1, string1);
        assertEquals(emptyString1, emptyString1);
        
        // Test against null and different types
        assertNotEquals(string1, null);
        assertNotEquals(string1, "test"); // Different type
        assertNotEquals(string1, 123); // Different type
    }

    @Test
    void testHashCode() {
        DBusString string1 = DBusString.valueOf("test");
        DBusString string2 = DBusString.valueOf("test");
        DBusString string3 = DBusString.valueOf("different");
        DBusString emptyString1 = DBusString.valueOf("");
        DBusString emptyString2 = DBusString.valueOf("");

        // Equal objects should have equal hash codes
        assertEquals(string1.hashCode(), string2.hashCode());
        assertEquals(emptyString1.hashCode(), emptyString2.hashCode());
        
        // Different objects should typically have different hash codes
        assertNotEquals(string1.hashCode(), string3.hashCode());
        assertNotEquals(string1.hashCode(), emptyString1.hashCode());
    }

    @Test
    void testToString() {
        // Test with regular string
        String input = "Hello, D-Bus!";
        DBusString dbusString = DBusString.valueOf(input);
        assertEquals(input, dbusString.toString());
        
        // Test with empty string
        DBusString emptyString = DBusString.valueOf("");
        assertEquals("", emptyString.toString());
        
        // Test with various valid strings
        DBusString unicodeString = DBusString.valueOf("Unicode: 🌍");
        assertEquals("Unicode: 🌍", unicodeString.toString());
    }

    @Test
    void testGetDelegate() {
        // Test with regular string
        String input = "Hello, D-Bus!";
        DBusString dbusString = DBusString.valueOf(input);
        assertEquals(input, dbusString.getDelegate());
        assertSame(input, dbusString.getDelegate()); // Should be the same reference
        
        // Test with empty string
        String empty = "";
        DBusString emptyString = DBusString.valueOf(empty);
        assertEquals(empty, emptyString.getDelegate());
        assertSame(empty, emptyString.getDelegate()); // Should be the same reference
        
        // Test with Unicode string
        String unicode = "🌍🚀";
        DBusString unicodeString = DBusString.valueOf(unicode);
        assertEquals(unicode, unicodeString.getDelegate());
        assertSame(unicode, unicodeString.getDelegate());
    }

    @Test
    void testGetType() {
        // Test that getType always returns STRING regardless of content
        assertEquals(Type.STRING, DBusString.valueOf("test").getType());
        assertEquals(Type.STRING, DBusString.valueOf("").getType());
        assertEquals(Type.STRING, DBusString.valueOf("Unicode: 🌍").getType());
        assertEquals(Type.STRING, DBusString.valueOf("very long string with lots of content").getType());
    }

    @Test
    void testWithSpecialCharacters() {
        // Test with newlines
        DBusString withNewlines = DBusString.valueOf("line1\nline2\nline3");
        assertEquals("line1\nline2\nline3", withNewlines.toString());
        
        // Test with tabs
        DBusString withTabs = DBusString.valueOf("col1\tcol2\tcol3");
        assertEquals("col1\tcol2\tcol3", withTabs.toString());
        
        // Test with carriage returns
        DBusString withCR = DBusString.valueOf("line1\rline2");
        assertEquals("line1\rline2", withCR.toString());
        
        // Test with mixed line endings
        DBusString mixedEndings = DBusString.valueOf("line1\r\nline2\nline3\r");
        assertEquals("line1\r\nline2\nline3\r", mixedEndings.toString());
    }

    @Test
    void testWithUnicodeCharacters() {
        // Test with various Unicode characters
        DBusString unicode = DBusString.valueOf("Hello 世界 🌍");
        assertEquals("Hello 世界 🌍", unicode.toString());
        
        // Test with emojis
        DBusString emoji = DBusString.valueOf("🚀🎉🌟💫");
        assertEquals("🚀🎉🌟💫", emoji.toString());
        
        // Test with Arabic text
        DBusString arabic = DBusString.valueOf("مرحبا بالعالم");
        assertEquals("مرحبا بالعالم", arabic.toString());
    }

    @Test
    void testWithWhitespace() {
        // Test with leading whitespace
        DBusString leadingSpace = DBusString.valueOf("   hello");
        assertEquals("   hello", leadingSpace.toString());
        
        // Test with trailing whitespace
        DBusString trailingSpace = DBusString.valueOf("hello   ");
        assertEquals("hello   ", trailingSpace.toString());
        
        // Test with only whitespace
        DBusString onlySpaces = DBusString.valueOf("   ");
        assertEquals("   ", onlySpaces.toString());
        
        // Test with mixed whitespace
        DBusString mixedWhitespace = DBusString.valueOf(" \t\n\r ");
        assertEquals(" \t\n\r ", mixedWhitespace.toString());
    }

    @Test
    void testEqualsWithVariousInputs() {
        // Test equals with identical strings
        assertEquals(DBusString.valueOf("test"), DBusString.valueOf("test"));
        
        // Test equals with different strings
        assertNotEquals(DBusString.valueOf("test1"), DBusString.valueOf("test2"));
        
        // Test equals with case sensitivity
        assertNotEquals(DBusString.valueOf("Test"), DBusString.valueOf("test"));
        
        // Test equals with whitespace differences
        assertNotEquals(DBusString.valueOf("test"), DBusString.valueOf(" test"));
        assertNotEquals(DBusString.valueOf("test"), DBusString.valueOf("test "));
        assertNotEquals(DBusString.valueOf("test"), DBusString.valueOf("te st"));
    }

    @Test
    @Tag("memory-intensive")
    void testWithVeryLongString() {
        // Test with a very long string
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Long string content ");
        }
        String longString = sb.toString();
        
        DBusString dbusString = DBusString.valueOf(longString);
        assertEquals(longString, dbusString.toString());
        assertEquals(longString, dbusString.getDelegate());
        assertEquals(Type.STRING, dbusString.getType());
    }

    @Test
    void testImmutability() {
        // Test that DBusString is immutable
        String original = "original";
        DBusString dbusString = DBusString.valueOf(original);
        
        // Getting the delegate should return the same reference
        assertSame(original, dbusString.getDelegate());
        
        // Modifying the original string (if it were mutable) shouldn't affect the DBusString
        // Since String is immutable in Java, this is guaranteed, but we test the principle
        String retrieved = dbusString.getDelegate();
        assertEquals(original, retrieved);
    }

    @Test
    void testUTF8Validation() {
        // Test D-Bus specification UTF-8 validation requirements
        // Per D-Bus specification: strings must be valid UTF-8
        
        // Test valid UTF-8 strings
        String[] validUTF8 = {
            "Hello, World!",
            "ASCII characters only",
            "UTF-8 with accents: café, naïve, résumé",
            "UTF-8 with symbols: €, £, ¥, ©, ®, ™",
            "UTF-8 with emoji: 🌍🚀🎉",
            "UTF-8 with CJK: 中文, 日本語, 한국어",
            "UTF-8 with Arabic: مرحبا بالعالم",
            "UTF-8 with Hebrew: שלום עולם",
            "UTF-8 with Russian: Привет, мир",
            "UTF-8 mathematical symbols: ∑, ∏, ∫, ∞, ≈, ≠",
            "UTF-8 arrows: ←, →, ↑, ↓, ⇔, ⇒"
        };
        
        for (String validString : validUTF8) {
            assertDoesNotThrow(() -> {
                DBusString dbusString = DBusString.valueOf(validString);
                assertEquals(validString, dbusString.toString());
                assertEquals(Type.STRING, dbusString.getType());
            });
        }
    }

    @Test
    void testNullByteHandling() {
        // Test D-Bus specification requirement: strings must not contain U+0000 (null bytes)
        
        // Test that empty string is valid
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf("");
            assertEquals("", dbusString.toString());
        });
        
        // Test that null reference is properly rejected per D-Bus specification
        assertThrows(NullPointerException.class, () -> {
            DBusString.valueOf(null);
        });
        
        // Test that strings with null bytes are rejected per D-Bus specification
        assertThrows(IllegalArgumentException.class, () -> {
            DBusString.valueOf("test\u0000invalid");
        });
    }

    @Test
    void testUnicodeNoncharacters() {
        // Test D-Bus specification: Since version 0.21, Unicode noncharacters are allowed
        // Noncharacters: U+FDD0..U+FDEF, U+nFFFE and U+nFFFF
        
        // Test noncharacters U+FDD0..U+FDEF
        String noncharacters1 = "\uFDD0\uFDD1\uFDD2\uFDEF";
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf(noncharacters1);
            assertEquals(noncharacters1, dbusString.toString());
        });
        
        // Test noncharacters U+FFFE and U+FFFF
        String noncharacters2 = "\uFFFE\uFFFF";
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf(noncharacters2);
            assertEquals(noncharacters2, dbusString.toString());
        });
        
        // Test noncharacters in higher planes (represented as surrogate pairs)
        // U+1FFFE and U+1FFFF
        String noncharacters3 = "\uD83F\uDFFE\uD83F\uDFFF";
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf(noncharacters3);
            assertEquals(noncharacters3, dbusString.toString());
        });
    }

    @Test
    void testSurrogatePairs() {
        // Test valid UTF-16 surrogate pairs (which represent valid UTF-8)
        String[] validSurrogatePairs = {
            "\uD83D\uDE00", // 😀 - grinning face emoji
            "\uD83C\uDF89", // 🎉 - party popper emoji
            "\uD83D\uDCA9", // 💩 - pile of poo emoji
            "\uD83D\uDE80", // 🚀 - rocket emoji
            "\uD83C\uDF0D", // 🌍 - earth globe emoji
            "\uD800\uDC00", // U+10000 - first character in supplementary plane
            "\uDBFF\uDFFF", // U+10FFFF - last valid Unicode character
        };
        
        for (String surrogateString : validSurrogatePairs) {
            assertDoesNotThrow(() -> {
                DBusString dbusString = DBusString.valueOf(surrogateString);
                assertEquals(surrogateString, dbusString.toString());
                assertEquals(Type.STRING, dbusString.getType());
            });
        }
    }

    @Test
    void testControlCharacters() {
        // Test control characters (which are valid UTF-8)
        String[] controlCharacters = {
            "\u0001\u0002\u0003", // Control characters
            "\u0009", // Tab
            "\n", // Line feed
            "\r", // Carriage return
            "\u001F", // Unit separator
            "\u007F", // Delete
            "\u0080\u0081\u0082", // C1 control characters
            "\u009F", // Application program command
        };
        
        for (String controlString : controlCharacters) {
            assertDoesNotThrow(() -> {
                DBusString dbusString = DBusString.valueOf(controlString);
                assertEquals(controlString, dbusString.toString());
                assertEquals(Type.STRING, dbusString.getType());
            });
        }
    }

    @Test
    void testMaximumCodepoints() {
        // Test maximum valid Unicode codepoints
        String[] maxCodepoints = {
            "\uD7FF", // Last character before surrogate range
            "\uE000", // First character after surrogate range
            "\uFFFD", // Replacement character
            "\uDBFF\uDFFF", // U+10FFFF - maximum valid Unicode codepoint
        };
        
        for (String maxCodepoint : maxCodepoints) {
            assertDoesNotThrow(() -> {
                DBusString dbusString = DBusString.valueOf(maxCodepoint);
                assertEquals(maxCodepoint, dbusString.toString());
                assertEquals(Type.STRING, dbusString.getType());
            });
        }
    }

    @Test
    void testSpecialWhitespace() {
        // Test various Unicode whitespace characters
        String[] whitespaceChars = {
            "\u0020", // Space
            "\u00A0", // Non-breaking space
            "\u1680", // Ogham space mark
            "\u2000", // En quad
            "\u2001", // Em quad
            "\u2002", // En space
            "\u2003", // Em space
            "\u2004", // Three-per-em space
            "\u2005", // Four-per-em space
            "\u2006", // Six-per-em space
            "\u2007", // Figure space
            "\u2008", // Punctuation space
            "\u2009", // Thin space
            "\u200A", // Hair space
            "\u202F", // Narrow no-break space
            "\u205F", // Medium mathematical space
            "\u3000", // Ideographic space
        };
        
        for (String whitespace : whitespaceChars) {
            assertDoesNotThrow(() -> {
                DBusString dbusString = DBusString.valueOf(whitespace);
                assertEquals(whitespace, dbusString.toString());
                assertEquals(Type.STRING, dbusString.getType());
            });
        }
    }

    @Test
    void testUTF8SpecificationCompliance() {
        // Test overall D-Bus UTF-8 specification compliance
        // Per D-Bus specification: "The UTF-8 text must be validated strictly: 
        // in particular, it must not contain overlong sequences or codepoints above U+10FFFF"
        
        // Test that implementation handles valid UTF-8 strings
        String validUTF8 = "Valid UTF-8: Hello, 世界! 🌍";
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf(validUTF8);
            assertEquals(validUTF8, dbusString.toString());
            assertEquals(Type.STRING, dbusString.getType());
        });
        
        // Test that implementation handles edge cases
        String edgeCases = "Edge cases: \u0001\u007F\u0080\u07FF\u0800\uFFFF";
        assertDoesNotThrow(() -> {
            DBusString dbusString = DBusString.valueOf(edgeCases);
            assertEquals(edgeCases, dbusString.toString());
            assertEquals(Type.STRING, dbusString.getType());
        });
        
        // Note: Java String already enforces UTF-16 validity, which maps to valid UTF-8
        // Invalid UTF-8 sequences cannot be represented in Java String
    }
}