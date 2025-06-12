/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageFlag;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Frame {

  private ByteOrder byteOrder;
  private MessageType type;
  private Set<MessageFlag> flags;
  private int protocolVersion;
  private UInt32 serial;
  private Map<HeaderField, Variant> headerFields;
  private ByteBuf body;

  public ByteOrder getByteOrder() {
    return byteOrder;
  }

  public void setByteOrder(final ByteOrder byteOrder) {
    this.byteOrder = byteOrder;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(final MessageType type) {
    this.type = type;
  }

  public Set<MessageFlag> getFlags() {
    return flags == null ? new HashSet<>() : new HashSet<>(flags);
  }

  public void setFlags(final Set<MessageFlag> flags) {
    this.flags = new HashSet<>(flags);
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(final int protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public UInt32 getSerial() {
    return serial;
  }

  public void setSerial(final UInt32 serial) {
    this.serial = serial;
  }

  public Map<HeaderField, Variant> getHeaderFields() {
    return new HashMap<>(headerFields);
  }

  public void setHeaderFields(final Map<HeaderField, Variant> headerFields) {
    this.headerFields = new HashMap<>(headerFields);
  }

  public ByteBuf getBody() {
    return body;
  }

  public void setBody(final ByteBuf body) {
    this.body = body;
  }

  @Override
  public String toString() {
    @SuppressWarnings("StringBufferReplaceableByString")
    final StringBuilder builder = new StringBuilder();
    builder.append("Frame{");
    builder.append("byteOrder=").append(byteOrder);
    builder.append(", type=").append(type);
    builder.append(", flags=").append(flags);
    builder.append(", protocolVersion=").append(protocolVersion);
    builder.append(", serial=").append(serial);
    builder.append(", headerFields=").append(headerFields);
    builder.append(", body=").append(body);
    builder.append("}");
    return builder.toString();
  }
}
