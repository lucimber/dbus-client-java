/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal implementation of {@link Context} used for doubly-linked pipeline structure.
 * Only used within {@link DefaultPipeline}.
 */
final class InternalContext implements Context {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalContext.class);
  private final Connection connection;
  private final DefaultPipeline pipeline;
  private final String name;
  private final Handler handler;
  private InternalContext prev;
  private InternalContext next;

  InternalContext(Connection connection, DefaultPipeline pipeline, String name, Handler handler) {
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
    this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.handler = Objects.requireNonNull(handler, "handler must not be null");
  }

  public InternalContext getNext() {
    return next;
  }

  public void setNext(InternalContext next) {
    this.next = next;
  }

  public InternalContext getPrev() {
    return prev;
  }

  public void setPrev(InternalContext prev) {
    this.prev = prev;
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Handler getHandler() {
    return handler;
  }

  @Override
  public void propagateConnectionActive() {
    if (isRemoved()) {
      String msg = "Not propagating connection-active event, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(msg);
    }

    InternalContext ctx = getNext();
    if (ctx == null) {
      LOGGER.debug("Connection-active event received, evaluated, and ceremonially ignored. "
              + "It's now haunting /dev/null.");
    } else {
      ctx.onConnectionActive();
    }
  }

  @Override
  public void propagateConnectionInactive() {
    if (isRemoved()) {
      String msg = "Not propagating connection-inactive event, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(msg);
    }

    InternalContext ctx = getNext();
    if (ctx == null) {
      LOGGER.debug("Connection-inactive event received, evaluated, and ceremonially ignored. "
              + "It's now haunting /dev/null.");
    } else {
      ctx.onConnectionInactive();
    }
  }

  @Override
  public void propagateInboundMessage(InboundMessage msg) {
    if (isRemoved()) {
      String s = "Not propagating inbound message, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(s);
    }

    InternalContext ctx = getNext();
    if (ctx == null) {
      LOGGER.warn("Inbound message received, evaluated, and ceremonially ignored. "
              + "It's now haunting /dev/null.");
    } else {
      ctx.handleInboundMessage(msg);
    }
  }

  @Override
  public void propagateOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future) {
    if (isRemoved()) {
      String s = "Not propagating outbound message, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(s);
    }

    InternalContext ctx = getPrev();
    if (ctx == null) {
      throw new IllegalStateException("Cannot propagate an outbound message any further on this pipeline. "
              + "Please execute handleOutboundMessage on this context instead.");
    } else {
      ctx.handleOutboundMessage(msg, future);
    }
  }

  @Override
  public void propagateInboundFailure(Throwable cause) {
    if (isRemoved()) {
      String msg = "Not propagating inbound failure, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(msg);
    }

    InternalContext ctx = getNext();
    if (ctx == null) {
      LOGGER.warn("Inbound failure received, evaluated, and ceremonially ignored. "
              + "It's now haunting /dev/null.");
    } else {
      ctx.handleInboundFailure(cause);
    }
  }

  @Override
  public void propagateUserEvent(Object evt) {
    if (isRemoved()) {
      String msg = "Not propagating user-defined event, "
              + "because this handler is removed from the pipeline.";
      throw new IllegalArgumentException(msg);
    }

    InternalContext ctx = getNext();
    if (ctx == null) {
      LOGGER.debug("User-defined event received, evaluated, and ceremonially ignored. "
              + "It's now haunting /dev/null.");
    } else {
      ctx.handleUserEvent(evt);
    }
  }

  @Override
  public boolean isRemoved() {
    return next == null && prev == null;
  }

  public void handleInboundMessage(InboundMessage msg) {
    if (handler instanceof InboundHandler inboundHandler) {
      try {
        inboundHandler.handleInboundMessage(this, msg);
      } catch (Throwable t) {
        handleInboundFailure(t);
      }
    } else {
      propagateInboundMessage(msg);
    }
  }

  public void handleOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future) {
    if (handler instanceof OutboundHandler outboundHandler) {
      try {
        outboundHandler.handleOutboundMessage(this, msg, future);
      } catch (Throwable t) {
        LOGGER.error("Outbound message event fumbled into chaos. "
                + "Terminating the connection with prejudice.");
        try {
          pipeline.getConnection().close();
        } catch (Exception ignored) {
          // Intentionally ignoring exception during emergency close
        }
      }
    } else {
      propagateOutboundMessage(msg, future);
    }
  }

  public void handleInboundFailure(Throwable cause) {
    if (handler instanceof InboundHandler inboundHandler) {
      try {
        inboundHandler.handleInboundFailure(this, cause);
      } catch (Throwable t) {
        LOGGER.error("Failure caused by inbound message event fumbled into chaos. "
                + "Recovery was a nice idea. "
                + "Terminating the connection with prejudice.");
        try {
          pipeline.getConnection().close();
        } catch (Exception ignored) {
          // Intentionally ignoring exception during emergency close
        }
      }
    } else {
      propagateInboundFailure(cause);
    }
  }

  public void handleUserEvent(Object evt) {
    try {
      handler.handleUserEvent(this, evt);
    } catch (Throwable t) {
      LOGGER.error("User-defined event fumbled into chaos. "
              + "Terminating the connection with prejudice.");
      try {
        pipeline.getConnection().close();
      } catch (Exception ignored) {
        // Intentionally ignoring exception during emergency close
      }
    }
  }

  public void onConnectionActive() {
    try {
      handler.onConnectionActive(this);
    } catch (Throwable t) {
      LOGGER.error("Connection-active event fumbled into chaos. "
              + "Terminating the connection with prejudice.");
      try {
        pipeline.getConnection().close();
      } catch (Exception ignored) {
        // Intentionally ignoring exception during emergency close
      }
    }
  }

  public void onConnectionInactive() {
    try {
      handler.onConnectionInactive(this);
    } catch (Throwable t) {
      LOGGER.error("Connection-inactive event fumbled into chaos. "
              + "Terminating the connection with prejudice.");
      try {
        pipeline.getConnection().close();
      } catch (Exception ignored) {
        // Intentionally ignoring exception during emergency close
      }
    }
  }
}
