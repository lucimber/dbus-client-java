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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class CookieSaslMechanism implements SaslMechanism {

  private static final Logger LOGGER = LoggerFactory.getLogger(CookieSaslMechanism.class);
  private static final String DEFAULT_COOKIE_CONTEXT = "org_freedesktop_general"; // Common context

  private final String cookieContext;
  private String username;
  private String clientNonceHex; // Hex-encoded client nonce
  private boolean challengeProcessed = false;

  public CookieSaslMechanism() {
    this(DEFAULT_COOKIE_CONTEXT);
  }

  public CookieSaslMechanism(String cookieContext) {
    this.cookieContext = Objects.requireNonNull(cookieContext, "Cookie context cannot be null");
  }

  @Override
  public String getName() {
    return "DBUS_COOKIE_SHA1";
  }

  @Override
  public void init(ChannelHandlerContext ctx) throws SaslMechanismException {
    this.username = System.getProperty("user.name");
    if (this.username == null || this.username.isEmpty()) {
      throw new SaslMechanismException("Could not determine username for DBUS_COOKIE_SHA1.");
    }
    SecureRandom random = new SecureRandom();
    byte[] nonceBytes = new byte[16]; // 128 bits
    random.nextBytes(nonceBytes);
    this.clientNonceHex = SaslUtil.hexEncode(nonceBytes);
    LOGGER.debug("DBUS_COOKIE_SHA1 initialized for user: {}, client_nonce: {}", username, clientNonceHex);
  }

  @Override
  public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
    Promise<String> promise = ctx.executor().newPromise();
    EventExecutor worker = findWorkerExecutor(ctx);
    worker.execute(() -> {
      if (this.username == null) { // Should have been set by init
        promise.setFailure(new SaslMechanismException("Username not initialized for DBUS_COOKIE_SHA1."));
        return;
      }
      try {
        String hexEncodedUsername = SaslUtil.hexEncode(this.username.getBytes(StandardCharsets.UTF_8));
        promise.setSuccess(hexEncodedUsername);
      } catch (Exception e) {
        promise.setFailure(new SaslMechanismException("Failed to create initial response for DBUS_COOKIE_SHA1", e));
      }
    });
    return promise;
  }

  @Override
  public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challengeHex) {
    Promise<String> promise = ctx.executor().newPromise();
    EventExecutor worker = findWorkerExecutor(ctx);

    worker.execute(() -> {
      try {
        String challengeStr = new String(SaslUtil.hexDecode(challengeHex), StandardCharsets.UTF_8);
        String[] parts = challengeStr.split(" ");
        if (parts.length != 3) {
          promise.setFailure(new SaslMechanismException("Invalid DBUS_COOKIE_SHA1 challenge format. Expected 3 parts, got " + parts.length + ". Challenge: " + challengeStr));
          return;
        }
        String cookieId = parts[0]; // This is the actual ID, not hex
        String serverNonceHex = parts[1];
        String serverCookieValue = parts[2]; // This is the actual cookie value from server, not hex

        String localCookieValue = readLocalCookie(this.username, this.cookieContext, cookieId);
        if (localCookieValue == null) {
          promise.setFailure(new SaslMechanismException("Local cookie not found for ID: " + cookieId + " in context: " + this.cookieContext));
          return;
        }

        // Hash ingredients: <cookie_id_from_challenge>:<server_nonce_hex>:<client_nonce_hex>:<local_cookie_value>
        String stringToHash = cookieId + ":" + serverNonceHex + ":" + this.clientNonceHex + ":" + localCookieValue;

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
        String hexHash = SaslUtil.hexEncode(hashBytes);

        // Response: <client_nonce_hex> <hex_sha1_hash>
        String clientResponseStr = this.clientNonceHex + " " + hexHash;
        String hexEncodedClientResponse = SaslUtil.hexEncode(clientResponseStr.getBytes(StandardCharsets.UTF_8));

        promise.setSuccess(hexEncodedClientResponse);
        this.challengeProcessed = true;

      } catch (NoSuchAlgorithmException e) {
        promise.setFailure(new SaslMechanismException("SHA-1 algorithm not available for DBUS_COOKIE_SHA1", e));
      } catch (Exception e) {
        promise.setFailure(new SaslMechanismException("Failed to process DBUS_COOKIE_SHA1 challenge", e));
      }
    });
    return promise;
  }

  private String readLocalCookie(String userName, String contextName, String cookieId) throws IOException {
    // This path is typical for system bus user keyrings. Session bus might differ.
    // User-specific directory: ~/.dbus-keyrings/
    // Filename inside is often the cookie ID.
    // Content: <cookie_id> <creation_timestamp_unix> <cookie_secret_value>
    Path cookieDirPath = Paths.get(System.getProperty("user.home"), ".dbus-keyrings");
    // Some systems might use a subdirectory per context (though often just "session" or "user")
    // For "org_freedesktop_general", it might be directly in .dbus-keyrings or a subdir.
    // Let's try a common structure if context is general, or use context as subdir.
    Path contextPath;
    if (DEFAULT_COOKIE_CONTEXT.equals(contextName) || "session".equals(contextName) || "user".equals(contextName)) {
      contextPath = cookieDirPath; // Try root of .dbus-keyrings for common contexts
    } else {
      contextPath = cookieDirPath.resolve(contextName); // Or specific context subdir
    }
    Path cookieFilePath = contextPath.resolve(cookieId);

    LOGGER.debug("Attempting to read cookie for user '{}', context '{}', id '{}' from: {}",
          userName, contextName, cookieId, cookieFilePath.toAbsolutePath());

    if (Files.exists(cookieFilePath) && Files.isReadable(cookieFilePath)) {
      try (BufferedReader reader = Files.newBufferedReader(cookieFilePath, StandardCharsets.UTF_8)) {
        String line = reader.readLine();
        if (line != null) {
          String[] parts = line.trim().split(" ", 3); // Split into 3 parts max
          if (parts.length == 3 && parts[0].equals(cookieId)) {
            LOGGER.debug("Successfully read cookie value for id '{}'", cookieId);
            return parts[2]; // The cookie secret value
          } else {
            LOGGER.warn("Cookie file {} has unexpected format or mismatched ID. Line: {}", cookieFilePath, line);
          }
        } else {
          LOGGER.warn("Cookie file {} is empty.", cookieFilePath);
        }
      }
    } else {
      LOGGER.warn("Cookie file not found or not readable: {}", cookieFilePath.toAbsolutePath());
    }
    return null;
  }


  @Override
  public boolean isComplete() {
    return challengeProcessed;
  }

  @Override
  public void dispose() {
    this.username = null;
    this.clientNonceHex = null;
    // No other long-lived sensitive data specific to a single auth attempt
  }

  private EventExecutor findWorkerExecutor(ChannelHandlerContext ctx) {
    return GlobalEventExecutor.INSTANCE; // Simplification; use a dedicated pool in production
  }
}
