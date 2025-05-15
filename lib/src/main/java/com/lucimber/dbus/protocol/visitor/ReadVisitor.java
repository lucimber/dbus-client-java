/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.visitor;

import com.lucimber.dbus.protocol.signature.ast.TypeDescriptor;
import com.lucimber.dbus.protocol.types.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes D-Bus wire format into {@link BetterDBusType} instances.
 * <p>
 * Reads from a {@link ByteBuffer}, handling alignment and type codes.
 * </p>
 *
 * @since 2.0
 */
public class ReadVisitor {

  private final ByteBuffer buffer;
  private final ByteOrder order;

  public ReadVisitor(ByteBuffer buffer, ByteOrder order) {
    this.buffer = buffer.order(order);
    this.order = order;
  }

  /**
   * Reads a value of the given descriptor from the buffer.
   *
   * @param desc the type descriptor
   * @return the deserialized DbusType
   */
  public BetterDBusType read(TypeDescriptor desc) {
    // dispatch based on descriptor type
    if (desc instanceof com.lucimber.dbus.protocol.signature.ast.Basic b) {
      switch (b.code()) {
        case 'y' -> {
          return new BetterDBusByte(buffer.get());
        }
        case 'b' -> {
          return new BetterDBusBoolean(buffer.getInt() != 0);
        }
        case 'n' -> {
          return new BetterDBusInt16(buffer.getShort());
        }
        case 'q' -> {
          return new BetterDBusUInt16(buffer.getShort() & 0xFFFF);
        }
        case 'i' -> {
          return new BetterDBusInt32(buffer.getInt());
        }
        case 'u' -> {
          return new BetterDBusUInt32(buffer.getInt() & 0xFFFFFFFFL);
        }
        case 'x' -> {
          return new BetterDBusInt64(buffer.getLong());
        }
        case 't' -> {
          return new BetterDBusUInt64(BigInteger.valueOf(buffer.getLong()));
        }
        case 'd' -> {
          return new BetterDBusDouble(buffer.getDouble());
        }
        case 's' -> {
          int len = buffer.getInt();
          byte[] str = new byte[len];
          buffer.get(str);
          buffer.get(); // null terminator
          return new BetterDBusString(new String(str));
        }
        case 'o' -> {
          // object path as string
          int len = buffer.getInt();
          byte[] str = new byte[len];
          buffer.get(str);
          buffer.get();
          return new BetterDBusObjectPath(new String(str));
        }
        case 'g' -> {
          int slen = buffer.get();
          byte[] sig = new byte[slen];
          buffer.get(sig);
          buffer.get();
          return new BetterDBusSignature(new String(sig));
        }
        case 'h' -> {
          return new BetterDBusUnixFd(buffer.getInt());
        }
        default -> throw new IllegalArgumentException("Unknown basic code: " + b.code());
      }
    } else if (desc instanceof com.lucimber.dbus.protocol.signature.ast.Array a) {
      int length = buffer.getInt();
      int limit = buffer.position() + length;
      List<BetterDBusType> elems = new ArrayList<>();
      while (buffer.position() < limit) {
        elems.add(read(a.elementType()));
      }
      return new BetterDBusArray(elems);
    } else if (desc instanceof com.lucimber.dbus.protocol.signature.ast.Dict d) {
      // For direct dict read, read one entry
      BetterDBusType key = read(d.keyType());
      BetterDBusType val = read(d.valueType());
      return new BetterDBusDictEntry(key, val);
    } else if (desc instanceof com.lucimber.dbus.protocol.signature.ast.Struct s) {
      List<BetterDBusType> members = new ArrayList<>();
      for (var memberDesc : s.members()) {
        members.add(read(memberDesc));
      }
      return new BetterDBusStruct(members);
    } else if (desc instanceof com.lucimber.dbus.protocol.signature.ast.Variant) {
      // Read nested signature
      BetterDBusSignature sig = (BetterDBusSignature) new ReadVisitor(buffer, order)
            .read(new com.lucimber.dbus.protocol.signature.ast.Basic('g'));
      // Then read body by parsing signature
      TypeDescriptor nestedDesc = new com.lucimber.dbus.protocol.signature.SignatureParser(sig.getValue()).parse();
      BetterDBusType nestedValue = new ReadVisitor(buffer, order).read(nestedDesc);
      return new BetterDBusVariant(nestedValue);
    }
    throw new IllegalArgumentException("Unsupported descriptor: " + desc);
  }
}