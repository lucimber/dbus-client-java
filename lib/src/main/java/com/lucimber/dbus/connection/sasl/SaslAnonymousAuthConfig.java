/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

/**
 * The {@code ANONYMOUS} mechanism is defined in RFC 4505
 * "Anonymous Simple Authentication and Security Layer (SASL) Mechanism".
 * It does not perform any authentication at all, and should not be accepted by message buses.
 * However, it might sometimes be useful for non-message-bus uses of D-Bus.
 */
public final class SaslAnonymousAuthConfig implements SaslAuthConfig {
  @Override
  public SaslAuthMechanism getAuthMechanism() {
    return SaslAuthMechanism.ANONYMOUS;
  }

  @Override
  public String toString() {
    return "SaslAnonymousAuthConfig{}";
  }
}
