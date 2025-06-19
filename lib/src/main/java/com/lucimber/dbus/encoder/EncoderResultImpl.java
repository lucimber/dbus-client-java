/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import java.util.Objects;

/**
 * Default implementation of the {@link EncoderResult} interface.
 *
 * @param <T> The buffer's type.
 */
public final class EncoderResultImpl<T> implements EncoderResult<T> {

  private final T buffer;
  private final int byteCount;

  /**
   * Creates a new instance with the necessary arguments.
   *
   * @param byteCount The number of encoded bytes.
   * @param buffer    The buffer that contains the result of the encoding.
   */
  public EncoderResultImpl(final int byteCount, final T buffer) {
    this.byteCount = byteCount;
    this.buffer = Objects.requireNonNull(buffer);
  }

  @Override
  public int getProducedBytes() {
    return byteCount;
  }

  @Override
  public T getBuffer() {
    return buffer;
  }
}
