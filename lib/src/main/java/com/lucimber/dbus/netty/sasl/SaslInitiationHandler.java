/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.netty.DBusChannelEvent;
import com.lucimber.dbus.netty.WriteOperationListener;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Netty channel handler that initiates the DBus SASL authentication process
 * by sending a NUL byte when the channel becomes active.
 * <p>
 * After successfully sending the NUL byte, this handler fires a
 * {@link DBusChannelEvent#SASL_NUL_BYTE_SENT} user event and then
 * removes itself from the pipeline.
 * </p>
 */
public class SaslInitiationHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaslInitiationHandler.class);
  private static final byte[] NUL_BYTE_ARRAY = new byte[]{0};

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    LOGGER.debug("Channel active. Sending SASL NUL byte to {}.", ctx.channel().remoteAddress());

    // Send the NUL byte
    ctx.writeAndFlush(Unpooled.wrappedBuffer(NUL_BYTE_ARRAY))
            .addListener(new WriteOperationListener<>(LOGGER, future -> {
              if (future.isSuccess()) {
                LOGGER.debug("SASL NUL byte sent successfully.");
                // Fire event to signal the next stage of SASL can begin
                ctx.fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
                // Remove this handler from the pipeline as its job is done
                ctx.pipeline().remove(this);
                LOGGER.debug("SaslInitiationHandler removed from pipeline.");
              } else {
                LOGGER.error("Failed to send SASL NUL byte. Closing channel.", future.cause());
                ctx.close(); // Close channel on failure to send critical initial byte
              }
            }));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error("Exception in SaslInitiationHandler. Closing channel.", cause);
    ctx.close();
  }
}
