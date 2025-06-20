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

import java.nio.charset.StandardCharsets;

public class ExternalSaslMechanism implements SaslMechanism {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalSaslMechanism.class);
  private String authorizationId; // UID as hex string
  private boolean complete = false;

  @Override
  public String getName() {
    return "EXTERNAL";
  }

  @Override
  public void init(ChannelHandlerContext ctx) throws SaslMechanismException {
    // For DBus, EXTERNAL typically means sending the UID of the client process.
    // This is a simplified way to get UID. A robust solution is platform-specific (JNI/JNA).
    try {
      // Attempt to get UID using a common (but not universally portable) approach
      // This part might need significant enhancement for production.
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
        // Try to get UID via com.sun.security.auth.module.UnixSystem (if available)
        // This class is internal and its availability/behavior can vary.
        try {
          Class<?> unixSystemClass = Class.forName("com.sun.security.auth.module.UnixSystem");
          Object unixSystemInstance = unixSystemClass.getDeclaredConstructor().newInstance();
          long uid = (long) unixSystemClass.getMethod("getUid").invoke(unixSystemInstance);
          this.authorizationId = Long.toString(uid); // UID as decimal string
        } catch (Exception reflectionEx) {
          LOGGER.warn("Failed to get UID via UnixSystem, falling back to user.name for EXTERNAL. Error: {}", reflectionEx.getMessage());
          // Fallback to username if UID retrieval fails or is not applicable
          this.authorizationId = System.getProperty("user.name", "unknownuser");
        }
      } else {
        // For non-Unix-like systems, EXTERNAL might use username or other principal.
        this.authorizationId = System.getProperty("user.name", "unknownuser");
        LOGGER.warn("EXTERNAL mechanism on non-Unix OS, using username: {}", this.authorizationId);
      }

      if (this.authorizationId == null || this.authorizationId.isEmpty()) {
        throw new SaslMechanismException("Could not determine authorization ID (UID/username) for EXTERNAL mechanism.");
      }
      LOGGER.debug("EXTERNAL mechanism initialized with authorizationId: {}", this.authorizationId);

    } catch (Exception e) {
      throw new SaslMechanismException("Failed to initialize EXTERNAL mechanism", e);
    }
  }

  @Override
  public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
    Promise<String> promise = ctx.executor().newPromise();
    // The authorizationId itself is sent, hex-encoded.
    EventExecutor worker = findWorkerExecutor(ctx); // Trivial for string encoding, but good pattern
    worker.execute(() -> {
      // TODO: Move logic of gathering username from init method to here...
      if (this.authorizationId == null) {
        promise.setFailure(new SaslMechanismException("Authorization ID not initialized for EXTERNAL."));
        return;
      }
      try {
        String hexEncodedAuthzId = SaslUtil.hexEncode(this.authorizationId.getBytes(StandardCharsets.UTF_8));
        promise.setSuccess(hexEncodedAuthzId);
        this.complete = true; // EXTERNAL is one-shot from client
      } catch (Exception e) {
        promise.setFailure(new SaslMechanismException("Failed to create initial response for EXTERNAL", e));
      }
    });
    return promise;
  }

  @Override
  public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challenge) {
    // EXTERNAL mechanism typically does not involve challenges from the server.
    Promise<String> promise = ctx.executor().newPromise();
    promise.setFailure(new SaslMechanismException("EXTERNAL mechanism does not support challenges. Received: " + challenge));
    return promise;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public void dispose() {
    this.authorizationId = null;
  }

  private EventExecutor findWorkerExecutor(ChannelHandlerContext ctx) {
    // For real applications, inject or look up a dedicated worker EventExecutorGroup
    return GlobalEventExecutor.INSTANCE; // Simplification
  }
}
