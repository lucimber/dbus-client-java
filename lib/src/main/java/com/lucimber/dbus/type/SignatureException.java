package com.lucimber.dbus.type;

/**
 * A {@link RuntimeException} that gets thrown by {@link Signature},
 * if a marshalled {@link Signature} cannot be parsed.
 */
public class SignatureException extends RuntimeException {
    public SignatureException(final String message) {
        super(message);
    }

    public SignatureException(final Throwable cause) {
        super(cause);
    }

    public SignatureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
