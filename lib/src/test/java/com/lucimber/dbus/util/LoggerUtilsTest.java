/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import static org.junit.jupiter.api.Assertions.*;

class LoggerUtilsTest {

  @Test
  void testSaslMarker() {
    Marker marker = LoggerUtils.SASL;
    
    assertNotNull(marker);
    assertEquals("SASL", marker.getName());
  }

  @Test
  void testDbusMarker() {
    Marker marker = LoggerUtils.DBUS;
    
    assertNotNull(marker);
    assertEquals("DBUS", marker.getName());
  }

  @Test
  void testTransportMarker() {
    Marker marker = LoggerUtils.TRANSPORT;
    
    assertNotNull(marker);
    assertEquals("TRANSPORT", marker.getName());
  }

  @Test
  void testMarshallingMarker() {
    Marker marker = LoggerUtils.MARSHALLING;
    
    assertNotNull(marker);
    assertEquals("MARSHALLING", marker.getName());
  }

  @Test
  void testHandlerLifecycleMarker() {
    Marker marker = LoggerUtils.HANDLER_LIFECYCLE;
    
    assertNotNull(marker);
    assertEquals("HANDLER_LIFECYCLE", marker.getName());
  }

  @Test
  void testConnectionMarker() {
    Marker marker = LoggerUtils.CONNECTION;
    
    assertNotNull(marker);
    assertEquals("CONNECTION", marker.getName());
  }

  @Test
  void testHealthMarker() {
    Marker marker = LoggerUtils.HEALTH;
    
    assertNotNull(marker);
    assertEquals("HEALTH", marker.getName());
  }

  @Test
  void testAllMarkersAreUnique() {
    Marker[] markers = {
      LoggerUtils.SASL,
      LoggerUtils.DBUS,
      LoggerUtils.TRANSPORT,
      LoggerUtils.MARSHALLING,
      LoggerUtils.HANDLER_LIFECYCLE,
      LoggerUtils.CONNECTION,
      LoggerUtils.HEALTH
    };

    // Test that all markers have different names
    for (int i = 0; i < markers.length; i++) {
      for (int j = i + 1; j < markers.length; j++) {
        assertNotEquals(markers[i].getName(), markers[j].getName(),
          "Markers should have unique names: " + markers[i].getName() + " vs " + markers[j].getName());
      }
    }
  }

  @Test
  void testMarkersAreSingleton() {
    // Test that calling the static fields multiple times returns the same instance
    assertSame(LoggerUtils.SASL, LoggerUtils.SASL);
    assertSame(LoggerUtils.DBUS, LoggerUtils.DBUS);
    assertSame(LoggerUtils.TRANSPORT, LoggerUtils.TRANSPORT);
    assertSame(LoggerUtils.MARSHALLING, LoggerUtils.MARSHALLING);
    assertSame(LoggerUtils.HANDLER_LIFECYCLE, LoggerUtils.HANDLER_LIFECYCLE);
    assertSame(LoggerUtils.CONNECTION, LoggerUtils.CONNECTION);
    assertSame(LoggerUtils.HEALTH, LoggerUtils.HEALTH);
  }

  @Test
  void testMarkerEquality() {
    // Test marker equality behavior
    assertEquals(LoggerUtils.SASL, LoggerUtils.SASL);
    assertEquals(LoggerUtils.DBUS, LoggerUtils.DBUS);
    assertEquals(LoggerUtils.TRANSPORT, LoggerUtils.TRANSPORT);
    assertEquals(LoggerUtils.MARSHALLING, LoggerUtils.MARSHALLING);
    assertEquals(LoggerUtils.HANDLER_LIFECYCLE, LoggerUtils.HANDLER_LIFECYCLE);
    assertEquals(LoggerUtils.CONNECTION, LoggerUtils.CONNECTION);
    assertEquals(LoggerUtils.HEALTH, LoggerUtils.HEALTH);
  }

  @Test
  void testMarkerHashCode() {
    // Test that hash codes are consistent
    assertEquals(LoggerUtils.SASL.hashCode(), LoggerUtils.SASL.hashCode());
    assertEquals(LoggerUtils.DBUS.hashCode(), LoggerUtils.DBUS.hashCode());
    assertEquals(LoggerUtils.TRANSPORT.hashCode(), LoggerUtils.TRANSPORT.hashCode());
    assertEquals(LoggerUtils.MARSHALLING.hashCode(), LoggerUtils.MARSHALLING.hashCode());
    assertEquals(LoggerUtils.HANDLER_LIFECYCLE.hashCode(), LoggerUtils.HANDLER_LIFECYCLE.hashCode());
    assertEquals(LoggerUtils.CONNECTION.hashCode(), LoggerUtils.CONNECTION.hashCode());
    assertEquals(LoggerUtils.HEALTH.hashCode(), LoggerUtils.HEALTH.hashCode());
  }

  @Test
  void testMarkerToString() {
    // Test that toString returns meaningful values
    assertTrue(LoggerUtils.SASL.toString().contains("SASL"));
    assertTrue(LoggerUtils.DBUS.toString().contains("DBUS"));
    assertTrue(LoggerUtils.TRANSPORT.toString().contains("TRANSPORT"));
    assertTrue(LoggerUtils.MARSHALLING.toString().contains("MARSHALLING"));
    assertTrue(LoggerUtils.HANDLER_LIFECYCLE.toString().contains("HANDLER_LIFECYCLE"));
    assertTrue(LoggerUtils.CONNECTION.toString().contains("CONNECTION"));
    assertTrue(LoggerUtils.HEALTH.toString().contains("HEALTH"));
  }
}