/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CookieSaslMechanismTest {

    @Mock private ChannelHandlerContext context;

    @Mock private DefaultEventLoop eventLoop;

    private CookieSaslMechanism mechanism;
    private String originalUsername;
    private String originalHome;

    @TempDir private Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mechanism = new CookieSaslMechanism();
        originalUsername = System.getProperty("user.name");
        originalHome = System.getProperty("user.home");

        when(context.executor()).thenReturn(eventLoop);
        when(eventLoop.newPromise())
                .thenAnswer(invocation -> new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
    }

    @AfterEach
    void tearDown() {
        if (originalUsername != null) {
            System.setProperty("user.name", originalUsername);
        }
        if (originalHome != null) {
            System.setProperty("user.home", originalHome);
        }
        mechanism.dispose();
    }

    @Test
    void testGetName() {
        assertEquals("DBUS_COOKIE_SHA1", mechanism.getName());
    }

    @Test
    void testInitWithValidUsername() throws SaslMechanismException {
        System.setProperty("user.name", "testuser");
        mechanism.init(context);
        assertFalse(mechanism.isComplete());
    }

    @Test
    void testInitWithNoUsername() {
        System.clearProperty("user.name");
        assertThrows(SaslMechanismException.class, () -> mechanism.init(context));
    }

    @Test
    void testInitWithEmptyUsername() {
        System.setProperty("user.name", "");
        assertThrows(SaslMechanismException.class, () -> mechanism.init(context));
    }

    @Test
    void testGetInitialResponseAsync() throws Exception {
        System.setProperty("user.name", "testuser");
        mechanism.init(context);

        Future<String> responseFuture = mechanism.getInitialResponseAsync(context);
        String response = responseFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(response);
        String decoded = new String(SaslUtil.hexDecode(response), StandardCharsets.UTF_8);
        assertEquals("testuser", decoded);
    }

    @Test
    void testProcessChallengeAsyncWithValidCookie() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Create .dbus-keyrings directory and cookie file
        Path keyringsDir = tempDir.resolve(".dbus-keyrings");
        Files.createDirectories(keyringsDir);
        Path cookieFile = keyringsDir.resolve("org_freedesktop_general");

        String cookieId = "123456";
        String cookieValue = "abcdef1234567890";
        String cookieContent = cookieId + " 1234567890 " + cookieValue;
        Files.write(cookieFile, cookieContent.getBytes(StandardCharsets.UTF_8));

        // Create server challenge
        String serverChallenge = "serverchal123";
        String challengeStr = "org_freedesktop_general " + cookieId + " " + serverChallenge;
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);
        String response = responseFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertTrue(mechanism.isComplete());

        // Verify response format
        String decodedResponse = new String(SaslUtil.hexDecode(response), StandardCharsets.UTF_8);
        String[] parts = decodedResponse.split(" ");
        assertEquals(2, parts.length);
    }

    @Test
    void testProcessChallengeAsyncWithInvalidFormat() throws Exception {
        System.setProperty("user.name", "testuser");
        mechanism.init(context);

        String invalidChallenge = "invalid";
        String challengeHex = SaslUtil.hexEncode(invalidChallenge.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(exception.getCause().getMessage().contains("Invalid server challenge format"));
    }

    @Test
    void testProcessChallengeAsyncWithMissingCookie() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Create empty .dbus-keyrings directory (no cookie file)
        Path keyringsDir = tempDir.resolve(".dbus-keyrings");
        Files.createDirectories(keyringsDir);

        String challengeStr = "org_freedesktop_general 123456 serverchal123";
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
        assertTrue(exception.getCause().getMessage().contains("Cookie not found"));
    }

    @Test
    void testProcessChallengeAsyncWithNonExistentCookieFile() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // No .dbus-keyrings directory at all
        String challengeStr = "org_freedesktop_general 123456 serverchal123";
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
    }

    @Test
    void testPathTraversalProtection() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Try path traversal with ".."
        String challengeStr = "../../../etc/passwd 123456 serverchal123";
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
    }

    @Test
    void testInvalidCookieContextCharacters() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Try with forward slash in context
        String challengeStr = "some/path 123456 serverchal123";
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
    }

    @Test
    void testDispose() throws SaslMechanismException {
        System.setProperty("user.name", "testuser");
        mechanism.init(context);

        mechanism.dispose();

        assertFalse(mechanism.isComplete());
    }

    @Test
    void testCookieFileWithMultipleEntries() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Create .dbus-keyrings directory and cookie file with multiple entries
        Path keyringsDir = tempDir.resolve(".dbus-keyrings");
        Files.createDirectories(keyringsDir);
        Path cookieFile = keyringsDir.resolve("org_freedesktop_general");

        String targetCookieId = "456789";
        String targetCookieValue = "targetvalue123";
        String cookieContent =
                "123456 1234567890 othercookie\n"
                        + targetCookieId
                        + " 1234567890 "
                        + targetCookieValue
                        + "\n"
                        + "789012 1234567890 anothercookie";
        Files.write(cookieFile, cookieContent.getBytes(StandardCharsets.UTF_8));

        // Create server challenge
        String serverChallenge = "serverchal456";
        String challengeStr = "org_freedesktop_general " + targetCookieId + " " + serverChallenge;
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);
        String response = responseFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertTrue(mechanism.isComplete());
    }

    @Test
    void testEmptyCookieContext() throws Exception {
        System.setProperty("user.name", "testuser");
        System.setProperty("user.home", tempDir.toString());
        mechanism.init(context);

        // Empty context name
        String challengeStr = " 123456 serverchal123";
        String challengeHex = SaslUtil.hexEncode(challengeStr.getBytes(StandardCharsets.UTF_8));

        Future<String> responseFuture = mechanism.processChallengeAsync(context, challengeHex);

        Exception exception =
                assertThrows(Exception.class, () -> responseFuture.get(5, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SaslMechanismException);
    }
}
