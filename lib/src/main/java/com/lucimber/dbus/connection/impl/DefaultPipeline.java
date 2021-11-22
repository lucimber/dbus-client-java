package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class DefaultPipeline implements Pipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Connection connection;
    private final ReentrantReadWriteLock contextLock;
    private final DefaultHandlerContext headContext;
    private final DefaultHandlerContext tailContext;

    DefaultPipeline(final Connection connection) {
        this.connection = Objects.requireNonNull(connection);
        headContext = new DefaultHandlerContext("HEAD_CONTEXT", this, new DefaultHeadHandler(connection));
        tailContext = new DefaultHandlerContext("TAIL_CONTEXT", this, new DefaultTailHandler());
        headContext.setNextContext(tailContext);
        tailContext.setPreviousContext(headContext);
        contextLock = new ReentrantReadWriteLock(true);
    }

    @Override
    public void addBefore(final String nameOther, final String name, final Handler handler) {
        Objects.requireNonNull(nameOther, "nameOther must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        LoggerUtils.debug(LOGGER, () -> "Adding " + name + " as handler in front of "
                + nameOther + " in connection pipeline.");
        LoggerUtils.trace(LOGGER, () -> "Acquiring write-lock.");
        final Lock writeLock = contextLock.writeLock();
        writeLock.lock();
        try {
            final boolean nameTaken = isNameTaken(name);
            if (nameTaken) {
                throw new IllegalArgumentException("name already taken");
            }
            final DefaultHandlerContext ctx = new DefaultHandlerContext(name, this, handler);
            DefaultHandlerContext tmpCtx = headContext.getNextContext();
            while (tmpCtx != null) {
                if (tmpCtx.getName().equals(nameOther)) {
                    LoggerUtils.trace(LOGGER, () -> "Found handler in pipeline.");
                    final DefaultHandlerContext previousCtx = tmpCtx.getPreviousContext();
                    ctx.setPreviousContext(previousCtx);
                    ctx.setNextContext(tmpCtx);
                    previousCtx.setNextContext(ctx);
                    tmpCtx.setPreviousContext(ctx);
                    break;
                } else {
                    tmpCtx = tmpCtx.getNextContext();
                }
            }
        } finally {
            writeLock.unlock();
            LoggerUtils.trace(LOGGER, () -> "Released write-lock.");
        }
    }

    @Override
    public void addLast(final String name, final Handler handler) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        LoggerUtils.debug(LOGGER, () -> "Adding " + name + " as last handler in connection pipeline.");
        LoggerUtils.trace(LOGGER, () -> "Acquiring write-lock.");
        final Lock writeLock = contextLock.writeLock();
        writeLock.lock();
        try {
            final boolean nameTaken = isNameTaken(name);
            if (nameTaken) {
                throw new IllegalArgumentException("name already taken");
            }
            final DefaultHandlerContext ctx = new DefaultHandlerContext(name, this, handler);
            final DefaultHandlerContext previousCtx = tailContext.getPreviousContext();
            ctx.setPreviousContext(previousCtx);
            ctx.setNextContext(tailContext);
            previousCtx.setNextContext(ctx);
            tailContext.setPreviousContext(ctx);
        } finally {
            writeLock.unlock();
            LoggerUtils.trace(LOGGER, () -> "Released write-lock.");
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void remove(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        LoggerUtils.debug(LOGGER, () -> "Removing " + name + " from connection pipeline.");
        LoggerUtils.trace(LOGGER, () -> "Acquiring write-lock.");
        final Lock writeLock = contextLock.writeLock();
        writeLock.lock();
        try {
            DefaultHandlerContext ctx = headContext.getNextContext();
            while (ctx != null) {
                if (ctx.getName().equals(name)) {
                    LoggerUtils.trace(LOGGER, () -> "Found handler in pipeline.");
                    final DefaultHandlerContext previousCtx = ctx.getPreviousContext();
                    final DefaultHandlerContext nextCtx = ctx.getNextContext();
                    previousCtx.setNextContext(nextCtx);
                    nextCtx.setPreviousContext(previousCtx);
                    ctx = null;
                } else {
                    ctx = ctx.getNextContext();
                }
            }
        } finally {
            writeLock.unlock();
            LoggerUtils.trace(LOGGER, () -> "Released write-lock.");
        }
    }

    @Override
    public void replace(final String oldName, final String newName, final Handler handler) {
        Objects.requireNonNull(oldName, "oldName must not be null");
        Objects.requireNonNull(newName, "newName must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        LoggerUtils.debug(LOGGER, () -> "Replacing " + oldName + " with " + newName + " in connection pipeline.");
        LoggerUtils.trace(LOGGER, () -> "Acquiring write-lock.");
        final Lock writeLock = contextLock.writeLock();
        writeLock.lock();
        try {
            final DefaultHandlerContext newCtx = new DefaultHandlerContext(newName, this, handler);
            DefaultHandlerContext ctx = headContext.getNextContext();
            while (ctx != null) {
                if (ctx.getName().equals(oldName)) {
                    LoggerUtils.trace(LOGGER, () -> "Found handler in pipeline.");
                    final DefaultHandlerContext previousCtx = ctx.getPreviousContext();
                    final DefaultHandlerContext nextCtx = ctx.getNextContext();
                    newCtx.setPreviousContext(previousCtx);
                    newCtx.setNextContext(nextCtx);
                    previousCtx.setNextContext(newCtx);
                    nextCtx.setPreviousContext(newCtx);
                    ctx = null;
                } else {
                    ctx = ctx.getNextContext();
                }
            }
        } finally {
            writeLock.unlock();
            LoggerUtils.trace(LOGGER, () -> "Released write-lock.");
        }
    }

    @Override
    public void passInboundMessage(final InboundMessage msg) {
        LoggerUtils.debug(LOGGER, () -> "Passing inbound message further up the pipeline.");
        final HandlerContext ctx = headContext;
        try {
            ctx.getHandler().onInboundMessage(ctx, msg);
        } catch (Exception ex) {
            ctx.getHandler().onInboundFailure(ctx, ex);
        }
    }

    @Override
    public void passInboundFailure(final Throwable cause) {
        LoggerUtils.debug(LOGGER, () -> "Passing cause of inbound failure further up the pipeline.");
        final HandlerContext ctx = headContext;
        ctx.getHandler().onInboundFailure(ctx, cause);
    }

    @Override
    public void passOutboundMessage(final OutboundMessage msg) {
        LoggerUtils.debug(LOGGER, () -> "Passing outbound message further down the pipeline.");
        final HandlerContext ctx = tailContext;
        try {
            ctx.getHandler().onOutboundMessage(ctx, msg);
        } catch (Exception ex) {
            ctx.getHandler().onOutboundFailure(ctx, ex);
        }
    }

    @Override
    public void passOutboundFailure(final Throwable cause) {
        LoggerUtils.debug(LOGGER, () -> "Passing cause of outbound failure further up the pipeline.");
        final HandlerContext ctx = headContext;
        ctx.getHandler().onOutboundFailure(ctx, cause);
    }

    @Override
    public void passConnectionActiveEvent() {
        LoggerUtils.debug(LOGGER, () -> "Passing connection active event further up the pipeline.");
        final DefaultHandlerContext ctx = headContext;
        ctx.getHandler().onConnectionActive(ctx);
    }

    @Override
    public void passConnectionInactiveEvent() {
        LoggerUtils.debug(LOGGER, () -> "Passing connection inactive event further up the pipeline.");
        final DefaultHandlerContext ctx = headContext;
        ctx.getHandler().onConnectionInactive(ctx);
    }

    private boolean isNameTaken(final String name) {
        DefaultHandlerContext ctx = headContext.getNextContext();
        while (ctx != null) {
            if (ctx.getName().equals(name)) {
                return true;
            } else {
                ctx = ctx.getNextContext();
            }
        }
        return false;
    }

    @Override
    public void passUserEvent(final Object event) {
        LoggerUtils.debug(LOGGER, () -> "Passing user event further up the pipeline.");
        final HandlerContext ctx = headContext;
        try {
            ctx.getHandler().onUserEvent(ctx, event);
        } catch (Exception ex) {
            ctx.getHandler().onInboundFailure(ctx, ex);
        }
    }
}
