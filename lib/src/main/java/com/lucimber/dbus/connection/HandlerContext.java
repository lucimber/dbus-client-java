package com.lucimber.dbus.connection;

/**
 * Mediates between {@link Handler}s that are part of the same {@link Pipeline}.
 */
public interface HandlerContext extends InboundMessageInvoker, OutboundMessageInvoker,
        ConnectionEventMediator, UserEventMediator {

  /**
   * Gets the handler of this context.
   *
   * @return the {@link Handler}
   */
  Handler getHandler();

  /**
   * Gets the name of this context.
   *
   * @return the name
   */
  String getName();

  /**
   * Gets the pipeline that this context is part of.
   *
   * @return the {@link Pipeline}
   */
  Pipeline getPipeline();

  /**
   * Passes an user event to the next receiver in the pipeline.
   *
   * @param event the user event
   */
  void passUserEvent(Object event);
}
