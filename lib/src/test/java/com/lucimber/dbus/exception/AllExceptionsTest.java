/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import org.junit.jupiter.api.Test;

/** Test suite for all D-Bus exception classes. */
class AllExceptionsTest {

    @Test
    void testAuthFailedException() {
        ExceptionTestUtils.testExceptionClass(
                AuthFailedException.class, "org.freedesktop.DBus.Error.AuthFailed");
    }

    @Test
    void testBadAddressException() {
        ExceptionTestUtils.testExceptionClass(
                BadAddressException.class, "org.freedesktop.DBus.Error.BadAddress");
    }

    @Test
    void testDisconnectedException() {
        ExceptionTestUtils.testExceptionClass(
                DisconnectedException.class, "org.freedesktop.DBus.Error.Disconnected");
    }

    @Test
    void testFailedException() {
        ExceptionTestUtils.testExceptionClass(
                FailedException.class, "org.freedesktop.DBus.Error.Failed");
    }

    @Test
    void testFileExistsException() {
        ExceptionTestUtils.testExceptionClass(
                FileExistsException.class, "org.freedesktop.DBus.Error.FileExists");
    }

    @Test
    void testFileNotFoundException() {
        ExceptionTestUtils.testExceptionClass(
                FileNotFoundException.class, "org.freedesktop.DBus.Error.FileNotFound");
    }

    @Test
    void testInconsistentMessageException() {
        ExceptionTestUtils.testExceptionClass(
                InconsistentMessageException.class,
                "org.freedesktop.DBus.Error.InconsistentMessage");
    }

    @Test
    void testInteractiveAuthorizationRequiredException() {
        ExceptionTestUtils.testExceptionClass(
                InteractiveAuthorizationRequiredException.class,
                "org.freedesktop.DBus.Error.InteractiveAuthorizationRequired");
    }

    @Test
    void testInvalidArgsException() {
        ExceptionTestUtils.testExceptionClass(
                InvalidArgsException.class, "org.freedesktop.DBus.Error.InvalidArgs");
    }

    @Test
    void testInvalidSignatureException() {
        ExceptionTestUtils.testExceptionClass(
                InvalidSignatureException.class, "org.freedesktop.DBus.Error.InvalidSignature");
    }

    @Test
    void testIOErrorException() {
        ExceptionTestUtils.testExceptionClass(
                IOErrorException.class, "org.freedesktop.DBus.Error.IOError");
    }

    @Test
    void testLimitsExceededException() {
        ExceptionTestUtils.testExceptionClass(
                LimitsExceededException.class, "org.freedesktop.DBus.Error.LimitsExceeded");
    }

    @Test
    void testMatchRuleInvalidException() {
        ExceptionTestUtils.testExceptionClass(
                MatchRuleInvalidException.class, "org.freedesktop.DBus.Error.MatchRuleInvalid");
    }

    @Test
    void testMatchRuleNotFoundException() {
        ExceptionTestUtils.testExceptionClass(
                MatchRuleNotFoundException.class, "org.freedesktop.DBus.Error.MatchRuleNotFound");
    }

    @Test
    void testNameHasNoOwnerException() {
        ExceptionTestUtils.testExceptionClass(
                NameHasNoOwnerException.class, "org.freedesktop.DBus.Error.NameHasNoOwner");
    }

    @Test
    void testNoMemoryException() {
        ExceptionTestUtils.testExceptionClass(
                NoMemoryException.class, "org.freedesktop.DBus.Error.NoMemory");
    }

    @Test
    void testNoNetworkException() {
        ExceptionTestUtils.testExceptionClass(
                NoNetworkException.class, "org.freedesktop.DBus.Error.NoNetwork");
    }

    @Test
    void testNoReplyException() {
        ExceptionTestUtils.testExceptionClass(
                NoReplyException.class, "org.freedesktop.DBus.Error.NoReply");
    }

    @Test
    void testNoServerException() {
        ExceptionTestUtils.testExceptionClass(
                NoServerException.class, "org.freedesktop.DBus.Error.NoServer");
    }

    @Test
    void testNotSupportedException() {
        ExceptionTestUtils.testExceptionClass(
                NotSupportedException.class, "org.freedesktop.DBus.Error.NotSupported");
    }

    @Test
    void testPropertyReadOnlyException() {
        ExceptionTestUtils.testExceptionClass(
                PropertyReadOnlyException.class, "org.freedesktop.DBus.Error.PropertyReadOnly");
    }

    @Test
    void testServiceUnknownException() {
        ExceptionTestUtils.testExceptionClass(
                ServiceUnknownException.class, "org.freedesktop.DBus.Error.ServiceUnknown");
    }

    @Test
    void testTimeoutException() {
        ExceptionTestUtils.testExceptionClass(
                TimeoutException.class, "org.freedesktop.DBus.Error.Timeout");
    }

    @Test
    void testUnixProcessIdUnknownException() {
        ExceptionTestUtils.testExceptionClass(
                UnixProcessIdUnknownException.class,
                "org.freedesktop.DBus.Error.UnixProcessIdUnknown");
    }

    @Test
    void testUnknownInterfaceException() {
        ExceptionTestUtils.testExceptionClass(
                UnknownInterfaceException.class, "org.freedesktop.DBus.Error.UnknownInterface");
    }

    @Test
    void testUnknownMethodException() {
        ExceptionTestUtils.testExceptionClass(
                UnknownMethodException.class, "org.freedesktop.DBus.Error.UnknownMethod");
    }

    @Test
    void testUnknownObjectException() {
        ExceptionTestUtils.testExceptionClass(
                UnknownObjectException.class, "org.freedesktop.DBus.Error.UnknownObject");
    }

    @Test
    void testUnknownPropertyException() {
        ExceptionTestUtils.testExceptionClass(
                UnknownPropertyException.class, "org.freedesktop.DBus.Error.UnknownProperty");
    }

    @Test
    void testAddressInUseException() {
        ExceptionTestUtils.testExceptionClass(
                AddressInUseException.class, "org.freedesktop.DBus.Error.AddressInUse");
    }
}
