/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

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
  public DecoderResultImpl(int consumedBytes, ValueT value) {
    this.consumedBytes = consumedBytes;
    this.value = Objects.requireNonNull(value, "value must not be null");
  }

  @Override
  public int getConsumedBytes() {
    return consumedBytes;
  }

  @Override
  public void setConsumedBytes(int consumedBytes) {
    this.consumedBytes = consumedBytes;
  }

  @Override
  public ValueT getValue() {
    return value;
  }
}
