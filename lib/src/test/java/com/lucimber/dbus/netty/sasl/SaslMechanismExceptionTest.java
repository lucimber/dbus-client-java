/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaslMechanismExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "SASL authentication failed";
        SaslMechanismException exception = new SaslMechanismException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessage() {
        SaslMechanismException exception = new SaslMechanismException(null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        String message = "";
        SaslMechanismException exception = new SaslMechanismException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "SASL authentication failed";
        RuntimeException cause = new RuntimeException("Underlying cause");
        SaslMechanismException exception = new SaslMechanismException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithNullMessageAndCause() {
        RuntimeException cause = new RuntimeException("Underlying cause");
        SaslMechanismException exception = new SaslMechanismException(null, cause);
        
        assertNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndNullCause() {
        String message = "SASL authentication failed";
        SaslMechanismException exception = new SaslMechanismException(message, null);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessageAndNullCause() {
        SaslMechanismException exception = new SaslMechanismException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testInheritanceFromException() {
        SaslMechanismException exception = new SaslMechanismException("test");
        
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(SaslMechanismException.class, () -> {
            throw new SaslMechanismException("Test exception");
        });
    }

    @Test
    void testExceptionCanBeCaught() {
        try {
            throw new SaslMechanismException("Test exception");
        } catch (SaslMechanismException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }
}