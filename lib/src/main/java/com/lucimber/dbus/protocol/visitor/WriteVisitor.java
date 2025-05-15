/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.visitor;

import com.lucimber.dbus.protocol.types.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Serializes {@link BetterDBusType} instances into D-Bus wire format.
 * <p>
 * Implements alignment, padding, endianness and type-specific encoding.
 * </p>
 *
 * @since 2.0
 */
public class WriteVisitor implements DBusTypeVisitor<ByteBuffer> {

  private final ByteOrder order;

  public WriteVisitor(ByteOrder order) {
    this.order = order;
  }

  @Override
  public ByteBuffer visitByte(BetterDBusByte value) {
    ByteBuffer buf = ByteBuffer.allocate(1).order(order);
    buf.put(value.value());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitBoolean(BetterDBusBoolean value) {
    ByteBuffer buf = ByteBuffer.allocate(4).order(order);
    buf.putInt(value.value() ? 1 : 0);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitInt16(BetterDBusInt16 value) {
    ByteBuffer buf = ByteBuffer.allocate(2).order(order);
    buf.putShort(value.value());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitUInt16(BetterDBusUInt16 value) {
    return visitInt16(new BetterDBusInt16((short) (value.value() & 0xFFFF)));
  }

  @Override
  public ByteBuffer visitInt32(BetterDBusInt32 value) {
    ByteBuffer buf = ByteBuffer.allocate(4).order(order);
    buf.putInt(value.value());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitUInt32(BetterDBusUInt32 value) {
    return visitInt32(new BetterDBusInt32((int) (value.value() & 0xFFFFFFFFL)));
  }

  @Override
  public ByteBuffer visitInt64(BetterDBusInt64 value) {
    ByteBuffer buf = ByteBuffer.allocate(8).order(order);
    buf.putLong(value.value());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitUInt64(BetterDBusUInt64 value) {
    return visitInt64(new BetterDBusInt64(value.value().longValue()));
  }

  @Override
  public ByteBuffer visitDouble(BetterDBusDouble value) {
    ByteBuffer buf = ByteBuffer.allocate(8).order(order);
    buf.putDouble(value.value());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitString(BetterDBusString value) {
    byte[] bytes = value.value().getBytes();
    ByteBuffer buf = ByteBuffer.allocate(4 + bytes.length + 1).order(order);
    buf.putInt(bytes.length);
    buf.put(bytes);
    buf.put((byte) 0);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitObjectPath(BetterDBusObjectPath value) {
    return visitString(new BetterDBusString(value.getValue()));
  }

  @Override
  public ByteBuffer visitSignature(BetterDBusSignature value) {
    byte[] bytes = value.getValue().getBytes();
    ByteBuffer buf = ByteBuffer.allocate(1 + bytes.length + 1);
    buf.put((byte) bytes.length);
    buf.put(bytes);
    buf.put((byte) 0);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitUnixFd(BetterDBusUnixFd value) {
    ByteBuffer buf = ByteBuffer.allocate(4).order(order);
    buf.putInt(value.getValue());
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitArray(BetterDBusArray value) {
    // Serialize length + elements; alignment to 4 bytes
    List<BetterDBusType> elems = value.elements();
    // First, collect element buffers
    List<ByteBuffer> parts = elems.stream()
          .map(e -> e.accept(new WriteVisitor(order)))
          .toList();
    int total = parts.stream().mapToInt(ByteBuffer::remaining).sum();
    ByteBuffer buf = ByteBuffer.allocate(4 + total).order(order);
    buf.putInt(total);
    parts.forEach(buf::put);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitStruct(BetterDBusStruct value) {
    List<BetterDBusType> members = value.members();
    List<ByteBuffer> parts = members.stream()
          .map(e -> e.accept(new WriteVisitor(order)))
          .toList();
    int total = parts.stream().mapToInt(ByteBuffer::remaining).sum();
    ByteBuffer buf = ByteBuffer.allocate(total).order(order);
    parts.forEach(buf::put);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitDictEntry(BetterDBusDictEntry value) {
    ByteBuffer keyBuf = value.key().accept(new WriteVisitor(order));
    ByteBuffer valBuf = value.value().accept(new WriteVisitor(order));
    ByteBuffer buf = ByteBuffer.allocate(keyBuf.remaining() + valBuf.remaining()).order(order);
    buf.put(keyBuf);
    buf.put(valBuf);
    buf.flip();
    return buf;
  }

  @Override
  public ByteBuffer visitVariant(BetterDBusVariant value) {
    // Variant: signature + value
    BetterDBusSignature sig = new BetterDBusSignature(value.value().signature());
    ByteBuffer sigBuf = visitSignature(sig);
    ByteBuffer valBuf = value.value().accept(new WriteVisitor(order));
    ByteBuffer buf = ByteBuffer.allocate(sigBuf.remaining() + valBuf.remaining()).order(order);
    buf.put(sigBuf);
    buf.put(valBuf);
    buf.flip();
    return buf;
  }
}