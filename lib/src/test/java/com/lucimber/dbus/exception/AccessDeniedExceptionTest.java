/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.type.DBusString;
import org.junit.jupiter.api.Test;

class AccessDeniedExceptionTest {

    @Test
    void testDefaultConstructor() {
        AccessDeniedException exception = new AccessDeniedException();

        assertEquals(
                "org.freedesktop.DBus.Error.AccessDenied", exception.getErrorName().toString());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessage() {
        DBusString message = DBusString.valueOf("Access denied to resource");
        AccessDeniedException exception = new AccessDeniedException(message);

        assertEquals(
                "org.freedesktop.DBus.Error.AccessDenied", exception.getErrorName().toString());
        assertEquals("Access denied to resource", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Security violation");
        AccessDeniedException exception = new AccessDeniedException(cause);

        assertEquals(
                "org.freedesktop.DBus.Error.AccessDenied", exception.getErrorName().toString());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        DBusString message = DBusString.valueOf("Access denied to resource");
        RuntimeException cause = new RuntimeException("Security violation");
        AccessDeniedException exception = new AccessDeniedException(message, cause);

        assertEquals(
                "org.freedesktop.DBus.Error.AccessDenied", exception.getErrorName().toString());
        assertEquals("Access denied to resource", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritanceFromAbstractException() {
        AccessDeniedException exception = new AccessDeniedException();
        assertTrue(exception instanceof AbstractException);
        assertTrue(exception instanceof Exception);
    }
}
