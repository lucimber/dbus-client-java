/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.type.DBusString;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class ReconnectionHandlerManagerTest {

  private Promise<Void> connectPromise;
  private ReconnectionHandlerManager manager;
  private EmbeddedChannel channel;

  @BeforeEach
  void setUp() {
    MDC.clear();
    connectPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    manager = new ReconnectionHandlerManager(connectPromise);
    channel = new EmbeddedChannel(manager);
  }

  @Test
  void testConstructor() {
    Promise<Void> promise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ReconnectionHandlerManager handler = new ReconnectionHandlerManager(promise);
    
    assertNotNull(handler);
  }

  @Test
  void testReconnectionStartingEvent() {
    // Set initial values for channel attributes
    DBusString initialBusName = DBusString.valueOf(":1.123");
    AtomicLong initialCounter = new AtomicLong(42);
    
    channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(initialBusName);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(initialCounter);
    
    // Verify initial state
    assertEquals(initialBusName, channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    assertEquals(42L, channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get().get());
    
    // Fire reconnection starting event
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    
    // Verify attributes were reset
    assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    AtomicLong newCounter = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    assertNotNull(newCounter);
    assertEquals(1L, newCounter.get());
    assertNotSame(initialCounter, newCounter); // Should be a new instance
  }

  @Test
  void testReconnectionHandlersReaddRequiredEvent() {
    // Create a spy of a real ConnectionCompletionHandler
    Promise<Void> oldPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ConnectionCompletionHandler completionHandler = spy(new ConnectionCompletionHandler(oldPromise));
    
    // Add the completion handler to pipeline
    channel.pipeline().addAfter(channel.pipeline().names().get(0), 
        DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, completionHandler);
    
    // Fire event
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    
    // Verify that the completion handler was reset with new promise
    verify(completionHandler).reset(connectPromise);
  }

  @Test
  void testReconnectionHandlersReaddWithoutCompletionHandler() {
    // Fire event when no completion handler exists
    assertDoesNotThrow(() -> {
      channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    });
  }

  @Test
  void testOtherEventsPropagated() {
    // Create a spy to track event propagation
    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    when(mockCtx.channel()).thenReturn(channel);
    
    // Create a ReconnectionHandlerManager that we can spy on
    ReconnectionHandlerManager spyManager = spy(new ReconnectionHandlerManager(connectPromise));
    
    try {
      // Test that other events are propagated
      spyManager.userEventTriggered(mockCtx, DBusChannelEvent.SASL_AUTH_COMPLETE);
      
      // Verify that super.userEventTriggered was called
      verify(mockCtx).fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    } catch (Exception e) {
      fail("Should not throw exception for other events");
    }
  }

  @Test
  void testChannelAttributeReset() {
    // Set initial state
    DBusString busName = DBusString.valueOf(":1.999");
    AtomicLong counter = new AtomicLong(100);
    
    channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(busName);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(counter);
    
    // Fire reconnection starting event
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    
    // Verify serial counter reset to 1
    AtomicLong resetCounter = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    assertEquals(1L, resetCounter.get());
    
    // Verify bus name is null
    assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
  }

  @Test
  void testMultipleReconnectionEvents() {
    // Test multiple reconnection starting events
    for (int i = 0; i < 3; i++) {
      channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(new AtomicLong(i * 10));
      channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(DBusString.valueOf(":1." + i));
      
      channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
      
      // Each time should reset to 1
      assertEquals(1L, channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get().get());
      assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    }
  }

  @Test
  void testConnectionCompletionHandlerUpdate() {
    Promise<Void> promise1 = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    Promise<Void> promise2 = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ConnectionCompletionHandler handler1 = spy(new ConnectionCompletionHandler(promise1));
    ConnectionCompletionHandler handler2 = spy(new ConnectionCompletionHandler(promise2));
    
    // Add first handler
    channel.pipeline().addFirst(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, handler1);
    
    // Fire event
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    verify(handler1).reset(connectPromise);
    
    // Replace handler
    channel.pipeline().replace(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, 
        DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, handler2);
    
    // Fire event again
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    verify(handler2).reset(connectPromise);
  }

  @Test
  void testHandlerWithNullConnectPromise() {
    Promise<Void> nullPromise = null;
    ReconnectionHandlerManager handlerWithNullPromise = new ReconnectionHandlerManager(nullPromise);
    EmbeddedChannel testChannel = new EmbeddedChannel(handlerWithNullPromise);
    
    Promise<Void> initialPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ConnectionCompletionHandler completionHandler = spy(new ConnectionCompletionHandler(initialPromise));
    testChannel.pipeline().addFirst(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, completionHandler);
    
    // Should not throw exception even with null promise
    assertDoesNotThrow(() -> {
      testChannel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    });
    
    // Should still call reset even with null promise
    verify(completionHandler).reset(nullPromise);
  }

  @Test
  void testEventPropagation() {
    // Simply test that events don't cause exceptions when propagated
    assertDoesNotThrow(() -> {
      channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
      channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
      channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    });
  }

  @Test
  void testAttributeInitialization() {
    // Create fresh channel
    EmbeddedChannel freshChannel = new EmbeddedChannel();
    
    // Initially attributes should be null
    assertNull(freshChannel.attr(DBusChannelAttribute.SERIAL_COUNTER).get());
    assertNull(freshChannel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    
    // Add manager and fire event
    freshChannel.pipeline().addFirst(new ReconnectionHandlerManager(connectPromise));
    freshChannel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    
    // Should initialize serial counter to 1
    AtomicLong counter = freshChannel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    assertNotNull(counter);
    assertEquals(1L, counter.get());
    
    // Bus name should remain null
    assertNull(freshChannel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
  }

  @Test
  void testSerialCounterIndependence() {
    // Fire reconnection starting event
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    AtomicLong counter1 = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    
    // Fire again
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    AtomicLong counter2 = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    
    // Should be different instances but same value
    assertNotSame(counter1, counter2);
    assertEquals(counter1.get(), counter2.get());
  }

  @Test
  void testUnknownEventHandling() {
    // Custom event object
    Object customEvent = new Object();
    
    // Should not throw exception for unknown events
    assertDoesNotThrow(() -> {
      channel.pipeline().fireUserEventTriggered(customEvent);
    });
  }

  @Test
  void testMultipleManagersInPipeline() {
    // Add second manager
    Promise<Void> promise2 = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ReconnectionHandlerManager manager2 = new ReconnectionHandlerManager(promise2);
    channel.pipeline().addAfter(channel.pipeline().names().get(0), "manager2", manager2);
    
    Promise<Void> initialPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
    ConnectionCompletionHandler completionHandler = spy(new ConnectionCompletionHandler(initialPromise));
    channel.pipeline().addLast(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER, completionHandler);
    
    // Fire event - both managers should handle it
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED);
    
    // Completion handler should be reset twice (once per manager)
    verify(completionHandler, times(2)).reset(any(Promise.class));
  }
}