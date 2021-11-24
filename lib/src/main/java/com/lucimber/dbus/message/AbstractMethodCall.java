package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractMethodCall extends AbstractMessage {

  private ObjectPath objectPath;
  private DBusString name;
  private DBusString interfaceName;

  AbstractMethodCall(final UInt32 serial, final ObjectPath objectPath, final DBusString name) {
    super(serial);
    this.objectPath = Objects.requireNonNull(objectPath);
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public String toString() {
    final String s = "AbstractMethodCall{serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, getSerial(), objectPath, interfaceName, name, getSignature());
  }

  /**
   * Gets the object path of this method.
   *
   * @return an {@link ObjectPath}
   */
  public ObjectPath getObjectPath() {
    return objectPath;
  }

  /**
   * Sets the object path of this method.
   *
   * @param objectPath an {@link ObjectPath}
   */
  public void setObjectPath(final ObjectPath objectPath) {
    this.objectPath = Objects.requireNonNull(objectPath);
  }

  /**
   * Gets the name of this method.
   *
   * @return a {@link DBusString}
   */
  public DBusString getName() {
    return name;
  }

  /**
   * Sets the name of this method.
   *
   * @param name a {@link DBusString}
   */
  public void setName(final DBusString name) {
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Gets the name of the interface, to which this method belongs.
   *
   * @return a {@link DBusString}
   */
  public Optional<DBusString> getInterfaceName() {
    return Optional.ofNullable(interfaceName);
  }

  /**
   * Sets the name of the interface, to which this method belongs.
   *
   * @param interfaceName a {@link DBusString}
   */
  public void setInterfaceName(final DBusString interfaceName) {
    this.interfaceName = interfaceName;
  }
}
