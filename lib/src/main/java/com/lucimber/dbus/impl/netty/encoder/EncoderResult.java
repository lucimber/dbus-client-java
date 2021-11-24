package com.lucimber.dbus.impl.netty.encoder;

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
