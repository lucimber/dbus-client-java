package io.lucimber.dbus.impl.netty.connection;

enum CustomChannelEvent {
    SASL_NUL_BYTE_SENT,
    SASL_AUTH_STARTED,
    SASL_AUTH_COMPLETE,
    MANDATORY_NAME_ACQUIRED
}
