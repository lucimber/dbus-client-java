/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

public interface SaslMechanism {
    String getName();

    void init(ChannelHandlerContext ctx) throws SaslMechanismException;

    Future<String> getInitialResponseAsync(ChannelHandlerContext ctx);

    Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challenge);

    boolean isComplete(); // True if client-side steps are done (may still await server OK)

    void dispose(); // Clear sensitive data
}
