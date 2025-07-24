/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import com.lucimber.dbus.util.LoggerUtils;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;

/**
 * A {@link GenericFutureListener} that provides consistent logging for write operations and
 * optionally chains custom completion logic.
 *
 * <p>This listener automatically logs the outcome of write operations using the {@link
 * LoggerUtils#TRANSPORT} marker with appropriate log levels:
 *
 * <ul>
 *   <li><strong>DEBUG</strong> - Successful operations
 *   <li><strong>ERROR</strong> - Failed operations with exception details
 *   <li><strong>WARN</strong> - Cancelled operations
 * </ul>
 *
 * <p>After logging, the listener can optionally execute custom completion logic via the provided
 * {@link Consumer} function.
 *
 * <h2>Usage Examples</h2>
 *
 * <p><strong>Basic logging only:</strong>
 *
 * <pre>{@code
 * ctx.writeAndFlush(message)
 *    .addListener(new WriteOperationListener<>(LOGGER));
 * }</pre>
 *
 * <p><strong>With custom completion logic:</strong>
 *
 * <pre>{@code
 * ctx.writeAndFlush(message)
 *    .addListener(new WriteOperationListener<>(LOGGER, future -> {
 *        if (future.isSuccess()) {
 *            // Handle successful write
 *            processSuccessfulWrite();
 *        } else {
 *            // Handle failed write
 *            handleWriteFailure(future.cause());
 *        }
 *    }));
 * }</pre>
 *
 * <p><strong>SASL authentication example:</strong>
 *
 * <pre>{@code
 * ctx.writeAndFlush(authMessage)
 *    .addListener(new WriteOperationListener<>(LOGGER, future -> {
 *        if (future.isSuccess()) {
 *            currentState = State.AWAITING_RESPONSE;
 *            startResponseTimeout(ctx);
 *        } else {
 *            failAuthentication(ctx, "Failed to send auth message");
 *        }
 *    }));
 * }</pre>
 *
 * <p>This class is thread-safe and can be used across multiple channels and handlers. The logger
 * instance is not required to be static final, making it suitable for use in various contexts where
 * different loggers may be needed.
 *
 * @param <T> the type of future being listened to, must extend {@link Future}
 * @see GenericFutureListener
 * @see LoggerUtils#TRANSPORT
 * @since 1.0
 */
@SuppressWarnings("PMD.LoggerIsNotStaticFinal")
public final class WriteOperationListener<T extends Future<?>> implements GenericFutureListener<T> {

    private final Consumer<T> futureConsumer;
    private final Logger logger;

    /**
     * Creates a new WriteOperationListener that only performs logging.
     *
     * <p>This constructor creates a listener that will log the outcome of write operations but will
     * not execute any custom completion logic.
     *
     * @param logger the logger to use for logging write operation outcomes; must not be null
     * @throws NullPointerException if logger is null
     */
    public WriteOperationListener(Logger logger) {
        this(logger, null);
    }

    /**
     * Creates a new WriteOperationListener with custom completion logic.
     *
     * <p>This constructor creates a listener that will log the outcome of write operations and then
     * execute the provided custom completion logic.
     *
     * @param logger the logger to use for logging write operation outcomes; must not be null
     * @param futureConsumer optional consumer to execute custom completion logic; may be null
     * @throws NullPointerException if logger is null
     */
    public WriteOperationListener(Logger logger, Consumer<T> futureConsumer) {
        this.logger = Objects.requireNonNull(logger);
        this.futureConsumer = futureConsumer;
    }

    /**
     * Called when the write operation completes, regardless of success or failure.
     *
     * <p>This method first logs the outcome of the write operation using the {@link
     * LoggerUtils#TRANSPORT} marker:
     *
     * <ul>
     *   <li>If the operation was successful, logs at DEBUG level
     *   <li>If the operation failed with an exception, logs at ERROR level with the exception
     *   <li>If the operation was cancelled, logs at WARN level
     * </ul>
     *
     * <p>After logging, if a custom completion consumer was provided during construction, it will
     * be executed with the future as its argument.
     *
     * <p>This method is thread-safe and can be called from any thread.
     *
     * @param future the completed future representing the write operation result
     */
    @Override
    public void operationComplete(T future) {
        if (future.isSuccess()) {
            logger.debug(LoggerUtils.TRANSPORT, "I/O operation was completed successfully.");
        } else if (future.cause() != null) {
            logger.error(
                    LoggerUtils.TRANSPORT,
                    "I/O operation was completed with failure.",
                    future.cause());
        } else if (future.isCancelled()) {
            logger.warn(LoggerUtils.TRANSPORT, "I/O operation was completed by cancellation.");
        }
        if (futureConsumer != null) {
            futureConsumer.accept(future);
        }
    }
}
