package io.lucimber.dbus.standard;

/**
 * Specific runtime exception which gets thrown if a caller tries to perform an action that is not supported.
 */
public final class NotSupportedException extends DbusRuntimeException {

    private static final String ERROR_NAME = "io.lucimber.DBus1.Error.NotSupported";

    /**
     * Creates a new instance.
     */
    public NotSupportedException() {
        super(ERROR_NAME);
    }

    /**
     * Creates a new instance.
     *
     * @param message the optional detail message
     */
    public NotSupportedException(final String message) {
        super(ERROR_NAME, message);
    }

    /**
     * Creates a new instance.
     *
     * @param message the optional detail message
     * @param cause   the optional cause
     */
    public NotSupportedException(final String message, final Throwable cause) {
        super(ERROR_NAME, message, cause);
    }
}
