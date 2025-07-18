/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.InboundHandler;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The critical bridge between the Netty pipeline and public API pipeline systems.
 * 
 * <p>This handler serves as the reality checkpoint where messages transition between 
 * the low-level transport layer (Netty) and the high-level application layer (public API).
 * It manages request-response correlation, timeouts, and ensures proper thread isolation
 * by routing messages to the appropriate execution contexts. All inbound messages and
 * connection events are switched from the Netty EventLoop to the ApplicationTaskExecutor
 * here, enabling safe blocking operations in user handlers.</p>
 * 
 * <p>Named after the brilliant drum & bass track "Reality Checkpoint" by Logistics,
 * because like that track, this class represents a moment of transition and clarity
 * in the complex flow of D-Bus message processing.</p>
 */
public class RealityCheckpoint extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RealityCheckpoint.class);
  private static final long DEFAULT_METHOD_CALL_TIMEOUT_MS = 30_000; // 30 seconds

  private final ConcurrentHashMap<DBusUInt32, PendingMethodCall> pendingMethodCalls;
  private final ExecutorService applicationTaskExecutor; // For offloading user code
  private final Connection connection;
  private final long methodCallTimeoutMs;
  private ChannelHandlerContext ctx;

  /**
   * Creates a new instance.
   *
   * @param applicationTaskExecutor The executor service to run application-level callbacks
   *                                (like signal handlers) on, to avoid blocking the Netty EventLoop.
   *                                If null, a default will be attempted or tasks run on EventLoop
   *                                (not recommended for blocking user code).
   * @param connection              An active D-Bus connection.
   */
  public RealityCheckpoint(ExecutorService applicationTaskExecutor, Connection connection) {
    this(applicationTaskExecutor, connection, DEFAULT_METHOD_CALL_TIMEOUT_MS);
  }

  /**
   * Creates a new instance with custom timeout.
   *
   * @param applicationTaskExecutor The executor service to run application-level callbacks
   *                                (like signal handlers) on, to avoid blocking the Netty EventLoop.
   *                                If null, a default will be attempted or tasks run on EventLoop
   *                                (not recommended for blocking user code).
   * @param connection              An active D-Bus connection.
   * @param methodCallTimeoutMs     Timeout in milliseconds for method calls before they are considered stale
   *                                and removed from pending calls map.
   */
  public RealityCheckpoint(ExecutorService applicationTaskExecutor, Connection connection, long methodCallTimeoutMs) {
    this.applicationTaskExecutor = Objects.requireNonNull(applicationTaskExecutor,
            "ApplicationTaskExecutor cannot be null. Provide one for offloading user code.");
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
    this.methodCallTimeoutMs = methodCallTimeoutMs > 0 ? methodCallTimeoutMs : DEFAULT_METHOD_CALL_TIMEOUT_MS;
    pendingMethodCalls = new ConcurrentHashMap<>();
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    LOGGER.info("{} added to pipeline.", this.getClass().getSimpleName());
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt == DBusChannelEvent.MANDATORY_NAME_ACQUIRED) {
      DBusString localBusName = ctx.channel().attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get();
      LOGGER.info("{} active with bus name: {}. Ready for DBus interactions.",
              this.getClass().getSimpleName(), (localBusName != null ? localBusName : "unknown"));
      // Client is now fully operational. Application can start making calls / expecting signals.
      // Switch to ApplicationTaskExecutor for connection events
      applicationTaskExecutor.submit(() -> {
        connection.getPipeline().propagateConnectionActive();
      });
    } else if (evt == DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED) {
      LOGGER.error("Mandatory bus name acquisition failed. Closing the channel.");
      ctx.close();
    } else {
      LOGGER.warn("Received unhandled user event type: {}", evt.getClass().getName());
      ctx.fireUserEventTriggered(evt);
    }
  }

  /**
   * Sends the given {@link OutboundMessage} and returns a nested {@link Future} that tracks both
   * the write completion and the eventual reply.
   *
   * <p>This method is intended for use by higher-level components within the library or application.
   * It assumes that the {@code OutboundMessage} already has a valid and unique serial number assigned.
   *
   * <p>The returned {@code Future} structure works as follows:
   * <ul>
   *   <li>The <strong>outer {@code Future}</strong> completes when the outbound message has been
   *   successfully written to the transport.</li>
   *   <li>The <strong>inner {@code Future}</strong>, provided as the outer result, will be completed
   *   when the corresponding {@link InboundMessage} reply is received.</li>
   * </ul>
   *
   * @param msg the outbound message to send (must have a preassigned unique serial number).
   * @return a {@code Future} that completes with another {@code Future<InboundMessage>}:
   * the outer future completes when the message is written;
   * the inner future completes when the reply arrives.
   */
  public Future<Future<InboundMessage>> writeMessage(OutboundMessage msg) {
    if (ctx == null || !ctx.channel().isActive()) {
      Promise<Future<InboundMessage>> promise = ctx != null
              ? ctx.executor().newPromise() : GlobalEventExecutor.INSTANCE.newPromise();
      var re = new IllegalStateException("Channel is not active or handler not properly initialized.");
      promise.setFailure(re);
      return promise;
    }

    Promise<InboundMessage> replyPromise = ctx.executor().newPromise();

    if (msg instanceof OutboundMethodCall methodCall) {
      if (methodCall.isReplyExpected()) {
        // Use per-call timeout if specified, otherwise use connection default
        long timeoutMs = methodCall.getTimeout()
                .map(Duration::toMillis)
                .orElse(methodCallTimeoutMs);

        ScheduledFuture<?> timeoutFuture = ctx.executor().schedule(() -> {
          PendingMethodCall pendingCall = pendingMethodCalls.remove(msg.getSerial());
          if (pendingCall != null && !pendingCall.promise().isDone()) {
            LOGGER.warn("Method call with serial {} timed out after {}ms", msg.getSerial(), timeoutMs);
            pendingCall.promise().tryFailure(new TimeoutException(
                    "Method call with serial " + msg.getSerial() + " timed out after " + timeoutMs + "ms"));
          }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        pendingMethodCalls.put(msg.getSerial(), new PendingMethodCall(replyPromise, timeoutFuture));
      } else {
        replyPromise.trySuccess(null);
      }
    }

    Promise<Future<InboundMessage>> returnFuture = ctx.executor().newPromise();

    write(msg).addListener(f -> {
      if (f.isSuccess()) {
        returnFuture.trySuccess(replyPromise);
      } else if (f.cause() != null) {
        cancelPendingMethodCall(msg.getSerial());
        returnFuture.tryFailure(f.cause());
      } else if (f.isCancelled()) {
        cancelPendingMethodCall(msg.getSerial());
        if (returnFuture.isCancellable()) {
          returnFuture.cancel(true);
        } else {
          var re = new RuntimeException("Parental write-future has been cancelled.");
          returnFuture.tryFailure(re);
        }
      }
    });

    return returnFuture;
  }

  /**
   * Sends the given {@link OutboundMessage} without returning a future for the reply.
   *
   * <p>Unlike {@code writeMessage}, this method does not provide a {@link Future} for the expected
   * {@link InboundMessage} response. Instead, the reply will be delivered asynchronously
   * through the connection’s {@link Pipeline} and must be handled by registered
   * {@link InboundHandler}s.
   *
   * <p>This method is suitable when the application relies on the pipeline for inbound message
   * processing rather than awaiting a reply programmatically.
   *
   * @param msg the outbound message to send.
   * @return a {@link Future} that completes when the message has been successfully written,
   * or exceptionally if an error occurs during transmission.
   */
  public Future<Void> writeAndRouteResponse(OutboundMessage msg) {
    if (ctx == null || !ctx.channel().isActive()) {
      Promise<Void> promise = ctx != null
              ? ctx.executor().newPromise() : GlobalEventExecutor.INSTANCE.newPromise();
      var re = new IllegalStateException("Channel is not active or handler not properly initialized.");
      promise.setFailure(re);
      return promise;
    }

    return write(msg);
  }

  private ChannelFuture write(OutboundMessage msg) {
    LOGGER.debug("Writing outbound message (serial {}): {}", msg.getSerial(), msg);
    return ctx.writeAndFlush(msg).addListener(new WriteOperationListener<>(LOGGER));
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof InboundMessage inboundMessage) {
      handleInboundMessage(inboundMessage);
    } else {
      LOGGER.warn("Received unhandled message type: {}", msg.getClass().getName());
      ctx.fireChannelRead(msg);
    }
  }

  private void handleInboundMessage(InboundMessage msg) {
    if (msg instanceof InboundMethodReturn methodReturn) {
      handleInboundReply(methodReturn, methodReturn.getReplySerial());
    } else if (msg instanceof InboundError error) {
      handleInboundReply(error, error.getReplySerial());
    } else {
      // Propagate inbound message to the connection's pipeline on ApplicationTaskExecutor
      // This is the critical thread switch from Netty EventLoop to ApplicationTaskExecutor
      LOGGER.debug("Propagating inbound message to the connection's pipeline on ApplicationTaskExecutor.");
      applicationTaskExecutor.submit(() -> {
        connection.getPipeline().propagateInboundMessage(msg);
      });
    }
  }

  private void handleInboundReply(InboundMessage msg, DBusUInt32 replySerial) {
    if (msg instanceof InboundError) {
      LOGGER.debug("Received error reply for serial {}: {}", replySerial, msg);
    } else {
      LOGGER.debug("Received method return for serial {}: {}", replySerial, msg);
    }

    // Intercept inbound message if it's a reply to a pending method call,
    // initiated by the writeMessage method.
    PendingMethodCall pendingCall = pendingMethodCalls.remove(replySerial);
    if (pendingCall != null) {
      pendingCall.timeoutFuture().cancel(false);
      pendingCall.promise().setSuccess(msg);
      return;
    }

    // If the inbound message wasn't intercepted above,
    // we propagate it to the connection's pipeline on ApplicationTaskExecutor
    // so that it will be handled there.
    LOGGER.debug("Propagating inbound reply to the connection's pipeline on ApplicationTaskExecutor,"
            + " since it wasn't intercepted.");
    applicationTaskExecutor.submit(() -> {
      connection.getPipeline().propagateInboundMessage(msg);
    });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error("Exception in DbusClientLogicHandler processing pipeline: ", cause);
    failAllPendingCalls(cause);
    ctx.close(); // Close connection on unhandled errors in this handler
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.warn("Channel became inactive. Failing all pending D-Bus calls.");
    failAllPendingCalls(new ClosedChannelException());
    // Switch to ApplicationTaskExecutor for connection events
    applicationTaskExecutor.submit(() -> {
      connection.getPipeline().propagateConnectionInactive();
    });
    super.channelInactive(ctx);
  }

  private void cancelPendingMethodCall(DBusUInt32 serial) {
    PendingMethodCall pendingCall = pendingMethodCalls.remove(serial);
    if (pendingCall != null) {
      pendingCall.timeoutFuture().cancel(false);
    }
  }

  private void failAllPendingCalls(Throwable cause) {
    for (PendingMethodCall pendingCall : pendingMethodCalls.values()) {
      pendingCall.timeoutFuture().cancel(false);
      if (!pendingCall.promise().isDone()) {
        pendingCall.promise().tryFailure(cause);
      }
    }
    pendingMethodCalls.clear();
  }

  /**
   * Record for tracking pending method calls with their timeout futures.
   */
  private record PendingMethodCall(Promise<InboundMessage> promise, ScheduledFuture<?> timeoutFuture) {
  }
}
