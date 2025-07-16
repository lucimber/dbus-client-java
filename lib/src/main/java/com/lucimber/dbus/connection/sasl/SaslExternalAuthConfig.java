/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

import java.util.Objects;

/**
 * The {@code EXTERNAL} mechanism is defined in RFC 4422 "Simple Authentication and Security Layer (SASL)",
 * appendix A "The SASL EXTERNAL Mechanism". This is the recommended authentication mechanism on platforms
 * where credentials can be transferred out-of-band, in particular Unix platforms that can perform
 * credentials-passing over the unix: transport.
 *
 * <p>On Unix platforms, interoperable clients should prefer to send the ASCII decimal string form
 * of the integer Unix user ID as the authorization identity, for example 1000.
 * When encoded in hex by the authentication protocol, this will typically result in a line like
 * {@code AUTH EXTERNAL 31303030} followed by \r\n.
 *
 * <p>On Windows platforms, clients that use the {@code EXTERNAL} mechanism should use
 * the Windows security identifier in its string form as the authorization identity,
 * for example {@literal S-1-5-21-3623811015-3361044348-30300820-1013 } for a domain
 * or local computer user or {@literal S-1-5-18} for the {@literal LOCAL_SYSTEM} user.
 * When encoded in hex by the authentication protocol,
 * this will typically result in a line like {@code AUTH EXTERNAL 532d312d352d3138} followed by \r\n.
 */
public final class SaslExternalAuthConfig implements SaslAuthConfig {

  private final String identity;

  public SaslExternalAuthConfig(final String identity) {
    this.identity = Objects.requireNonNull(identity);
  }

  @Override
  public SaslAuthMechanism getAuthMechanism() {
    return SaslAuthMechanism.EXTERNAL;
  }

  public String getIdentity() {
    return identity;
  }
}
