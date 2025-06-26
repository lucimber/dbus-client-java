/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AppLogicHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppLogicHandler.class);

  // Map: serial number -> Promise for the reply Message
  private final Map<UInt32, Promise<InboundMessage>> pendingMethodCalls = new ConcurrentHashMap<>();
  // Map: Signal Match Rule (String or dedicated object) -> List of listeners
  private final Map<String, List<Consumer<InboundSignal>>> signalHandlers = new ConcurrentHashMap<>(); // Simplified key for now

  private ChannelHandlerContext ctx;
  private final ExecutorService applicationTaskExecutor; // For offloading user code
  private final Connection connection;

  /**
   * Creates a new instance.
   *
   * @param applicationTaskExecutor The executor service to run application-level callbacks
   *                                (like signal handlers) on, to avoid blocking the Netty EventLoop.
   *                                If null, a default will be attempted or tasks run on EventLoop (not recommended for blocking user code).
   * @param connection              An active D-Bus connection.
   */
  public AppLogicHandler(ExecutorService applicationTaskExecutor, Connection connection) {
    this.applicationTaskExecutor = Objects.requireNonNull(applicationTaskExecutor,
          "ApplicationTaskExecutor cannot be null. Provide one for offloading user code.");
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
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
      connection.getPipeline().passConnectionActiveEvent();
    } else if (evt == DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED) {
      LOGGER.error("Mandatory bus name acquisition failed. Closing the channel.");
      connection.getPipeline().passConnectionInactiveEvent();
      ctx.close();
    } else {
      LOGGER.warn("Received unhandled user event type: {}", evt.getClass().getName());
      ctx.fireUserEventTriggered(evt);
    }
  }

  /**
   * Sends an outbound message and returns a Future that will be fulfilled with the reply.
   * This method is intended to be called by higher-level API code within the library or application.
   * The serial number on the OutboundMessage is assumed to be already set and unique.
   *
   * @param msg The OutboundMessage to send.
   * @return A Future for the reply InboundMessage.
   */
  public Future<InboundMessage> sendMessage(OutboundMessage msg) {
    if (ctx == null || !ctx.channel().isActive()) {
      Promise<InboundMessage> promise = ctx != null
            ? ctx.executor().newPromise() : GlobalEventExecutor.INSTANCE.newPromise();
      var re = new IllegalStateException("Channel is not active or handler not properly initialized.");
      promise.setFailure(re);
      return promise;
    }

    Promise<InboundMessage> replyPromise = ctx.executor().newPromise();

    if (msg instanceof OutboundMethodCall methodCall) {
      if (methodCall.isReplyExpected()) {
        pendingMethodCalls.put(msg.getSerial(), replyPromise);
      }
    }

    LOGGER.debug("Writing outbound message (serial {}): {}", msg.getSerial(), msg);
    ctx.writeAndFlush(msg).addListener(f -> new DefaultFutureListener<>(LOGGER, v -> {
      if (msg instanceof OutboundMethodCall methodCall) {
        if (f.isSuccess()) {
          // For no-reply calls that wrote successfully, ensure promise is completed.
          if (!methodCall.isReplyExpected() && !replyPromise.isDone()) {
            replyPromise.trySuccess(null);
          }
        } else {
          var p = methodCall.isReplyExpected() ? pendingMethodCalls.remove(msg.getSerial()) : replyPromise;
          if (p != null && !p.isDone()) { // Check !p.isDone() for the no-reply case
            p.tryFailure(f.cause()); // tryFailure to avoid issues if already completed
          }
        }
      }
    }));
    return replyPromise;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    // TODO: Rework: divide between response for direct msg and dbus-pipeline
    if (msg instanceof InboundMethodReturn methodReturn) {
      handleInboundReply(methodReturn.getReplySerial(), methodReturn);
    } else if (msg instanceof InboundError error) {
      handleInboundReply(error.getReplySerial(), error);
    } else if (msg instanceof InboundSignal signal) {
      handleInboundSignal(signal);
    } else if (msg instanceof InboundMethodCall methodCall) {
      LOGGER.warn("Received InboundMethodCall. Client-side object exposure not yet fully implemented: {}", methodCall);
      if (methodCall.isReplyExpected()) {
        sendErrorReply(ctx, methodCall, "org.freedesktop.DBus.Error.NotSupported", "Method not supported by this client.");
      }
    } else {
      LOGGER.warn("Received unhandled message type: {}", msg.getClass().getName());
      ctx.fireChannelRead(msg);
    }
  }

  private void handleInboundReply(UInt32 replySerial, InboundMessage replyMessage) {
    Promise<InboundMessage> promise = pendingMethodCalls.remove(replySerial);
    if (promise != null) {
      if (replyMessage instanceof InboundError) {
        LOGGER.warn("Received error reply for serial {}: {}", replySerial, replyMessage);
        promise.setFailure(new DbusErrorException((InboundError) replyMessage));
      } else {
        LOGGER.debug("Received method return for serial {}: {}", replySerial, replyMessage);
        promise.setSuccess(replyMessage);
      }
    } else {
      LOGGER.warn("Received unsolicited reply with replySerial: {}. Message: {}", replySerial, replyMessage);
      // Potentially forward to a general "unsolicited message" handler or log.
    }
  }

  // Helper for generating a consistent key for signalHandlers map
  // TODO: This needs to be more robust to match how actual rules are made/matched
  private String generateSignalMatchKey(ObjectPath path, DBusString iface, DBusString member, DBusString sender) {
    StringBuilder sb = new StringBuilder();
    if (path != null) sb.append("path='").append(path).append("',");
    if (iface != null) sb.append("interface='").append(iface).append("',");
    if (member != null) sb.append("member='").append(member).append("',");
    if (sender != null) sb.append("sender='").append(sender).append("',");
    sb.append("type='signal'"); // Always for signals
    return sb.toString();
  }

  private void handleInboundSignal(InboundSignal signal) {
    // More precise matching for dispatching
    String key = generateSignalMatchKey(signal.getObjectPath(), signal.getInterfaceName(), signal.getMember(), signal.getSender());
    String interfaceAndMemberKey = generateSignalMatchKey(null, signal.getInterfaceName(), signal.getMember(), null); // Simpler key

    List<Consumer<InboundSignal>> specificListeners = signalHandlers.get(key);
    List<Consumer<InboundSignal>> generalListeners = signalHandlers.get(interfaceAndMemberKey);

    List<Consumer<InboundSignal>> allMatchingListeners = new ArrayList<>();
    if (specificListeners != null) allMatchingListeners.addAll(specificListeners);
    if (generalListeners != null) { // Avoid duplicates if same handler registered for specific and general
      for (Consumer<InboundSignal> l : generalListeners) {
        if (!allMatchingListeners.contains(l)) allMatchingListeners.add(l);
      }
    }


    if (!allMatchingListeners.isEmpty()) {
      LOGGER.debug("Dispatching signal (match key approx '{}') to {} listeners: {}",
            interfaceAndMemberKey, allMatchingListeners.size(), signal);
      for (Consumer<InboundSignal> listener : allMatchingListeners) {
        applicationTaskExecutor.execute(() -> {
          try {
            listener.accept(signal);
          } catch (Exception e) {
            LOGGER.error("Exception in application signal handler for signal: {}", signal, e);
          }
        });
      }
    } else {
      LOGGER.trace("No registered handlers for signal (match key approx '{}'): {}", interfaceAndMemberKey, signal);
    }
  }

  private void sendErrorReply(ChannelHandlerContext ctx, InboundMethodCall request, String errorName, String errorMessage) {
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

  /**
   * Registers a handler for a specific signal locally.
   * The corresponding AddMatch rule should have already been successfully sent to the bus.
   *
   * @param interfaceName The interface of the signal.
   * @param signalName    The name of the signal.
   * @param handler       The consumer to handle the signal.
   */
  public void registerLocalSignalHandler(DBusString interfaceName, DBusString signalName, Consumer<InboundSignal> handler) {
    // Simplified key. Real match rules are more complex and might involve path, sender, args.
    String key = generateSignalMatchKey(null, interfaceName, signalName, null); // Example key
    signalHandlers.computeIfAbsent(key, k -> new ArrayList<>()).add(handler);
    LOGGER.info("Locally registered signal handler for {}#{}", interfaceName, signalName);
  }

  public void removeLocalSignalHandler(DBusString interfaceName, DBusString signalName, Consumer<InboundSignal> handler) {
    String key = generateSignalMatchKey(null, interfaceName, signalName, null);
    List<Consumer<InboundSignal>> listeners = signalHandlers.get(key);
    if (listeners != null) {
      if (listeners.remove(handler)) {
        LOGGER.info("Locally unregistered signal handler for {}#{}", interfaceName, signalName);
      }
      if (listeners.isEmpty()) {
        signalHandlers.remove(key);
      }
    }
    // Corresponding RemoveMatch should be sent by DbusConnection
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
    connection.getPipeline().passConnectionInactiveEvent();
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

  // Custom exception for DBus errors returned to the application via Promise
  public static class DbusErrorException extends Exception {
    private final InboundError dbusError;

    public DbusErrorException(InboundError dbusError) {
      super(String.format("DBus error: %s (Message: %s)",
            dbusError.getErrorName(),
            extractErrorMessage(dbusError.getPayload())));
      this.dbusError = dbusError;
    }

    public InboundError getDbusError() {
      return dbusError;
    }

    private static String extractErrorMessage(List<DBusType> payload) {
      if (payload != null && !payload.isEmpty() && payload.get(0) instanceof DBusString) {
        return payload.get(0).toString();
      }
      return payload != null ? payload.toString() : "No error message payload";
    }
  }
}
