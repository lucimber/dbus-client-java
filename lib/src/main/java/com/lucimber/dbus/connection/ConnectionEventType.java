/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

/**
 * Types of connection events that can be fired during the connection lifecycle.
 */
public enum ConnectionEventType {
  
  /**
   * Connection state has changed (e.g., from CONNECTING to CONNECTED).
   */
  STATE_CHANGED,
  
  /**
   * A health check (ping/heartbeat) has succeeded.
   */
  HEALTH_CHECK_SUCCESS,
  
  /**
   * A health check (ping/heartbeat) has failed.
   */
  HEALTH_CHECK_FAILURE,
  
  /**
   * An automatic reconnection attempt is being made.
   */
  RECONNECTION_ATTEMPT,
  
  /**
   * Automatic reconnection has succeeded.
   */
  RECONNECTION_SUCCESS,
  
  /**
   * Automatic reconnection has failed (may retry with backoff).
   */
  RECONNECTION_FAILURE,
  
  /**
   * Maximum reconnection attempts reached, giving up.
   */
  RECONNECTION_EXHAUSTED
}