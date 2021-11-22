package com.lucimber.dbus.message;

import com.lucimber.dbus.type.UInt32;

public interface InboundReply extends Message, InboundMessage {

    /**
     * Gets the reply-serial of this inbound reply.
     *
     * @return an {@link UInt32}
     */
    UInt32 getReplySerial();

    /**
     * Sets the reply-serial of this inbound reply.
     *
     * @param replySerial an {@link UInt32}
     */
    void setReplySerial(UInt32 replySerial);
}
