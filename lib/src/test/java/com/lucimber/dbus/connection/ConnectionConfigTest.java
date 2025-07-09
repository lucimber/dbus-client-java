package com.lucimber.dbus.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionConfigTest {

  @Test
  void testDefaultConfig() {
    ConnectionConfig config = ConnectionConfig.defaultConfig();
    
    assertEquals(Duration.ofSeconds(30), config.getMethodCallTimeout());
    assertEquals(Duration.ofSeconds(10), config.getConnectTimeout());
    assertEquals(Duration.ofSeconds(60), config.getReadTimeout());
    assertEquals(Duration.ofSeconds(10), config.getWriteTimeout());
  }

  @Test
  void testBuilderWithCustomTimeouts() {
    ConnectionConfig config = ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(15))
            .withConnectTimeout(Duration.ofSeconds(5))
            .withReadTimeout(Duration.ofSeconds(30))
            .withWriteTimeout(Duration.ofSeconds(8))
            .build();

    assertEquals(Duration.ofSeconds(15), config.getMethodCallTimeout());
    assertEquals(Duration.ofSeconds(5), config.getConnectTimeout());
    assertEquals(Duration.ofSeconds(30), config.getReadTimeout());
    assertEquals(Duration.ofSeconds(8), config.getWriteTimeout());
  }

  @Test
  void testTimeoutInMilliseconds() {
    ConnectionConfig config = ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofMillis(500))
            .build();

    assertEquals(500, config.getMethodCallTimeoutMs());
  }

  @Test
  void testBuilderValidation() {
    assertThrows(IllegalArgumentException.class, () ->
            ConnectionConfig.builder().withMethodCallTimeout(Duration.ZERO));
    
    assertThrows(IllegalArgumentException.class, () ->
            ConnectionConfig.builder().withMethodCallTimeout(Duration.ofSeconds(-1)));
    
    assertThrows(NullPointerException.class, () ->
            ConnectionConfig.builder().withMethodCallTimeout(null));
  }

  @Test
  void testBuilderPartialConfiguration() {
    ConnectionConfig config = ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(20))
            .build();

    assertEquals(Duration.ofSeconds(20), config.getMethodCallTimeout());
    // Other values should be defaults
    assertEquals(Duration.ofSeconds(10), config.getConnectTimeout());
    assertEquals(Duration.ofSeconds(60), config.getReadTimeout());
    assertEquals(Duration.ofSeconds(10), config.getWriteTimeout());
  }

  @Test
  void testEqualsAndHashCode() {
    ConnectionConfig config1 = ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(15))
            .withConnectTimeout(Duration.ofSeconds(5))
            .build();

    ConnectionConfig config2 = ConnectionConfig.builder()
            .withMethodCallTimeout(Duration.ofSeconds(15))
            .withConnectTimeout(Duration.ofSeconds(5))
            .build();

    assertEquals(config1, config2);
    assertEquals(config1.hashCode(), config2.hashCode());
  }

  @Test
  void testToString() {
    ConnectionConfig config = ConnectionConfig.defaultConfig();
    String str = config.toString();
    
    assertTrue(str.contains("ConnectionConfig"));
    assertTrue(str.contains("methodCallTimeout"));
    assertTrue(str.contains("connectTimeout"));
  }
}