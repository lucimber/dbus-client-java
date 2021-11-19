package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.CharBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * An outbound handler that encodes US ASCII strings to bytes.
 */
final class SaslStringEncoder extends MessageToByteEncoder<String> {

    private static final String CRLF = "\r\n";
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_SASL_OUTBOUND);

    @Override
    protected void encode(final ChannelHandlerContext ctx, final String msg, final ByteBuf out) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding SASL string to bytes.");
        if (msg.length() != 0) {
            final CharBuffer charBuffer = CharBuffer.wrap(msg + CRLF);
            final ByteBuf byteBuffer = ByteBufUtil.encodeString(ctx.alloc(), charBuffer, US_ASCII);
            out.writeBytes(byteBuffer);
            byteBuffer.release();
        }
    }
}
