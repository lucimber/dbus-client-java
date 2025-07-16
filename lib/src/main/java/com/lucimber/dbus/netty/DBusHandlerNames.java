/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

/**
 * Utility class containing standardized names for D-Bus pipeline handlers.
 * This ensures consistent naming across the pipeline for handler management
 * and reconnection scenarios.
 */
public final class DBusHandlerNames {

  // SASL-related handler names
  public static final String SASL_INITIATION_HANDLER = "SaslInitiationHandler";
  public static final String SASL_CODEC = "SaslCodec";
  public static final String SASL_AUTHENTICATION_HANDLER = "SaslAuthenticationHandler";
  
  // D-Bus protocol handler names
  public static final String NETTY_BYTE_LOGGER = "NettyByteLogger";
  public static final String FRAME_ENCODER = "FrameEncoder";
  public static final String OUTBOUND_MESSAGE_ENCODER = "OutboundMessageEncoder";
  public static final String FRAME_DECODER = "FrameDecoder";
  public static final String INBOUND_MESSAGE_DECODER = "InboundMessageDecoder";
  public static final String DBUS_MANDATORY_NAME_HANDLER = "DBusMandatoryNameHandler";
  public static final String CONNECTION_COMPLETION_HANDLER = "ConnectionCompletionHandler";
  
  // SASL sub-handler names (managed by SaslCodec)
  public static final String SASL_MESSAGE_DECODER = "SaslMessageDecoder";
  public static final String SASL_MESSAGE_ENCODER = "SaslMessageEncoder";

  private DBusHandlerNames() {
    // Utility class - no instances
  }
}