package io.lucimber.dbus.message;

import io.lucimber.dbus.type.UInt32;

public interface OutboundReply extends Message, OutboundMessage {

    /**
     * Gets the serial number of the message this message is a reply to.
     *
     * @return an {@link UInt32}
     */
    UInt32 getReplySerial();

    /**
     * Sets the serial number of the message this message is a reply to.
     *
     * @param replySerial an {@link UInt32}
     */
    void setReplySerial(UInt32 replySerial);
}
