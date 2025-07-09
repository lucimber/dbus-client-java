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

  private final Duration methodCallTimeout;
  private final Duration connectTimeout;
  private final Duration readTimeout;
  private final Duration writeTimeout;

  private ConnectionConfig(Builder builder) {
    this.methodCallTimeout = builder.methodCallTimeout;
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;
    this.writeTimeout = builder.writeTimeout;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConnectionConfig that = (ConnectionConfig) o;
    return Objects.equals(methodCallTimeout, that.methodCallTimeout) &&
            Objects.equals(connectTimeout, that.connectTimeout) &&
            Objects.equals(readTimeout, that.readTimeout) &&
            Objects.equals(writeTimeout, that.writeTimeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodCallTimeout, connectTimeout, readTimeout, writeTimeout);
  }

  @Override
  public String toString() {
    return "ConnectionConfig{" +
            "methodCallTimeout=" + methodCallTimeout +
            ", connectTimeout=" + connectTimeout +
            ", readTimeout=" + readTimeout +
            ", writeTimeout=" + writeTimeout +
            '}';
  }

  /**
   * Builder class for creating ConnectionConfig instances.
   */
  public static final class Builder {
    private Duration methodCallTimeout = DEFAULT_METHOD_CALL_TIMEOUT;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration writeTimeout = DEFAULT_WRITE_TIMEOUT;

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
     * Builds a new ConnectionConfig instance with the configured values.
     *
     * @return A new ConnectionConfig instance
     */
    public ConnectionConfig build() {
      return new ConnectionConfig(this);
    }
  }
}