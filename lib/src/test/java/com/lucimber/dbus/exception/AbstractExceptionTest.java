/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import org.junit.jupiter.api.Test;

import com.lucimber.dbus.type.DBusString;

import static org.junit.jupiter.api.Assertions.*;

class AbstractExceptionTest {

    private static class TestException extends AbstractException {
        public TestException(DBusString errorName) {
            super(errorName);
        }

        public TestException(DBusString errorName, DBusString message) {
            super(errorName, message);
        }

        public TestException(DBusString errorName, Throwable cause) {
            super(errorName, cause);
        }

        public TestException(DBusString errorName, DBusString message, Throwable cause) {
            super(errorName, message, cause);
        }
    }

    @Test
    void testConstructorWithErrorName() {
        DBusString errorName = DBusString.valueOf("org.example.Error.Test");
        TestException exception = new TestException(errorName);

        assertEquals(errorName, exception.getErrorName());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithErrorNameAndMessage() {
        DBusString errorName = DBusString.valueOf("org.example.Error.Test");
        DBusString message = DBusString.valueOf("Test error message");
        TestException exception = new TestException(errorName, message);

        assertEquals(errorName, exception.getErrorName());
        assertEquals("Test error message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithErrorNameAndCause() {
        DBusString errorName = DBusString.valueOf("org.example.Error.Test");
        RuntimeException cause = new RuntimeException("Underlying cause");
        TestException exception = new TestException(errorName, cause);

        assertEquals(errorName, exception.getErrorName());
        assertEquals(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Underlying cause", exception.getMessage());
    }

    @Test
    void testConstructorWithAllParameters() {
        DBusString errorName = DBusString.valueOf("org.example.Error.Test");
        DBusString message = DBusString.valueOf("Test error message");
        RuntimeException cause = new RuntimeException("Underlying cause");
        TestException exception = new TestException(errorName, message, cause);

        assertEquals(errorName, exception.getErrorName());
        assertEquals("Test error message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testNullErrorNameThrowsException() {
        assertThrows(NullPointerException.class, () -> new TestException(null));
        assertThrows(
                NullPointerException.class,
                () -> new TestException(null, DBusString.valueOf("message")));
        assertThrows(
                NullPointerException.class, () -> new TestException(null, new RuntimeException()));
        assertThrows(
                NullPointerException.class,
                () ->
                        new TestException(
                                null, DBusString.valueOf("message"), new RuntimeException()));
    }
}
