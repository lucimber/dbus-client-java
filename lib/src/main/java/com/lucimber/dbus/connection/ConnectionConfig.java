/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration class for D-Bus connections containing timeout and other connection settings.
 * This class uses a builder pattern to allow for flexible configuration.
 */
public final class ConnectionConfig {

  private static final Duration DEFAULT_METHOD_CALL_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_HEALTH_CHECK_INTERVAL = Duration.ofSeconds(30);
  private static final Duration DEFAULT_HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration DEFAULT_RECONNECT_INITIAL_DELAY = Duration.ofSeconds(1);
  private static final Duration DEFAULT_RECONNECT_MAX_DELAY = Duration.ofMinutes(5);
  private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
  private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(5);

  private final Duration methodCallTimeout;
  private final Duration connectTimeout;
  private final Duration readTimeout;
  private final Duration writeTimeout;
  private final boolean healthCheckEnabled;
  private final Duration healthCheckInterval;
  private final Duration healthCheckTimeout;
  private final boolean autoReconnectEnabled;
  private final Duration reconnectInitialDelay;
  private final Duration reconnectMaxDelay;
  private final double reconnectBackoffMultiplier;
  private final int maxReconnectAttempts;
  private final Duration closeTimeout;

  private ConnectionConfig(Builder builder) {
    this.methodCallTimeout = builder.methodCallTimeout;
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;
    this.writeTimeout = builder.writeTimeout;
    this.healthCheckEnabled = builder.healthCheckEnabled;
    this.healthCheckInterval = builder.healthCheckInterval;
    this.healthCheckTimeout = builder.healthCheckTimeout;
    this.autoReconnectEnabled = builder.autoReconnectEnabled;
    this.reconnectInitialDelay = builder.reconnectInitialDelay;
    this.reconnectMaxDelay = builder.reconnectMaxDelay;
    this.reconnectBackoffMultiplier = builder.reconnectBackoffMultiplier;
    this.maxReconnectAttempts = builder.maxReconnectAttempts;
    this.closeTimeout = builder.closeTimeout;
  }

  /**
   * Creates a new builder with default timeout values.
   *
   * @return A new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration with all default timeout values.
   *
   * @return A default configuration instance
   */
  public static ConnectionConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the timeout for method call responses.
   *
   * @return The method call timeout duration
   */
  public Duration getMethodCallTimeout() {
    return methodCallTimeout;
  }

  /**
   * Gets the timeout for establishing connections.
   *
   * @return The connection timeout duration
   */
  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Gets the timeout for read operations.
   *
   * @return The read timeout duration
   */
  public Duration getReadTimeout() {
    return readTimeout;
  }

  /**
   * Gets the timeout for write operations.
   *
   * @return The write timeout duration
   */
  public Duration getWriteTimeout() {
    return writeTimeout;
  }

  /**
   * Gets the method call timeout in milliseconds for compatibility with existing code.
   *
   * @return The method call timeout in milliseconds
   */
  public long getMethodCallTimeoutMs() {
    return methodCallTimeout.toMillis();
  }

  /**
   * Gets the connection timeout in milliseconds for compatibility with existing code.
   *
   * @return The connection timeout in milliseconds
   */
  public long getConnectTimeoutMs() {
    return connectTimeout.toMillis();
  }

  /**
   * Gets the read timeout in milliseconds for compatibility with existing code.
   *
   * @return The read timeout in milliseconds
   */
  public long getReadTimeoutMs() {
    return readTimeout.toMillis();
  }

  /**
   * Gets the write timeout in milliseconds for compatibility with existing code.
   *
   * @return The write timeout in milliseconds
   */
  public long getWriteTimeoutMs() {
    return writeTimeout.toMillis();
  }

  /**
   * Gets whether health check monitoring is enabled.
   *
   * @return true if health checks are enabled
   */
  public boolean isHealthCheckEnabled() {
    return healthCheckEnabled;
  }

  /**
   * Gets the interval between health checks.
   *
   * @return The health check interval duration
   */
  public Duration getHealthCheckInterval() {
    return healthCheckInterval;
  }

  /**
   * Gets the timeout for individual health checks.
   *
   * @return The health check timeout duration
   */
  public Duration getHealthCheckTimeout() {
    return healthCheckTimeout;
  }

  /**
   * Gets whether automatic reconnection is enabled.
   *
   * @return true if auto-reconnect is enabled
   */
  public boolean isAutoReconnectEnabled() {
    return autoReconnectEnabled;
  }

