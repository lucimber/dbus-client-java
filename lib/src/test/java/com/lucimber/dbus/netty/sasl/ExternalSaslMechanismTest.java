/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class ExternalSaslMechanismTest {

    @Mock private ChannelHandlerContext context;

    @Mock private DefaultEventLoop eventLoop;

    private ExternalSaslMechanism mechanism;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mechanism = new ExternalSaslMechanism();

        when(context.executor()).thenReturn(eventLoop);
        when(eventLoop.newPromise())
                .thenAnswer(invocation -> new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
    }

    @Test
    void testGetName() {
        assertEquals("EXTERNAL", mechanism.getName());
    }

    @Test
    void testInitWithValidAuthorizationId() throws SaslMechanismException {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);
            assertFalse(mechanism.isComplete());
        }
    }

    @Test
    void testInitWithNullAuthorizationId() {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn(null);

            assertThrows(SaslMechanismException.class, () -> mechanism.init(context));
        }
    }

    @Test
    void testInitWithEmptyAuthorizationId() {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("");

            assertThrows(SaslMechanismException.class, () -> mechanism.init(context));
        }
    }

    @Test
    void testInitWithResolverException() {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver
                    .when(AuthorizationIdResolver::resolve)
                    .thenThrow(new SaslMechanismException("Failed to resolve"));

            assertThrows(SaslMechanismException.class, () -> mechanism.init(context));
        }
    }

    @Test
    void testGetInitialResponseAsync() throws Exception {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);

            Future<String> responseFuture = mechanism.getInitialResponseAsync(context);
            String response = responseFuture.get(5, TimeUnit.SECONDS);

            assertNotNull(response);
            String decoded = new String(SaslUtil.hexDecode(response), StandardCharsets.US_ASCII);
            assertEquals("1000", decoded);
            assertTrue(mechanism.isComplete());
        }
    }

    @Test
    void testGetInitialResponseAsyncWithoutInit() throws Exception {
        Future<String> responseFuture = mechanism.getInitialResponseAsync(context);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(exception.getCause().getMessage().contains("Authorization ID not initialized"));
    }

    @Test
    void testGetInitialResponseAsyncWithNullAuthorizationId() throws Exception {
        // Test scenario where authorization ID is set to null after init
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);

            // Use reflection to set authorizationId to null
            java.lang.reflect.Field authIdField =
                    ExternalSaslMechanism.class.getDeclaredField("authorizationId");
            authIdField.setAccessible(true);
            authIdField.set(mechanism, null);

            Future<String> responseFuture = mechanism.getInitialResponseAsync(context);

            Exception exception =
                    assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
            assertTrue(exception.getCause() instanceof SaslMechanismException);
            assertTrue(
                    exception.getCause().getMessage().contains("Authorization ID not initialized"));
        }
    }

    @Test
    void testProcessChallengeAsync() throws Exception {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);

            Future<String> responseFuture =
                    mechanism.processChallengeAsync(context, "unexpected_challenge");

            Exception exception =
                    assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
            assertTrue(exception.getCause() instanceof SaslMechanismException);
            assertTrue(
                    exception
                            .getCause()
                            .getMessage()
                            .contains("EXTERNAL does not support challenges"));
            assertTrue(exception.getCause().getMessage().contains("unexpected_challenge"));
        }
    }

    @Test
    void testDispose() throws Exception {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);
            Future<String> future = mechanism.getInitialResponseAsync(context);
            future.get(5, TimeUnit.SECONDS);

            assertTrue(mechanism.isComplete());

            mechanism.dispose();

            assertFalse(mechanism.isComplete());
        }
    }

    @Test
    void testMultipleGetInitialResponseCalls() throws Exception {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn("1000");

            mechanism.init(context);

            // First call
            Future<String> responseFuture1 = mechanism.getInitialResponseAsync(context);
            String response1 = responseFuture1.get(5, TimeUnit.SECONDS);

            // Second call
            Future<String> responseFuture2 = mechanism.getInitialResponseAsync(context);
            String response2 = responseFuture2.get(5, TimeUnit.SECONDS);

            assertEquals(response1, response2);
            assertTrue(mechanism.isComplete());
        }
    }

    @Test
    void testWithWindowsSid() throws Exception {
        try (MockedStatic<AuthorizationIdResolver> mockedResolver =
                mockStatic(AuthorizationIdResolver.class)) {
            String windowsSid = "S-1-5-21-1234567890-1234567890-1234567890-1001";
            mockedResolver.when(AuthorizationIdResolver::resolve).thenReturn(windowsSid);

            mechanism.init(context);

            Future<String> responseFuture = mechanism.getInitialResponseAsync(context);
            String response = responseFuture.get(5, TimeUnit.SECONDS);

            assertNotNull(response);
            String decoded = new String(SaslUtil.hexDecode(response), StandardCharsets.US_ASCII);
            assertEquals(windowsSid, decoded);
            assertTrue(mechanism.isComplete());
        }
    }
}
