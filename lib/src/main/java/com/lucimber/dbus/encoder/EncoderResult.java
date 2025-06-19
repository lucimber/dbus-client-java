/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

/**
 * The EncoderResult class yields the encoded D-Bus data type and the number of encoded bytes.
 * The number of encoded bytes is necessary for the calculation of the alignment padding.
 *
 * @param <BufferT> The data type of the buffer.
 */
public interface EncoderResult<BufferT> {
  /**
   * Gets the number of bytes, that have been encoded by this encoder
   * while producing the result.
   *
   * @return An integer as the number of encoded bytes.
   */
  int getProducedBytes();

  /**
   * Gets the buffer that contains the result of the encoding.
   *
   * @return A buffer.
   */
  BufferT getBuffer();
}
