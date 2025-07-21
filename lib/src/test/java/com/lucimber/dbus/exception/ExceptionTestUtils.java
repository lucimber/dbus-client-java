/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class for testing D-Bus exception classes.
 */
class ExceptionTestUtils {

    /**
     * Tests all standard constructors of a D-Bus exception class.
     */
    static <T extends AbstractException> void testExceptionClass(Class<T> exceptionClass, String expectedErrorName) {
        try {
            // Test default constructor
            Constructor<T> defaultConstructor = exceptionClass.getConstructor();
            T exception1 = defaultConstructor.newInstance();
            assertEquals(expectedErrorName, exception1.getErrorName().toString());
            assertNull(exception1.getMessage());
            assertNull(exception1.getCause());
            
            // Test constructor with message
            Constructor<T> messageConstructor = exceptionClass.getConstructor(DBusString.class);
            DBusString message = DBusString.valueOf("Test error message");
            T exception2 = messageConstructor.newInstance(message);
            assertEquals(expectedErrorName, exception2.getErrorName().toString());
            assertEquals("Test error message", exception2.getMessage());
            assertNull(exception2.getCause());
            
            // Test constructor with cause
            Constructor<T> causeConstructor = exceptionClass.getConstructor(Throwable.class);
            RuntimeException cause = new RuntimeException("Test cause");
            T exception3 = causeConstructor.newInstance(cause);
            assertEquals(expectedErrorName, exception3.getErrorName().toString());
            assertEquals(cause, exception3.getCause());
            
            // Test constructor with message and cause
            Constructor<T> fullConstructor = exceptionClass.getConstructor(DBusString.class, Throwable.class);
            T exception4 = fullConstructor.newInstance(message, cause);
            assertEquals(expectedErrorName, exception4.getErrorName().toString());
            assertEquals("Test error message", exception4.getMessage());
            assertEquals(cause, exception4.getCause());
            
            // Test inheritance
            assertTrue(exception1 instanceof AbstractException);
            assertTrue(exception1 instanceof Exception);
            
        } catch (Exception e) {
            fail("Failed to test exception class " + exceptionClass.getName() + ": " + e.getMessage());
        }
    }
}