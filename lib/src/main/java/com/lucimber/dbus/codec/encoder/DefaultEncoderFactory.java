/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of EncoderFactory that provides encoders for all standard D-Bus types.
 * <p>
 * This factory uses a registry-based approach to create encoders, making it easy to extend
 * with new types and improving performance by avoiding switch statements.
 */
public final class DefaultEncoderFactory implements EncoderFactory {

  private final Map<Type, EncoderCreator> encoderRegistry;

  /**
   * Creates a new DefaultEncoderFactory with all standard D-Bus encoders registered.
   */
  public DefaultEncoderFactory() {
    this.encoderRegistry = new EnumMap<>(Type.class);
    registerStandardEncoders();
  }

  private void registerStandardEncoders() {
    // Basic types
    encoderRegistry.put(Type.BOOLEAN, BooleanEncoder::new);
    encoderRegistry.put(Type.BYTE, order -> new ByteEncoder());
    encoderRegistry.put(Type.DOUBLE, DoubleEncoder::new);
    encoderRegistry.put(Type.INT16, Int16Encoder::new);
    encoderRegistry.put(Type.INT32, Int32Encoder::new);
    encoderRegistry.put(Type.INT64, Int64Encoder::new);
    encoderRegistry.put(Type.UINT16, UInt16Encoder::new);
    encoderRegistry.put(Type.UINT32, UInt32Encoder::new);
    encoderRegistry.put(Type.UINT64, UInt64Encoder::new);
    encoderRegistry.put(Type.STRING, StringEncoder::new);
    encoderRegistry.put(Type.OBJECT_PATH, ObjectPathEncoder::new);
    encoderRegistry.put(Type.SIGNATURE, order -> new SignatureEncoder());
    encoderRegistry.put(Type.UNIX_FD, UnixFdEncoder::new);

    // Note: Container types (ARRAY, DICT_ENTRY, STRUCT) are not registered here
    // because they require additional signature parameters that this basic factory cannot provide.
    // Only VARIANT is supported as it doesn't require external signature information.
    encoderRegistry.put(Type.VARIANT, VariantEncoder::new);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Encoder<DBusType, ByteBuffer> createEncoder(Type type, ByteOrder order) throws EncoderException {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(order, "order must not be null");

    EncoderCreator creator = encoderRegistry.get(type);
    if (creator == null) {
      throw new EncoderException("No encoder available for type: " + type);
    }

    return (Encoder<DBusType, ByteBuffer>) creator.create(order);
  }

  @Override
  public boolean canEncode(Type type) {
    return encoderRegistry.containsKey(type);
  }

  /**
   * Registers a custom encoder creator for a specific type.
   *
   * @param type    the D-Bus type
   * @param creator the encoder creator function
   */
  public void registerEncoder(Type type, EncoderCreator creator) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(creator, "creator must not be null");
    encoderRegistry.put(type, creator);
  }

  /**
   * Functional interface for creating encoders with a given byte order.
   */
  @FunctionalInterface
  public interface EncoderCreator {
    Encoder<?, ByteBuffer> create(ByteOrder order) throws EncoderException;
  }
}
