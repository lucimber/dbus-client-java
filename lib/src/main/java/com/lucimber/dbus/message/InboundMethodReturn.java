package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class InboundMethodReturn extends AbstractReply implements InboundReply {

    private DBusString sender;

    public InboundMethodReturn(final UInt32 serial, final UInt32 replySerial,
                               final DBusString sender) {
        super(serial, replySerial);
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    public DBusString getSender() {
        return sender;
    }

    @Override
    public void setSender(final DBusString sender) {
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    public String toString() {
        final String s = "InboundMethodReturn{sender='%s', serial=%s, replySerial=%s, signature=%s}";
        return String.format(s, sender, getSerial(), getReplySerial(), getSignature());
    }
}
