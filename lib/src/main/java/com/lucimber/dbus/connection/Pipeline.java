package com.lucimber.dbus.connection;

/**
 * In object-oriented design, the chain-of-responsibility pattern is a design pattern
 * consisting of a source of command objects and a series of processing objects.
 * Each processing object contains logic that defines the types of command objects that it can handle;
 * the rest are passed to the next processing object in the chain.
 * A mechanism also exists for adding new processing objects to the end of this chain.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern">Chain-of-responsibility pattern</a>
 */
public interface Pipeline extends InboundMessageInvoker, OutboundMessageInvoker,
        ConnectionEventMediator, UserEventMediator {

  /**
   * Adds a handler in front of another handler to this pipeline.
   *
   * @param nameOther the name of the other {@link Handler}
   * @param name      the, unique to this pipeline, name of the {@link Handler}
   * @param handler   the {@link Handler}
   */
  void addBefore(String nameOther, String name, Handler handler);

  /**
   * Adds a handler to the end of this pipeline.
   *
   * @param name    the, unique to this chain, name of the {@link Handler}
   * @param handler the {@link Handler}
   */
  void addLast(String name, Handler handler);

  /**
   * Returns the connection that this pipeline is attached to.
   *
   * @return the {@link Connection}
   */
  Connection getConnection();

  /**
   * Removes a handler from this pipeline.
   *
   * @param name the, unique to this pipeline, name of the {@link Handler}
   */
  void remove(String name);

  /**
   * Replaces a handler with another handler on this pipeline.
   *
   * @param oldName the name of the old {@link Handler}
   * @param newName the name of the new {@link Handler}
   * @param handler the new {@link Handler}
   */
  void replace(String oldName, String newName, Handler handler);
}
