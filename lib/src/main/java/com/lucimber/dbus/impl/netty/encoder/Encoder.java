package com.lucimber.dbus.impl.netty.encoder;

/**
 * Encodes D-Bus types into their marshalled representation.
 *
 * @param <ValueT>  The data type of the value.
 * @param <BufferT> The data type of the buffer.
 */
public interface Encoder<ValueT, BufferT> {
    /**
     * Encodes a value into bytes.
     *
     * @param value      The data type that needs to be encoded.
     * @param byteOffset The byte count of already encoded bytes, which is necessary for alignment padding.
     * @return An {@link EncoderResult} containing the marshalled representation of the value.
     * @throws EncoderException If the value could not have been encoded.
     */
    EncoderResult<BufferT> encode(ValueT value, int byteOffset) throws EncoderException;
}
