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
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inbound and outbound messages and routes them to the bound connection and pipeline (not netty).
 */
public class AppLogicHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppLogicHandler.class);

  private final ConcurrentHashMap<UInt32, Promise<InboundMessage>> pendingMethodCalls;
  private final ArrayList<UInt32> pendingRoutedMethodCalls;
  private final ReentrantLock pendingRoutedMethodCallsLock;
  private final ExecutorService applicationTaskExecutor; // For offloading user code
  private final Connection connection;
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
  public AppLogicHandler(ExecutorService applicationTaskExecutor, Connection connection) {
    this.applicationTaskExecutor = Objects.requireNonNull(applicationTaskExecutor,
            "ApplicationTaskExecutor cannot be null. Provide one for offloading user code.");
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
    pendingMethodCalls = new ConcurrentHashMap<>();
    pendingRoutedMethodCalls = new ArrayList<>();
    pendingRoutedMethodCallsLock = new ReentrantLock();
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
      connection.getPipeline().propagateConnectionActive();
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
   * <p>
   * This method is intended for use by higher-level components within the library or application.
   * It assumes that the {@code OutboundMessage} already has a valid and unique serial number assigned.
   * <p>
   * The returned {@code Future} structure works as follows:
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
        pendingMethodCalls.put(msg.getSerial(), replyPromise);
      } else {
        replyPromise.trySuccess(null);
      }
    }

    Promise<Future<InboundMessage>> returnFuture = ctx.executor().newPromise();

    write(msg).addListener(f -> {
      if (f.isSuccess()) {
        returnFuture.trySuccess(replyPromise);
      } else if (f.cause() != null) {
        pendingMethodCalls.remove(msg.getSerial());
        returnFuture.tryFailure(f.cause());
      } else if (f.isCancelled()) {
        pendingMethodCalls.remove(msg.getSerial());
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
   * <p>
   * Unlike {@code writeMessage}, this method does not provide a {@link Future} for the expected
   * {@link InboundMessage} response. Instead, the reply will be delivered asynchronously
   * through the connection’s {@link Pipeline} and must be handled by registered
   * {@link InboundHandler}s.
   * <p>
   * This method is suitable when the application relies on the pipeline for inbound message
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

    if (msg instanceof OutboundMethodCall methodCall) {
      if (methodCall.isReplyExpected()) {
        pendingRoutedMethodCallsLock.lock();
        try {
          pendingRoutedMethodCalls.add(msg.getSerial());
        } finally {
          pendingRoutedMethodCallsLock.unlock();
        }
      }
    }

    return write(msg);
  }

  private ChannelFuture write(OutboundMessage msg) {
    LOGGER.debug("Writing outbound message (serial {}): {}", msg.getSerial(), msg);
    return ctx.writeAndFlush(msg).addListener(f -> new DefaultFutureListener<>(LOGGER));
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof InboundMessage inboundMessage) {
      handleInboundMessage(ctx, inboundMessage);
    } else {
      LOGGER.warn("Received unhandled message type: {}", msg.getClass().getName());
      ctx.fireChannelRead(msg);
    }
  }

  private void handleInboundMessage(ChannelHandlerContext ctx, InboundMessage msg) {
    if (msg instanceof InboundMethodReturn methodReturn) {
      handleInboundReply(methodReturn, methodReturn.getReplySerial());
    } else if (msg instanceof InboundError error) {
      handleInboundReply(error, error.getReplySerial());
    } else if (msg instanceof InboundSignal signal) {
      LOGGER.warn("Received InboundSignal. "
              + "Client-side handling of signals not yet implemented: {}", signal);
    } else if (msg instanceof InboundMethodCall methodCall) {
      LOGGER.warn("Received InboundMethodCall. "
              + "Client-side object exposure not yet implemented: {}", methodCall);
      if (methodCall.isReplyExpected()) {
        sendErrorReply(ctx, methodCall,
                "org.freedesktop.DBus.Error.NotSupported",
                "Method not supported by this client.");
      }
    }
  }

  private void handleInboundReply(InboundMessage msg, UInt32 replySerial) {
    if (msg instanceof InboundError) {
      LOGGER.debug("Received error reply for serial {}: {}", replySerial, msg);
    } else {
      LOGGER.debug("Received method return for serial {}: {}", replySerial, msg);
    }

    Promise<InboundMessage> promise = pendingMethodCalls.remove(replySerial);
    if (promise != null) {
      // Normal completion path for sendRequest()
      promise.setSuccess(msg);
      return;
    }

    boolean replyIsExpected;
    pendingRoutedMethodCallsLock.lock();
    try {
      replyIsExpected = pendingRoutedMethodCalls.remove(replySerial);
    } finally {
      pendingRoutedMethodCallsLock.unlock();
    }

    if (replyIsExpected) {
      LOGGER.debug("Forwarding reply with serial {} to the pipeline.", replySerial);
      connection.getPipeline().propagateInboundMessage(msg);
    } else {
      LOGGER.warn("Received unsolicited reply with replySerial: {}. Message: {}", replySerial, msg);
    }
  }

  private void sendErrorReply(ChannelHandlerContext ctx, InboundMethodCall request,
                              String errorName, String errorMessage) {
    if (!request.isReplyExpected()) {
      return;
    }

    AtomicLong serialCounter = ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    UInt32 replyErrorSerial = UInt32.valueOf((int) serialCounter.getAndIncrement());
    DBusString errorNameStr = DBusString.valueOf(errorName);
    List<DBusType> payload = List.of(DBusString.valueOf(errorMessage)); // Error message is often first arg
    Signature signature = Signature.valueOf("s");

    // Destination for the error reply is the sender of the original request
    OutboundError errorReply = new OutboundError(replyErrorSerial, request.getSerial(), errorNameStr,
            request.getSender(), signature, payload);
    ctx.writeAndFlush(errorReply).addListener(future -> {
      if (!future.isSuccess()) {
        LOGGER.error("Failed to send error reply for request serial {}", request.getSerial(), future.cause());
      }
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
    connection.getPipeline().propagateConnectionInactive();
    super.channelInactive(ctx);
  }

  private void failAllPendingCalls(Throwable cause) {
    for (Promise<InboundMessage> promise : pendingMethodCalls.values()) {
      if (!promise.isDone()) {
        promise.tryFailure(cause);
      }
    }
    pendingMethodCalls.clear();
  }
}
