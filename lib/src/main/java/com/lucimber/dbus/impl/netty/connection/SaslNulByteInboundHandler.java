package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Immediately after connecting to the server, the client must send a single nul byte.
 * However, the nul byte must be sent even on other kinds of socket,
 * and even on operating systems that do not require a byte to be sent in order to transmit credentials.
 * If the first byte received from the client is not a nul byte, the server may disconnect that client.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#auth-nul-byte"
 * target="_top">Desktop Bus Specification (auth-nul-byte)</a>
 */
final class SaslNulByteInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_SASL_INBOUND);

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    sendInitialNulByte(ctx);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LoggerUtils.error(LOGGER, MARKER, () -> "A throwable has been received.", cause);
    LoggerUtils.debug(LOGGER, MARKER, () -> "Ignoring and passing throwable to next inbound handler.");
    ctx.fireExceptionCaught(cause);
  }

  private void sendInitialNulByte(final ChannelHandlerContext ctx) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Sending initial NUL byte.");
    final ByteBuf msg = ctx.alloc().buffer();
    msg.writeByte(0);
    final ChannelFuture future = ctx.writeAndFlush(msg);
    future.addListener(f -> {
      if (f.isDone() && f.isSuccess()) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "I/O operation was completed successfully.");
        LoggerUtils.trace(LOGGER, MARKER, () -> "Detaching from channel pipeline.");
        ctx.pipeline().remove(this);
        ctx.pipeline().fireUserEventTriggered(CustomChannelEvent.SASL_NUL_BYTE_SENT);
      } else if (f.isDone() && f.cause() != null) {
        LoggerUtils.error(LOGGER, MARKER, () -> "I/O operation was completed with failure.", f.cause());
        ctx.fireExceptionCaught(f.cause());
      } else if (f.isDone() && f.isCancelled()) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "I/O operation was completed by cancellation.");
      }
    });
  }
}
