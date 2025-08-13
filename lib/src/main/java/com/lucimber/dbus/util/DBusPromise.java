/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Promise-style utilities for working with D-Bus asynchronous operations.
 *
 * <p>This class provides a fluent API for handling D-Bus responses with better error handling and
 * type conversion.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DBusPromise.from(connection.sendRequest(methodCall))
 *     .timeout(Duration.ofSeconds(5))
 *     .mapReturn(payload -> payload.get(0))
 *     .as(DBusString.class)
 *     .thenAccept(result -> System.out.println("Result: " + result))
 *     .exceptionally(error -> {
 *         System.err.println("Failed: " + error.getMessage());
 *         return null;
 *     });
 * }</pre>
 */
public class DBusPromise<T> {

    private final CompletionStage<T> stage;

    private DBusPromise(CompletionStage<T> stage) {
        this.stage = stage;
    }

    /**
     * Creates a DBusPromise from a D-Bus message completion stage.
     *
     * @param messageStage the completion stage from a D-Bus request
     * @return a new DBusPromise
     */
    public static DBusPromise<InboundMessage> from(CompletionStage<InboundMessage> messageStage) {
        return new DBusPromise<>(messageStage);
    }

    /**
     * Creates a DBusPromise from a value.
     *
     * @param value the value
     * @param <U> the value type
     * @return a completed DBusPromise
     */
    public static <U> DBusPromise<U> completed(U value) {
        return new DBusPromise<>(CompletableFuture.completedFuture(value));
    }

    /**
     * Creates a failed DBusPromise.
     *
     * @param error the error
     * @param <U> the value type
     * @return a failed DBusPromise
     */
    public static <U> DBusPromise<U> failed(Throwable error) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return new DBusPromise<>(future);
    }

    /**
     * Applies a timeout to the operation.
     *
     * @param duration the timeout duration
     * @return a new DBusPromise with timeout
     */
    public DBusPromise<T> timeout(Duration duration) {
        CompletableFuture<T> future = stage.toCompletableFuture();
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();

        future.whenComplete(
                (result, error) -> {
                    if (error != null) {
                        timeoutFuture.completeExceptionally(error);
                    } else {
                        timeoutFuture.complete(result);
                    }
                });

        // Schedule timeout
        CompletableFuture.delayedExecutor(duration.toMillis(), TimeUnit.MILLISECONDS)
                .execute(
                        () ->
                                timeoutFuture.completeExceptionally(
                                        new DBusTimeoutException(
                                                "Operation timed out after " + duration)));

        return new DBusPromise<>(timeoutFuture);
    }

    /**
     * Maps the result using a function.
     *
     * @param mapper the mapping function
     * @param <U> the new type
     * @return a new DBusPromise with the mapped value
     */
    public <U> DBusPromise<U> map(Function<? super T, ? extends U> mapper) {
        return new DBusPromise<>(stage.thenApply(mapper));
    }

    /**
     * Maps an InboundMessage to its return payload. Automatically handles error responses.
     *
     * @return a new DBusPromise with the payload
     */
    public DBusPromise<List<DBusType>> mapReturn() {
        return new DBusPromise<>(
                stage.thenApply(
                        value -> {
                            if (!(value instanceof InboundMessage)) {
                                throw new IllegalStateException(
                                        "Expected InboundMessage but got " + value.getClass());
                            }

                            InboundMessage message = (InboundMessage) value;

                            if (message instanceof InboundError) {
                                InboundError error = (InboundError) message;
                                String errorMessage = "";
                                if (error.getPayload() != null && !error.getPayload().isEmpty()) {
                                    DBusType firstArg = error.getPayload().get(0);
                                    if (firstArg instanceof DBusString) {
                                        errorMessage = ((DBusString) firstArg).getDelegate();
                                    }
                                }
                                throw new DBusErrorException(
                                        error.getErrorName().toString(), errorMessage);
                            }

                            if (message instanceof InboundMethodReturn) {
                                return ((InboundMethodReturn) message).getPayload();
                            }

                            throw new IllegalStateException(
                                    "Unexpected message type: " + message.getClass());
                        }));
    }

    /**
     * Maps the first element of a payload list.
     *
     * @param type the expected D-Bus type class
     * @param <U> the D-Bus type
     * @return a new DBusPromise with the first element
     */
    @SuppressWarnings("unchecked")
    public <U extends DBusType> DBusPromise<U> firstAs(Class<U> type) {
        return map(
                value -> {
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            Object first = list.get(0);
                            if (type.isInstance(first)) {
                                return (U) first;
                            }
                            throw new ClassCastException(
                                    "Expected "
                                            + type.getSimpleName()
                                            + " but got "
                                            + first.getClass().getSimpleName());
                        }
                        throw new IllegalStateException("Empty payload");
                    }
                    throw new IllegalStateException("Expected List but got " + value.getClass());
                });
    }

    /**
     * Casts the result to a specific type.
     *
     * @param type the target type class
     * @param <U> the target type
     * @return a new DBusPromise with the cast value
     */
    @SuppressWarnings("unchecked")
    public <U> DBusPromise<U> as(Class<U> type) {
        return map(
                value -> {
                    if (type.isInstance(value)) {
                        return (U) value;
                    }
                    throw new ClassCastException(
                            "Expected "
                                    + type.getSimpleName()
                                    + " but got "
                                    + value.getClass().getSimpleName());
                });
    }

    /**
     * Handles both success and failure cases.
     *
     * @param action the action to perform
     * @return a new DBusPromise
     */
    public DBusPromise<Void> thenAccept(java.util.function.Consumer<? super T> action) {
        return new DBusPromise<>(stage.thenAccept(action));
    }

    /**
     * Handles exceptions.
     *
     * @param fn the exception handler
     * @return a new DBusPromise
     */
    public DBusPromise<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return new DBusPromise<>(stage.exceptionally(fn));
    }

    /**
     * Converts to a CompletableFuture.
     *
     * @return the underlying CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture() {
        return stage.toCompletableFuture();
    }

    /**
     * Gets the result synchronously.
     *
     * @return the result
     * @throws Exception if the operation fails
     */
    public T get() throws Exception {
        return stage.toCompletableFuture().get();
    }

    /**
     * Gets the result synchronously with a timeout.
     *
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return the result
     * @throws Exception if the operation fails or times out
     */
    public T get(long timeout, TimeUnit unit) throws Exception {
        return stage.toCompletableFuture().get(timeout, unit);
    }

    /** Exception thrown when a D-Bus operation times out. */
    public static class DBusTimeoutException extends RuntimeException {
        public DBusTimeoutException(String message) {
            super(message);
        }
    }

    /** Exception thrown when a D-Bus error is received. */
    public static class DBusErrorException extends RuntimeException {
        private final String errorName;

        public DBusErrorException(String errorName, String message) {
            super(message);
            this.errorName = errorName;
        }

        public String getErrorName() {
            return errorName;
        }
    }
}
