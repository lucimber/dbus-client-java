package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslAuthMessage;
import com.lucimber.dbus.connection.sasl.SaslBeginMessage;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslDataMessage;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import com.lucimber.dbus.util.SaslUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SaslCookieInboundHandler extends AbstractSaslInboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String directoryPath;
  private final String identity;

  SaslCookieInboundHandler(final String identity, final String directoryPath) {
    Objects.requireNonNull(identity);
    this.identity = SaslUtils.toHexadecimalString(identity.getBytes(StandardCharsets.US_ASCII));
    this.directoryPath = Objects.requireNonNull(directoryPath);
  }

  private static String[] decodeDataValues(final String encodedString) {
    LoggerUtils.trace(LOGGER, () -> "Decoding data values.");
    final byte[] bytes = SaslUtils.fromHexadecimalString(encodedString);
    return new String(bytes, StandardCharsets.US_ASCII).split("\\s");
  }

  private static String readCookieFromFile(final Path cookieFilePath, final String cookieId) throws IOException {
    LoggerUtils.trace(LOGGER, () -> "Reading cookie from file.");
    try (Stream<String> stream = Files.lines(cookieFilePath, StandardCharsets.US_ASCII)) {
      final Optional<String> cookie;
      try {
        cookie = stream
                .map(line -> line.split("\\s"))
                .filter(splitString -> splitString[0].equals(cookieId))
                .map(splitString -> splitString[2])
                .findFirst();
      } catch (NullPointerException e) {
        throw new IOException(cookieId, e);
      }
      if (cookie.isPresent()) {
        return cookie.get();
      } else {
        throw new IOException(cookieId);
      }
    }
  }

  private static ChannelFuture sendAuthMessage(final ChannelHandlerContext ctx, final String identity) {
    LoggerUtils.debug(LOGGER, () -> "Sending auth message.");
    final String s = String.format("%s %s", SaslAuthMechanism.COOKIE, identity);
    return ctx.writeAndFlush(new SaslAuthMessage(s));
  }

  private static ChannelFuture sendDataMessage(final ChannelHandlerContext ctx, final String ownChallenge,
                                               final String hexDigest) {
    LoggerUtils.debug(LOGGER, () -> "Sending data message.");
    final String value = String.format("%s %s", ownChallenge, hexDigest);
    final String hexValue = SaslUtils.toHexadecimalString(value.getBytes(StandardCharsets.US_ASCII));
    final SaslMessage msg = new SaslDataMessage(hexValue);
    return ctx.writeAndFlush(msg);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    sendAuthMessage(ctx, identity)
            .addListener(new DefaultFutureListener<>(LOGGER, v -> setCurrentState(State.WAITING_FOR_DATA)));
  }

  @Override
  void handleMessageInDataState(final ChannelHandlerContext ctx, final SaslMessage msg) {
    LoggerUtils.trace(LOGGER, () -> "Handling incoming message in DATA state.");
    final SaslCommandName commandName = msg.getCommandName();
    if (commandName.equals(SaslCommandName.SHARED_DATA)) {
      handleDataMessage(ctx, msg);
    } else if (commandName.equals(SaslCommandName.SHARED_ERROR)) {
      sendCancelMessage(ctx);
      setCurrentState(State.WAITING_FOR_REJECT);
    } else if (commandName.equals(SaslCommandName.SERVER_OK)) {
      handleOkMessage(ctx);
    } else if (commandName.equals(SaslCommandName.SERVER_REJECTED)) {
      ctx.close();
    } else {
      respondToWrongState(ctx);
    }
  }

  @Override
  void handleMessageInOkState(final ChannelHandlerContext ctx, final SaslMessage msg) {
    LoggerUtils.trace(LOGGER, () -> "Handling incoming message in OK state.");
    final SaslCommandName commandName = msg.getCommandName();
    if (commandName.equals(SaslCommandName.SERVER_OK)) {
      handleOkMessage(ctx);
    } else if (commandName.equals(SaslCommandName.SERVER_REJECTED)
            || commandName.equals(SaslCommandName.SHARED_DATA)
            || commandName.equals(SaslCommandName.SHARED_ERROR)) {
      sendCancelMessage(ctx);
      setCurrentState(State.WAITING_FOR_REJECT);
    } else {
      respondToWrongState(ctx);
    }
  }

  private void handleDataMessage(final ChannelHandlerContext ctx, final SaslMessage msg) {
    if (msg.getCommandValue().isPresent()) {
      try {
        final String encodedString = msg.getCommandValue().get();
        final String[] dataValues = decodeDataValues(encodedString);
        final int numOfDataValues = 3;
        if (dataValues.length != numOfDataValues) {
          throw new IndexOutOfBoundsException();
        }
        final Path path = Paths.get(directoryPath);
        final Path cookieFilePath = SaslUtils.locateCookieFile(path, dataValues[0]);
        final String cookie = readCookieFromFile(cookieFilePath, dataValues[1]);
        final int challengeLength = 40;
        final String challenge = SaslUtils.generateChallengeString(challengeLength);
        final String compositeString = String.format("%s:%s:%s",
                dataValues[2], challenge, cookie);
        final byte[] digest = SaslUtils.computeHashValue(compositeString.getBytes(StandardCharsets.US_ASCII));
        final String hexDigest = SaslUtils.toHexadecimalString(digest);
        sendDataMessage(ctx, challenge, hexDigest)
            .addListener(new DefaultFutureListener<>(LOGGER, v -> setCurrentState(State.WAITING_FOR_OK)));
      } catch (AccessDeniedException | NoSuchAlgorithmException e) {
        sendCancelMessage(ctx);
        setCurrentState(State.WAITING_FOR_REJECT);
      } catch (IOException e) {
        final String error = "Can not open the cookie file for context.";
        sendErrorMessage(ctx, error);
      } catch (IllegalArgumentException e) {
        final String error = "Argument is not properly encoded.";
        sendErrorMessage(ctx, error);
      } catch (IndexOutOfBoundsException e) {
        final String error = "Argument contains too few information.";
        sendErrorMessage(ctx, error);
      }
    } else {
      final String error = "Missing value for DATA";
      sendErrorMessage(ctx, error);
    }
  }

  private void handleOkMessage(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Sending begin message.");
    final ChannelFuture future = ctx.writeAndFlush(new SaslBeginMessage());
    future.addListener(new DefaultFutureListener<>(LOGGER, v -> {
      LoggerUtils.trace(LOGGER, () -> "Detaching from channel pipeline.");
      ctx.pipeline().remove(this);
      LoggerUtils.debug(LOGGER, () -> "Firing SASL_AUTH_COMPLETE as user event to next channel handler.");
      ctx.pipeline().fireUserEventTriggered(CustomChannelEvent.SASL_AUTH_COMPLETE);
    }));
  }
}
