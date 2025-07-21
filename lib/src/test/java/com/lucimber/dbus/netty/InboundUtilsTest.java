/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.MessageFlag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InboundUtilsTest {

  @Test
  void decodeSingleFlag() {
  final ByteBuf buffer = Unpooled.buffer();
  buffer.writeByte(0x01);
  final Set<MessageFlag> flags = InboundUtils.decodeFlags(buffer);
  assertEquals(Set.of(MessageFlag.NO_REPLY_EXPECTED), flags);
  }

  @Test
  void decodeCombinedFlags() {
  final ByteBuf buffer = Unpooled.buffer();
  buffer.writeByte(0x03);
  final Set<MessageFlag> flags = InboundUtils.decodeFlags(buffer);
  assertEquals(Set.of(MessageFlag.NO_REPLY_EXPECTED, MessageFlag.NO_AUTO_START), flags);
  }

  @Test
  void decodeAllFlags() {
  final ByteBuf buffer = Unpooled.buffer();
  buffer.writeByte(0x07);
  final Set<MessageFlag> flags = InboundUtils.decodeFlags(buffer);
  assertEquals(3, flags.size(), "Number of flags");
  assertTrue(flags.contains(MessageFlag.NO_REPLY_EXPECTED), "NO_REPLY_EXPECTED");
  assertTrue(flags.contains(MessageFlag.NO_AUTO_START), "NO_AUTO_START");
  assertTrue(flags.contains(MessageFlag.ALLOW_INTERACTIVE_AUTHORIZATION),
          "ALLOW_INTERACTIVE_AUTHORIZATION");
  }
}
