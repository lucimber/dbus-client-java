/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class NettyFutureConverter {
    static <T> CompletionStage<T> toCompletionStage(Future<T> nettyFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();

        nettyFuture.addListener(
                (GenericFutureListener<Future<T>>)
                        future -> {
                            if (future.isSuccess()) {
                                completableFuture.complete(future.getNow());
                            } else {
                                completableFuture.completeExceptionally(future.cause());
                            }
                        });

        return completableFuture;
    }
}
