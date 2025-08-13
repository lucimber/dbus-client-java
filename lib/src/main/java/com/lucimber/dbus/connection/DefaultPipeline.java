/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe pipeline of {@link OutboundHandler} instances associated with a {@link Connection}.
 * This class maintains the order of handlers and ensures propagation of read, write, exception, and
 * user-defined events.
 */
public final class DefaultPipeline implements Pipeline {

    private final Connection connection;
    private final InternalContext head;
    private final InternalContext tail;
    private final ConcurrentMap<String, InternalContext> nameCtxMap = new ConcurrentHashMap<>();

    /** Constructs a new pipeline for the given connection. */
    public DefaultPipeline(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        head = new InternalContext(connection, this, "HEAD", new InternalHeadHandler());
        tail = new InternalContext(connection, this, "TAIL", new InternalTailHandler());
        head.setNext(tail);
        tail.setPrev(head);
    }

    @Override
    public synchronized Pipeline addLast(String name, Handler handler) {
        if (nameCtxMap.containsKey(name)) {
            throw new IllegalArgumentException("Handler name already exists: " + name);
        }

        final InternalContext newCtx = new InternalContext(connection, this, name, handler);
        final InternalContext prev = tail.getPrev();
        prev.setNext(newCtx);
        newCtx.setPrev(prev);
        newCtx.setNext(tail);
        tail.setPrev(newCtx);

        nameCtxMap.put(name, newCtx);
        handler.onHandlerAdded(newCtx);

        return this;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public synchronized Pipeline remove(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank.");
        }

        if (name.equalsIgnoreCase("HEAD") || name.equalsIgnoreCase("TAIL")) {
            throw new IllegalArgumentException("Removal of head or tail not allowed.");
        }

        final InternalContext ctx = nameCtxMap.remove(name);
        if (ctx == null) {
            throw new IllegalArgumentException("No such handler: " + name);
        }

        ctx.getHandler().onHandlerRemoved(ctx);

        final InternalContext prev = ctx.getPrev();
        final InternalContext next = ctx.getNext();
        prev.setNext(next);
        next.setPrev(prev);
        ctx.setPrev(null);
        ctx.setNext(null);

        return this;
    }

    @Override
    public void propagateConnectionActive() {
        head.onConnectionActive();
    }

    @Override
    public void propagateConnectionInactive() {
        head.onConnectionInactive();
    }

    @Override
    public void propagateInboundMessage(InboundMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");
        head.handleInboundMessage(msg);
    }

    @Override
    public void propagateInboundFailure(Throwable cause) {
        Objects.requireNonNull(cause, "cause must not be null");
        head.handleInboundFailure(cause);
    }

    @Override
    public void propagateOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future) {
        Objects.requireNonNull(msg, "msg must not be null");
        Objects.requireNonNull(future, "future must not be null");
        tail.handleOutboundMessage(msg, future);
    }
}
