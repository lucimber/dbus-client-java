package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;

/**
 * An invoker that can be used to pass inbound messages to other components,
 * which are registered on the same pipeline instance.
 *
 * @see Pipeline
 * @see InboundMessage
 */
public interface InboundMessageInvoker {
  /**
   * Passes an inbound message further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param msg the {@link InboundMessage}
   */
  void passInboundMessage(InboundMessage msg);

  /**
   * Passes the cause of an inbound failure further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param cause the cause of the failure
   */
  void passInboundFailure(Throwable cause);
}
