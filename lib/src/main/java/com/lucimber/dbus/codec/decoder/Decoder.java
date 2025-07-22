/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusType;

/**
 * Decodes marshalled D-Bus data types.
 *
 * @param <BufferT> The data type of the buffer.
 * @param <ValueT>  The decoded data type.
 */
public interface Decoder<BufferT, ValueT extends DBusType> {
  /**
   * Decodes a specific type from a buffer.
   *
   * @param buffer The buffer that contains the marshalled type.
   * @param offset The number of bytes of a message, that have already been decoded.
   * @return The result of the decoding procedure.
   * @throws DecoderException If the value could not be decoded successfully.
   */
  DecoderResult<ValueT> decode(BufferT buffer, int offset) throws DecoderException;
}
