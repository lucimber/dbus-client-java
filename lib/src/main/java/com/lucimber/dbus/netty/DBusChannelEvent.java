/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler;
import com.lucimber.dbus.netty.sasl.SaslInitiationHandler;

public enum DBusChannelEvent {
    /**
     * Fired by {@link SaslInitiationHandler} after the initial NUL byte has been successfully sent.
     */
    SASL_NUL_BYTE_SENT,
    /**
     * Fired by {@link SaslAuthenticationHandler} when SASL authentication has successfully
     * completed.
     */
    SASL_AUTH_COMPLETE,
    /** Fired by {@link SaslAuthenticationHandler} when SASL authentication has failed. */
    SASL_AUTH_FAILED,
    /**
     * Fired by {@link DBusMandatoryNameHandler} when the app's unique bus name has been
     * successfully acquired via the Hello() method call.
     */
    MANDATORY_NAME_ACQUIRED,
    /** Fired by {@link DBusMandatoryNameHandler} if acquiring the mandatory bus name fails. */
    MANDATORY_NAME_ACQUISITION_FAILED,

    /**
     * Fired when a reconnection process is about to start. Handlers should prepare for reconnection
     * by resetting their state.
     */
    RECONNECTION_STARTING,

    /**
     * Fired when handlers that were removed during connection setup need to be re-added. This
     * allows for proper pipeline reconstruction during reconnection.
     */
    RECONNECTION_HANDLERS_READD_REQUIRED
}
