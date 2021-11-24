package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a byte from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusByte
 */
public final class ByteDecoder implements Decoder<ByteBuf, DBusByte> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static void logResult(final DBusByte value, final int offset, final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "BYTE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, 0, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusByte> decode(final ByteBuf buffer, final int offset) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      final int consumedBytes = 1;
      final DBusByte value = DBusByte.valueOf(buffer.readByte());
      final DecoderResult<DBusByte> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode BYTE.", t);
    }
  }
}
