package io.lucimber.dbus.connection;

import io.lucimber.dbus.message.InboundMessage;

public interface InboundMessageInvoker {
    /**
     * Passes an inbound message further up the pipeline,
     * eventually reaching the last handler.
     *
     * @param msg the {@link InboundMessage}
     */
    void passInboundMessage(InboundMessage msg);

    /**
     * Passes the cause of an inbound failure further up the pipeline,
     * eventually reaching the last handler.
     *
     * @param cause the cause of the failure
     */
    void passInboundFailure(Throwable cause);
}
