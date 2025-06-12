/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

enum CustomChannelEvent {
  SASL_NUL_BYTE_SENT,
  SASL_AUTH_STARTED,
  SASL_AUTH_COMPLETE,
  MANDATORY_NAME_ACQUIRED
}
