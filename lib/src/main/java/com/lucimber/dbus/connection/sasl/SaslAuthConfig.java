/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.sasl;

/**
 * This interface declares the common properties of the different SASL configuration types. A
 * concrete SASL configuration must implement this interface.
 */
public interface SaslAuthConfig {
    /**
     * Gets the authentication mechanism.
     *
     * @return the mechanism as a {@link SaslAuthMechanism}
     */
    SaslAuthMechanism getAuthMechanism();
}
