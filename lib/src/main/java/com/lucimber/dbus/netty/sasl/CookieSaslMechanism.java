/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class CookieSaslMechanism implements SaslMechanism {

  private static final Logger LOGGER = LoggerFactory.getLogger(CookieSaslMechanism.class);

  private String username;
  private String clientChallenge;
  private boolean completed = false;

  @Override
  public String getName() {
    return "DBUS_COOKIE_SHA1";
  }

  @Override
  public void init(ChannelHandlerContext ctx) throws SaslMechanismException {
    username = System.getProperty("user.name");
    if (username == null || username.isEmpty()) {
      throw new SaslMechanismException("Username could not be determined.");
    }
    byte[] nonce = new byte[16];
    new SecureRandom().nextBytes(nonce);
    clientChallenge = SaslUtil.hexEncode(nonce);
  }

  @Override
  public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
    Promise<String> promise = ctx.executor().newPromise();
    findWorkerExecutor(ctx).execute(() -> {
      try {
        String hexUsername = SaslUtil.hexEncode(username.getBytes(StandardCharsets.UTF_8));
        promise.setSuccess(hexUsername);
      } catch (Exception e) {
        promise.setFailure(new SaslMechanismException("Failed to encode username.", e));
      }
    });
    return promise;
  }

  @Override
  public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challengeHex) {
    Promise<String> promise = ctx.executor().newPromise();
    findWorkerExecutor(ctx).execute(() -> {
      try {
        String decoded = new String(SaslUtil.hexDecode(challengeHex), StandardCharsets.UTF_8);
        String[] parts = decoded.split(" ");
        if (parts.length != 3) {
          promise.setFailure(new SaslMechanismException("Invalid server challenge format. Expected 3 fields."));
          return;
        }

        String context = parts[0];
        String cookieId = parts[1];
        String serverChallenge = parts[2];

        String cookieValue = readCookieValue(context, cookieId);
        if (cookieValue == null) {
          promise.setFailure(new SaslMechanismException("Cookie not found for id: " + cookieId));
          return;
        }

        String combined = serverChallenge + ":" + clientChallenge + ":" + cookieValue;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        String responseHash = SaslUtil.hexEncode(digest.digest(combined.getBytes(StandardCharsets.UTF_8)));

        String response = clientChallenge + " " + responseHash;
        String hexResponse = SaslUtil.hexEncode(response.getBytes(StandardCharsets.UTF_8));
        completed = true;
        promise.setSuccess(hexResponse);
      } catch (Exception e) {
        promise.setFailure(new SaslMechanismException("Error processing server challenge.", e));
      }
    });
    return promise;
  }

  private String readCookieValue(String context, String cookieId) throws IOException {
    Path cookieFile = Paths.get(System.getProperty("user.home"), ".dbus-keyrings", context);
    if (!Files.exists(cookieFile) || !Files.isReadable(cookieFile)) {
      LOGGER.warn("Cookie file not found or unreadable: {}", cookieFile);
      return null;
    }

    try (BufferedReader reader = Files.newBufferedReader(cookieFile, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.trim().split(" ", 3);
        if (parts.length == 3 && parts[0].equals(cookieId)) {
          return parts[2];
        }
      }
    }

    LOGGER.warn("No matching cookie ID {} in file {}", cookieId, cookieFile);
    return null;
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public void dispose() {
    username = null;
    clientChallenge = null;
    completed = false;
  }

  private EventExecutor findWorkerExecutor(ChannelHandlerContext ctx) {
    return GlobalEventExecutor.INSTANCE;
  }
}