  /**
   * Gets the initial delay before the first reconnection attempt.
   *
   * @return The initial reconnection delay duration
   */
  public Duration getReconnectInitialDelay() {
    return reconnectInitialDelay;
  }

  /**
   * Gets the maximum delay between reconnection attempts.
   *
   * @return The maximum reconnection delay duration
   */
  public Duration getReconnectMaxDelay() {
    return reconnectMaxDelay;
  }

  /**
   * Gets the backoff multiplier for reconnection delays.
   *
   * @return The backoff multiplier
   */
  public double getReconnectBackoffMultiplier() {
    return reconnectBackoffMultiplier;
  }

  /**
   * Gets the maximum number of reconnection attempts.
   *
   * @return The maximum reconnection attempts
   */
  public int getMaxReconnectAttempts() {
    return maxReconnectAttempts;
  }

  /**
   * Gets the timeout for connection close operations.
   *
   * @return The close timeout duration
   */
  public Duration getCloseTimeout() {
    return closeTimeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionConfig that = (ConnectionConfig) o;
    return healthCheckEnabled == that.healthCheckEnabled
            && autoReconnectEnabled == that.autoReconnectEnabled
            && Double.compare(that.reconnectBackoffMultiplier, reconnectBackoffMultiplier) == 0
            && maxReconnectAttempts == that.maxReconnectAttempts
            && Objects.equals(methodCallTimeout, that.methodCallTimeout)
            && Objects.equals(connectTimeout, that.connectTimeout)
            && Objects.equals(readTimeout, that.readTimeout)
            && Objects.equals(writeTimeout, that.writeTimeout)
            && Objects.equals(healthCheckInterval, that.healthCheckInterval)
            && Objects.equals(healthCheckTimeout, that.healthCheckTimeout)
            && Objects.equals(reconnectInitialDelay, that.reconnectInitialDelay)
            && Objects.equals(reconnectMaxDelay, that.reconnectMaxDelay)
            && Objects.equals(closeTimeout, that.closeTimeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodCallTimeout, connectTimeout, readTimeout, writeTimeout,
            healthCheckEnabled, healthCheckInterval, healthCheckTimeout,
            autoReconnectEnabled, reconnectInitialDelay, reconnectMaxDelay,
            reconnectBackoffMultiplier, maxReconnectAttempts, closeTimeout);
  }

  @Override
  public String toString() {
    return "ConnectionConfig{"
            + "methodCallTimeout=" + methodCallTimeout
            + ", connectTimeout=" + connectTimeout
            + ", readTimeout=" + readTimeout
            + ", writeTimeout=" + writeTimeout
            + ", healthCheckEnabled=" + healthCheckEnabled
            + ", healthCheckInterval=" + healthCheckInterval
            + ", healthCheckTimeout=" + healthCheckTimeout
            + ", autoReconnectEnabled=" + autoReconnectEnabled
            + ", reconnectInitialDelay=" + reconnectInitialDelay
            + ", reconnectMaxDelay=" + reconnectMaxDelay
            + ", reconnectBackoffMultiplier=" + reconnectBackoffMultiplier
            + ", maxReconnectAttempts=" + maxReconnectAttempts
            + ", closeTimeout=" + closeTimeout
            + '}';
  }

  /**
   * Builder class for creating ConnectionConfig instances.
   */
  public static final class Builder {
    private Duration methodCallTimeout = DEFAULT_METHOD_CALL_TIMEOUT;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration writeTimeout = DEFAULT_WRITE_TIMEOUT;
    private boolean healthCheckEnabled = true;
    private Duration healthCheckInterval = DEFAULT_HEALTH_CHECK_INTERVAL;
    private Duration healthCheckTimeout = DEFAULT_HEALTH_CHECK_TIMEOUT;
    private boolean autoReconnectEnabled = true;
    private Duration reconnectInitialDelay = DEFAULT_RECONNECT_INITIAL_DELAY;
    private Duration reconnectMaxDelay = DEFAULT_RECONNECT_MAX_DELAY;
    private double reconnectBackoffMultiplier = 2.0;
    private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
    private Duration closeTimeout = DEFAULT_CLOSE_TIMEOUT;

    private Builder() {
    }

