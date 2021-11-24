package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;

/**
 * An invoker that can be used to pass outbound messages to other components,
 * which are registered on the same pipeline instance.
 *
 * @see Pipeline
 * @see OutboundMessage
 */
public interface OutboundMessageInvoker {
  /**
   * Passes an outbound message further down the pipeline,
   * eventually reaching the first handler.
   *
   * @param msg the {@link OutboundMessage}
   */
  void passOutboundMessage(OutboundMessage msg);

  /**
   * Passes the cause of an outbound failure further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param cause the cause of the failure
   */
  void passOutboundFailure(Throwable cause);
}
