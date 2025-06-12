/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.DBusType;
import java.util.Objects;

/**
 * Default implementation of the decoder result interface.
 *
 * @param <ValueT> The value's data type.
 */
public final class DecoderResultImpl<ValueT extends DBusType> implements DecoderResult<ValueT> {

  private final ValueT value;
  private int consumedBytes;

  /**
   * Constructs a new decoder result.
   *
   * @param consumedBytes number of consumed bytes
   * @param value         the decoded value
   */
  public DecoderResultImpl(final int consumedBytes, final ValueT value) {
    this.consumedBytes = consumedBytes;
    this.value = Objects.requireNonNull(value, "value must not be null");
  }

  @Override
  public int getConsumedBytes() {
    return consumedBytes;
  }

  @Override
  public void setConsumedBytes(final int consumedBytes) {
    this.consumedBytes = consumedBytes;
  }

  @Override
  public ValueT getValue() {
    return value;
  }
}