    /**
     * Sets the timeout for method call responses.
     *
     * @param timeout The timeout duration (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withMethodCallTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Method call timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Method call timeout must be positive");
      }
      this.methodCallTimeout = timeout;
      return this;
    }

    /**
     * Sets the timeout for establishing connections.
     *
     * @param timeout The timeout duration (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withConnectTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Connect timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Connect timeout must be positive");
      }
      this.connectTimeout = timeout;
      return this;
    }

    /**
     * Sets the timeout for read operations.
     *
     * @param timeout The timeout duration (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withReadTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Read timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Read timeout must be positive");
      }
      this.readTimeout = timeout;
      return this;
    }

    /**
     * Sets the timeout for write operations.
     *
     * @param timeout The timeout duration (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withWriteTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Write timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Write timeout must be positive");
      }
      this.writeTimeout = timeout;
      return this;
    }

    /**
     * Sets whether health check monitoring is enabled.
     *
     * @param enabled true to enable health checks
     * @return This builder instance
     */
    public Builder withHealthCheckEnabled(boolean enabled) {
      this.healthCheckEnabled = enabled;
      return this;
    }

    /**
     * Sets the interval between health checks.
     *
     * @param interval The health check interval (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if interval is null or not positive
     */
    public Builder withHealthCheckInterval(Duration interval) {
      Objects.requireNonNull(interval, "Health check interval cannot be null");
      if (interval.isNegative() || interval.isZero()) {
        throw new IllegalArgumentException("Health check interval must be positive");
      }
      this.healthCheckInterval = interval;
      return this;
    }

    /**
     * Sets the timeout for individual health checks.
     *
     * @param timeout The health check timeout (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withHealthCheckTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Health check timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Health check timeout must be positive");
      }
      this.healthCheckTimeout = timeout;
      return this;
    }

    /**
     * Sets whether automatic reconnection is enabled.
     *
     * @param enabled true to enable auto-reconnect
     * @return This builder instance
     */
    public Builder withAutoReconnectEnabled(boolean enabled) {
      this.autoReconnectEnabled = enabled;
      return this;
    }

    /**
     * Sets the initial delay before the first reconnection attempt.
     *
     * @param delay The initial reconnection delay (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if delay is null or not positive
     */
    public Builder withReconnectInitialDelay(Duration delay) {
      Objects.requireNonNull(delay, "Reconnect initial delay cannot be null");
      if (delay.isNegative() || delay.isZero()) {
        throw new IllegalArgumentException("Reconnect initial delay must be positive");
      }
      this.reconnectInitialDelay = delay;
      return this;
    }

    /**
     * Sets the maximum delay between reconnection attempts.
     *
     * @param delay The maximum reconnection delay (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if delay is null or not positive
     */
    public Builder withReconnectMaxDelay(Duration delay) {
      Objects.requireNonNull(delay, "Reconnect max delay cannot be null");
      if (delay.isNegative() || delay.isZero()) {
        throw new IllegalArgumentException("Reconnect max delay must be positive");
      }
      this.reconnectMaxDelay = delay;
      return this;
    }

    /**
     * Sets the backoff multiplier for reconnection delays.
     *
     * @param multiplier The backoff multiplier (must be >= 1.0)
     * @return This builder instance
     * @throws IllegalArgumentException if multiplier is less than 1.0
     */
    public Builder withReconnectBackoffMultiplier(double multiplier) {
      if (multiplier < 1.0) {
        throw new IllegalArgumentException("Reconnect backoff multiplier must be >= 1.0");
      }
      this.reconnectBackoffMultiplier = multiplier;
      return this;
    }

    /**
     * Sets the maximum number of reconnection attempts.
     *
     * @param attempts The maximum reconnection attempts (must be >= 0, 0 means unlimited)
     * @return This builder instance
     * @throws IllegalArgumentException if attempts is negative
     */
    public Builder withMaxReconnectAttempts(int attempts) {
      if (attempts < 0) {
        throw new IllegalArgumentException("Max reconnect attempts must be >= 0");
      }
      this.maxReconnectAttempts = attempts;
      return this;
    }

    /**
     * Sets the timeout for connection close operations.
     *
     * @param timeout The close timeout duration (must be positive)
     * @return This builder instance
     * @throws IllegalArgumentException if timeout is null or not positive
     */
    public Builder withCloseTimeout(Duration timeout) {
      Objects.requireNonNull(timeout, "Close timeout cannot be null");
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("Close timeout must be positive");
      }
      this.closeTimeout = timeout;
      return this;
    }

    /**
     * Builds a new ConnectionConfig instance with the configured values.
     *
     * @return A new ConnectionConfig instance
     */
    public ConnectionConfig build() {
      return new ConnectionConfig(this);
    }
  }
}