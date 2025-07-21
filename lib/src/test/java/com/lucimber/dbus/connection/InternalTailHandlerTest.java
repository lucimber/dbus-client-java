/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

final class InternalTailHandlerTest {

  private InternalTailHandler handler;
  private Context mockCtx;
  private AtomicLong serialCounter;

  @BeforeEach
  void setUp() {
  handler = new InternalTailHandler();
  mockCtx = mock(Context.class);
  Connection mockConnection = mock(Connection.class);
  serialCounter = new AtomicLong(123);
  when(mockCtx.getConnection()).thenReturn(mockConnection);
  when(mockConnection.getNextSerial())
      .thenAnswer(inv -> DBusUInt32.valueOf((int) serialCounter.getAndIncrement()));
  }

  @Test
  void testUnhandledMethodCallWithReplyExpectedSendsError() {
  InboundMethodCall call = InboundMethodCall.Builder.create()
      .withSerial(DBusUInt32.valueOf(1))
      .withSender(DBusString.valueOf("org.test.Sender"))
      .withObjectPath(DBusObjectPath.valueOf("/test"))
      .withMember(DBusString.valueOf("UnknownMethod"))
      .withReplyExpected(true)
      .withInterfaceName(DBusString.valueOf("org.test.Interface"))
      .build();

  doAnswer(inv -> {
      Object msg = inv.getArgument(0);
      CompletableFuture<Void> fut = inv.getArgument(1);
      fut.complete(null);
      return null;
  }).when(mockCtx).propagateOutboundMessage(any(), any());

  handler.handleInboundMessage(mockCtx, call);

  ArgumentCaptor<OutboundMessage> messageCaptor = ArgumentCaptor.forClass(OutboundMessage.class);
  verify(mockCtx).propagateOutboundMessage(messageCaptor.capture(), any());

  Object outbound = messageCaptor.getValue();
  assertInstanceOf(OutboundError.class, outbound);

  OutboundError error = (OutboundError) outbound;
  assertEquals(DBusUInt32.valueOf(1), error.getReplySerial());
  assertEquals("org.freedesktop.DBus.Error.Failed", error.getErrorName().getDelegate());
  assertTrue(error.getSignature().isPresent());
  assertEquals(DBusSignature.valueOf("s"), error.getSignature().get());
  }

  @Test
  void testUnhandledMethodCallWithoutReplyExpectedDoesNotSendError() {
  InboundMethodCall call = InboundMethodCall.Builder.create()
      .withSerial(DBusUInt32.valueOf(2))
      .withSender(DBusString.valueOf("org.test.Sender"))
      .withObjectPath(DBusObjectPath.valueOf("/test"))
      .withMember(DBusString.valueOf("NoReplyMethod"))
      .withReplyExpected(false)
      .withInterfaceName(DBusString.valueOf("org.test.Interface"))
      .build();

  handler.handleInboundMessage(mockCtx, call);
  verify(mockCtx, never()).propagateOutboundMessage(any(), any());
  }

  @Test
  void testUnhandledMethodReturnIsLoggedAndIgnored() {
  InboundMethodReturn reply = InboundMethodReturn.Builder.create()
      .withSerial(DBusUInt32.valueOf(3))
      .withReplySerial(DBusUInt32.valueOf(1))
      .withSender(DBusString.valueOf("org.test.Sender"))
      .build();

  handler.handleInboundMessage(mockCtx, reply);
  verify(mockCtx, never()).propagateOutboundMessage(any(), any());
  }

  @Test
  void testUnhandledErrorReplyIsLoggedAndIgnored() {
  InboundError error = InboundError.Builder.create()
      .withSerial(DBusUInt32.valueOf(4))
      .withReplySerial(DBusUInt32.valueOf(1))
      .withSender(DBusString.valueOf("org.test.Sender"))
      .withErrorName(DBusString.valueOf("org.test.Error"))
      .build();

  handler.handleInboundMessage(mockCtx, error);
  verify(mockCtx, never()).propagateOutboundMessage(any(), any());
  }

  @Test
  void testUnhandledSignalOrUnknownMessageIsIgnored() {
  InboundSignal signal = InboundSignal.Builder.create()
      .withSerial(DBusUInt32.valueOf(5))
      .withSender(DBusString.valueOf("org.test.Sender"))
      .withObjectPath(DBusObjectPath.valueOf("/unit/test"))
      .withInterfaceName(DBusString.valueOf("org.example.Interface"))
      .withMember(DBusString.valueOf("TestSignal"))
      .build();

  handler.handleInboundMessage(mockCtx, signal);
  verify(mockCtx, never()).propagateOutboundMessage(any(), any());
  }
}
