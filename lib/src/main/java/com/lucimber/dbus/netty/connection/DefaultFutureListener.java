/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.util.LoggerUtils;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;

@SuppressWarnings("PMD.LoggerIsNotStaticFinal")
final class DefaultFutureListener<T extends Future<?>> implements GenericFutureListener<T> {

  private final Consumer<T> futureConsumer;
  private final Logger logger;

  DefaultFutureListener(final Logger logger) {
    this(logger, null);
  }

  DefaultFutureListener(final Logger logger, final Consumer<T> futureConsumer) {
    this.logger = Objects.requireNonNull(logger);
    this.futureConsumer = futureConsumer;
  }

  @Override
  public void operationComplete(final T future) {
    if (future.isSuccess()) {
      LoggerUtils.debug(logger, () -> "I/O operation was completed successfully.");
    } else if (future.cause() != null) {
      LoggerUtils.error(logger, () -> "I/O operation was completed with failure.", future.cause());
    } else if (future.isCancelled()) {
      LoggerUtils.debug(logger, () -> "I/O operation was completed by cancellation.");
    }
    if (futureConsumer != null) {
      futureConsumer.accept(future);
    }
  }
}
