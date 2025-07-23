/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionContext;
import com.lucimber.dbus.connection.ConnectionHandle;
import com.lucimber.dbus.connection.ConnectionStrategy;

/**
 * Netty-based connection strategy for Unix domain socket transport.
 *
 * <p>This strategy handles connections over Unix domain sockets using native transports (Epoll on
 * Linux, KQueue on macOS) for optimal performance.
 */
public final class NettyUnixSocketStrategy implements ConnectionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyUnixSocketStrategy.class);

    @Override
    public boolean supports(SocketAddress address) {
        return address instanceof DomainSocketAddress;
    }

    @Override
    public CompletionStage<ConnectionHandle> connect(
            SocketAddress address, ConnectionConfig config, ConnectionContext context) {
        if (!isAvailable()) {
            CompletableFuture<ConnectionHandle> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new UnsupportedOperationException(
                            "Unix Domain Sockets require Epoll (Linux) or KQueue (macOS) native transport, but neither is available."));
            return future;
        }

        EventLoopGroup workerGroup = createEventLoopGroup();
        Promise<Void> nettyConnectPromise = workerGroup.next().newPromise();
        CompletableFuture<ConnectionHandle> handleFuture = new CompletableFuture<>();

        RealityCheckpoint realityCheckpoint = createRealityCheckpoint(context, config);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(workerGroup)
                .channel(getChannelClass())
                .handler(new DBusChannelInitializer(realityCheckpoint, nettyConnectPromise));

        LOGGER.info("Attempting Unix domain socket connection to {}", address);
        ChannelFuture channelFuture = bootstrap.connect(address);

        channelFuture.addListener(
                future -> {
                    if (future.isSuccess()) {
                        LOGGER.debug("Unix domain socket connection established to {}", address);
                        // Create handle but wait for D-Bus handshake completion before resolving
                        // the future
                        NettyConnectionHandle handle =
                                new NettyConnectionHandle(
                                        channelFuture.channel(),
                                        workerGroup,
                                        config,
                                        realityCheckpoint);

                        // The nettyConnectPromise will be completed by ConnectionCompletionHandler
                        // when MANDATORY_NAME_ACQUIRED event is received
                        nettyConnectPromise.addListener(
                                connectResult -> {
                                    if (connectResult.isSuccess()) {
                                        LOGGER.debug(
                                                "D-Bus handshake completed for Unix domain socket");
                                        // Notify context that connection is fully established
                                        context.onConnectionEstablished();
                                        handleFuture.complete(handle);
                                    } else {
                                        LOGGER.error(
                                                "D-Bus handshake failed for Unix domain socket",
                                                connectResult.cause());
                                        context.onError(connectResult.cause());
                                        handleFuture.completeExceptionally(connectResult.cause());
                                    }
                                });
                    } else {
                        LOGGER.error(
                                "Failed to establish Unix domain socket connection to {}",
                                address,
                                future.cause());
                        handleFuture.completeExceptionally(future.cause());
                    }
                });

        return handleFuture;
    }

    @Override
    public String getTransportName() {
        if (Epoll.isAvailable()) {
            return "Epoll (Unix Domain Socket)";
        } else if (KQueue.isAvailable()) {
            return "KQueue (Unix Domain Socket)";
        } else {
            return "Unix Domain Socket (unavailable)";
        }
    }

    @Override
    public boolean isAvailable() {
        return Epoll.isAvailable() || KQueue.isAvailable();
    }

    private EventLoopGroup createEventLoopGroup() {
        if (Epoll.isAvailable()) {
            LOGGER.debug("Creating Epoll EventLoopGroup for Unix domain socket");
            return new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        } else if (KQueue.isAvailable()) {
            LOGGER.debug("Creating KQueue EventLoopGroup for Unix domain socket");
            return new MultiThreadIoEventLoopGroup(1, KQueueIoHandler.newFactory());
        } else {
            throw new UnsupportedOperationException("No suitable native transport available");
        }
    }

    private Class<? extends io.netty.channel.Channel> getChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollDomainSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueDomainSocketChannel.class;
        } else {
            throw new UnsupportedOperationException("No suitable native transport available");
        }
    }

    private RealityCheckpoint createRealityCheckpoint(
            ConnectionContext context, ConnectionConfig config) {
        // Extract the connection from the context - this requires the context to provide access
        if (!(context instanceof NettyConnectionContext nettyContext)) {
            throw new IllegalArgumentException(
                    "Unix socket strategy requires NettyConnectionContext");
        }

        // Create executor service for this connection
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newFixedThreadPool(
                        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                        runnable -> {
                            Thread t =
                                    new Thread(
                                            runnable,
                                            "dbus-app-worker-" + System.identityHashCode(runnable));
                            t.setDaemon(true);
                            return t;
                        });

        // Get the connection from context - this will need to be added to NettyConnectionContext
        Connection connection = nettyContext.getConnection();
        return new RealityCheckpoint(executor, connection, config.getMethodCallTimeoutMs());
    }
}
