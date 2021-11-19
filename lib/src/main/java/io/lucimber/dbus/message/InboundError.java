package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class InboundError extends AbstractReply implements InboundReply {

    private DBusString sender;
    private DBusString name;

    public InboundError(final UInt32 serial, final UInt32 replySerial,
                        final DBusString sender, final DBusString name) {
        super(serial, replySerial);
        this.sender = Objects.requireNonNull(sender);
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public DBusString getSender() {
        return sender;
    }

    @Override
    public void setSender(final DBusString sender) {
        this.sender = Objects.requireNonNull(sender);
    }

    /**
     * Returns the name of this error.
     *
     * @return a {@link DBusString}
     */
    public DBusString getName() {
        return name;
    }

    /**
     * Sets the name of this error.
     *
     * @param name a {@link DBusString}
     */
    public void setName(final DBusString name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        final String s = "InboundError{serial=%s, replySerial=%s, sender='%s', name='%s', signature=%s}";
        return String.format(s, getSerial(), getReplySerial(), sender, name, getSignature());
    }
}
