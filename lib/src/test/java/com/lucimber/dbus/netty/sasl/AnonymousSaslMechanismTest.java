/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnonymousSaslMechanismTest {

    private ChannelHandlerContext ctx;
    private AnonymousSaslMechanism mechanism;

    @BeforeEach
    void setUp() {
        ctx = mock(ChannelHandlerContext.class);
        mechanism = new AnonymousSaslMechanism();
    }

    @Test
    void testGetName() {
        assertEquals("ANONYMOUS", mechanism.getName());
    }

    @Test
    void testInitialState() {
        assertFalse(mechanism.isComplete());
    }

    @Test
    void testInit() {
        // Init should not throw any exceptions
        assertDoesNotThrow(() -> mechanism.init(ctx));

        // Should still not be complete after init
        assertFalse(mechanism.isComplete());
    }

    @Test
    void testGetInitialResponseAsync() throws Exception {
        Future<String> responseFuture = mechanism.getInitialResponseAsync(ctx);
        assertNotNull(responseFuture);

        // The future should be completed immediately
        assertTrue(responseFuture.isDone());
        assertFalse(responseFuture.isCancelled());

        // ANONYMOUS mechanism returns null as the response
        String response = responseFuture.get();
        assertNull(response);

        // Should be complete after getting initial response
        assertTrue(mechanism.isComplete());
    }

    @Test
    void testProcessChallengeAsyncAlwaysFails() {
        Future<String> challengeFuture = mechanism.processChallengeAsync(ctx, "test-challenge");
        assertNotNull(challengeFuture);

        // The future should be completed immediately with failure
        assertTrue(challengeFuture.isDone());
        assertFalse(challengeFuture.isCancelled());

        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            challengeFuture.get();
                        });

        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(
                exception
                        .getCause()
                        .getMessage()
                        .contains("ANONYMOUS mechanism does not support server challenges"));
        assertTrue(exception.getCause().getMessage().contains("test-challenge"));
    }

    @Test
    void testProcessChallengeWithNullChallenge() {
        Future<String> challengeFuture = mechanism.processChallengeAsync(ctx, null);

        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            challengeFuture.get();
                        });

        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(
                exception
                        .getCause()
                        .getMessage()
                        .contains("ANONYMOUS mechanism does not support server challenges"));
        assertTrue(exception.getCause().getMessage().contains("null"));
    }

    @Test
    void testProcessChallengeWithEmptyChallenge() {
        Future<String> challengeFuture = mechanism.processChallengeAsync(ctx, "");

        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            challengeFuture.get();
                        });

        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(
                exception
                        .getCause()
                        .getMessage()
                        .contains("ANONYMOUS mechanism does not support server challenges"));
    }

    @Test
    void testDispose() throws Exception {
        // Complete the mechanism first
        Future<String> responseFuture = mechanism.getInitialResponseAsync(ctx);
        responseFuture.get(); // Wait for completion
        assertTrue(mechanism.isComplete());

        // Dispose should reset the state
        mechanism.dispose();
        assertFalse(mechanism.isComplete());
    }

    @Test
    void testDisposeWithoutCompletion() {
        // Dispose should work even if the mechanism was never completed
        assertFalse(mechanism.isComplete());
        assertDoesNotThrow(() -> mechanism.dispose());
        assertFalse(mechanism.isComplete());
    }

    @Test
    void testMultipleInitCalls() throws Exception {
        mechanism.init(ctx);
        mechanism.init(ctx); // Should not fail

        Future<String> responseFuture = mechanism.getInitialResponseAsync(ctx);
        String response = responseFuture.get();

        assertNull(response);
        assertTrue(mechanism.isComplete());
    }

    @Test
    void testMultipleGetInitialResponseCalls() throws Exception {
        // First call
        Future<String> responseFuture1 = mechanism.getInitialResponseAsync(ctx);
        String response1 = responseFuture1.get();
        assertNull(response1);
        assertTrue(mechanism.isComplete());

        // Second call should also work
        Future<String> responseFuture2 = mechanism.getInitialResponseAsync(ctx);
        String response2 = responseFuture2.get();
        assertNull(response2);
        assertTrue(mechanism.isComplete());
    }

    @Test
    void testSaslMechanismInterface() {
        assertTrue(mechanism instanceof SaslMechanism);
    }

    @Test
    void testMechanismNameIsConstant() {
        // Name should always be the same
        assertEquals("ANONYMOUS", mechanism.getName());
        assertEquals("ANONYMOUS", mechanism.getName());

        // Name should not change even after operations
        mechanism.init(ctx);
        assertEquals("ANONYMOUS", mechanism.getName());

        mechanism.getInitialResponseAsync(ctx);
        assertEquals("ANONYMOUS", mechanism.getName());

        mechanism.dispose();
        assertEquals("ANONYMOUS", mechanism.getName());
    }
}
