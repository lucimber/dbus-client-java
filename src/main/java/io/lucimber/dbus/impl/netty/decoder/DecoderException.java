package io.lucimber.dbus.impl.netty.decoder;

/**
 * A {@link RuntimeException} that gets thrown by a {@link Decoder},
 * if the decoding of a value isn't possible.
 */
public class DecoderException extends RuntimeException {
    /**
     * Creates a new instance with a message.
     *
     * @param message the associated message
     */
    public DecoderException(final String message) {
        super(message);
    }

    /**
     * Creates a new instance with a cause.
     *
     * @param cause the associated cause
     */
    public DecoderException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new instance with a message and a cause.
     *
     * @param message the associated message
     * @param cause   the associated cause
     */
    public DecoderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
