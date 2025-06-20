/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

public enum DbusChannelEvent {
  /**
   * Fired by {@link SaslInitiationHandler} after the initial NUL byte has been successfully sent.
   * Signals that the {@link SaslAuthenticationHandler} can begin the AUTH exchange.
   */
  SASL_NUL_BYTE_SENT,
  /**
   * Fired by {@link SaslAuthenticationHandler} when SASL authentication has successfully completed.
   * The DBus message handlers (FrameDecoder, etc.) can now be added to the pipeline.
   */
  SASL_AUTH_COMPLETE,
  /**
   * Fired by {@link SaslAuthenticationHandler} when SASL authentication has failed.
   * The connection should likely be closed.
   */
  SASL_AUTH_FAILED,
  /**
   * Fired when the core DBus de/encoders and {@link DBusMandatoryNameHandler} are set up.
   * {@link DBusMandatoryNameHandler} listens for this to send the Hello call.
   */
  DBUS_PIPELINE_READY,
  /**
   * Fired by {@link DBusMandatoryNameHandler} when the app's unique bus name
   * has been successfully acquired via the Hello() method call.
   */
  MANDATORY_NAME_ACQUIRED,
  /**
   * Fired by {@link DBusMandatoryNameHandler} if acquiring the mandatory bus name fails.
   */
  MANDATORY_NAME_ACQUISITION_FAILED
}
