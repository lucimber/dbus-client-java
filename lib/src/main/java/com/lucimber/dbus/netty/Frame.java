/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageFlag;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
  private ByteBuffer body;
  private UInt32 bodyLength;

  public ByteOrder getByteOrder() {
    return byteOrder;
  }

  public void setByteOrder(ByteOrder byteOrder) {
    this.byteOrder = byteOrder;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public Set<MessageFlag> getFlags() {
    return flags == null ? new HashSet<>() : new HashSet<>(flags);
  }

  public void setFlags(Set<MessageFlag> flags) {
    this.flags = new HashSet<>(flags);
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(int protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public UInt32 getSerial() {
    return serial;
  }

  public void setSerial(UInt32 serial) {
    this.serial = serial;
  }

  public Map<HeaderField, Variant> getHeaderFields() {
    return new HashMap<>(headerFields);
  }

  public void setHeaderFields(Map<HeaderField, Variant> headerFields) {
    this.headerFields = new HashMap<>(headerFields);
  }

  public ByteBuffer getBody() {
    return body;
  }

  public void setBody(ByteBuffer body) {
    this.body = body;
  }

  public UInt32 getBodyLength() {
    return bodyLength;
  }

  public void setBodyLength(UInt32 bodyLength) {
    this.bodyLength = bodyLength;
  }

  @Override
  public String toString() {
    @SuppressWarnings("StringBufferReplaceableByString") StringBuilder builder = new StringBuilder();
    builder.append("Frame{");
    builder.append("byteOrder=").append(byteOrder);
    builder.append(", type=").append(type);
    builder.append(", flags=").append(flags);
    builder.append(", protocolVersion=").append(protocolVersion);
    builder.append(", serial=").append(serial);
    builder.append(", headerFields=").append(headerFields);
    builder.append(", bodyLength=").append(bodyLength);
    builder.append(", body=").append(body);
    builder.append("}");
    return builder.toString();
  }
}
