/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a struct from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see Struct
 */
public final class StructDecoder implements Decoder<ByteBuf, Struct> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final ByteOrder order;
  private final Signature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order     a {@link ByteOrder}
   * @param signature a {@link Signature}; must describe a struct
   */
  public StructDecoder(final ByteOrder order, final Signature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isStruct()) {
      throw new IllegalArgumentException("signature must describe a struct");
    }
  }

  private static void logResult(final Signature signature, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "STRUCT: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<Struct> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    try {
      Objects.requireNonNull(buffer, "buffer must not be null");
      int consumedBytes = 0;
      final ArrayList<DBusType> list = new ArrayList<>();
      final int padding = DecoderUtils.skipPadding(buffer, offset, Type.STRUCT);
      consumedBytes += padding;
      final Signature subSignature = signature.subContainer();
      final List<Signature> signatures;
      if (subSignature.getQuantity() == 1) {
        signatures = new ArrayList<>();
        signatures.add(subSignature);
      } else {
        signatures = subSignature.getChildren();
      }
      for (Signature s : signatures) {
        final DecoderResult<?> result = decodeSingle(s, buffer, offset + consumedBytes);
        consumedBytes += result.getConsumedBytes();
        list.add(result.getValue());
      }
      final Struct struct = new Struct(signature, list);
      final DecoderResult<Struct> result = new DecoderResultImpl<>(consumedBytes, struct);
      logResult(signature, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode STRUCT.", t);
    }
  }

  private DecoderResult<?> decodeSingle(final Signature signature, final ByteBuf buffer, final int offset)
          throws Exception {
    final char c = signature.toString().charAt(0);
    final Type type = TypeUtils.getTypeFromChar(c)
            .orElseThrow(() -> new Exception("can not map from char to type: " + c));
    if (type == Type.DICT_ENTRY) {
      throw new Exception("Invalid symbol in struct signature (dict entry).");
    }
    return DecoderUtils.decode(signature, buffer, offset, order);
  }
}
