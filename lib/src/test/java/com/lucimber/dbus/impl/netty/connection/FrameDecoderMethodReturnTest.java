package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.impl.netty.encoder.EncoderResult;
import com.lucimber.dbus.impl.netty.encoder.EncoderResultImpl;
import com.lucimber.dbus.impl.netty.encoder.EncoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class FrameDecoderMethodReturnTest {

  private static final int PROTOCOL_VERSION = 1;

  private static EncoderResult<ByteBuf> encodeFrameBody(final List<DBusType> payload, final ByteOrder byteOrder) {
    final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
    final ByteBuf body = allocator.buffer();
    int localByteCount = 0;
    for (DBusType o : payload) {
      final EncoderResult<ByteBuf> result = EncoderUtils.encode(o, localByteCount, byteOrder);
      localByteCount += result.getProducedBytes();
      final ByteBuf buffer = result.getBuffer();
      body.writeBytes(buffer);
      buffer.release();
    }
    return new EncoderResultImpl<>(localByteCount, body);
  }

  @Test
  void succeedWithSimpleMethodReturn() {
    final FrameDecoder decoder = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(decoder);
    final Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_RETURN);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    frame.setHeaderFields(headerFields);
    assertTrue(channel.writeInbound(frame));
    final InboundMethodReturn inboundMessage = channel.readInbound();
    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial");
  }

  @Test
  void succeedWithComplexMethodReturn() {
    final FrameDecoder decoder = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(decoder);
    final Frame frame = new Frame();
    final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    frame.setByteOrder(byteOrder);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_RETURN);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    final Signature signature = Signature.valueOf("a{oa{sa{sv}}}");
    final Variant signatureVariant = Variant.valueOf(signature);
    headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    frame.setHeaderFields(headerFields);
    final Dict<ObjectPath, Dict<DBusString, Dict<DBusString, Variant>>> dict =
            new Dict<>(signature);
    final List<DBusType> payload = new ArrayList<>();
    payload.add(dict);
    final EncoderResult<ByteBuf> result = encodeFrameBody(payload, byteOrder);
    frame.setBody(result.getBuffer());
    assertTrue(channel.writeInbound(frame));
    final InboundMethodReturn methodReturn = channel.readInbound();
    assertEquals(sender, methodReturn.getSender(), "Sender");
    assertEquals(replySerial, methodReturn.getReplySerial(), "Reply serial");
  }
}
