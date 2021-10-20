package io.lucimber.dbus.standard;

import java.util.Objects;

/**
 * Base class of runtime exceptions that are used by this implementation.
 */
public class DbusRuntimeException extends RuntimeException {

    private final String errorName;

    /**
     * Creates a new instance.
     *
     * @param errorName the name of the D-Bus error
     */
    public DbusRuntimeException(final String errorName) {
        super();
        this.errorName = Objects.requireNonNull(errorName);
    }

    /**
     * Creates a new instance.
     *
     * @param errorName the name of the D-Bus error
     * @param message   the optional detail message
     */
    public DbusRuntimeException(final String errorName, final String message) {
        super(message);
        this.errorName = Objects.requireNonNull(errorName);
    }

    /**
     * Creates a new instance.
     *
     * @param errorName the name of the D-Bus error
     * @param message   the optional detail message
     * @param cause     the optional cause
     */
    public DbusRuntimeException(final String errorName, final String message, final Throwable cause) {
        super(message, cause);
        this.errorName = Objects.requireNonNull(errorName);
    }

    /**
     * Creates a new instance.
     *
     * @param errorName          the name of the D-Bus error
     * @param message            the optional detail message
     * @param cause              the optional cause
     * @param enableSuppression  whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    protected DbusRuntimeException(final String errorName, final String message, final Throwable cause,
                                   final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorName = Objects.requireNonNull(errorName);
    }

    /**
     * Gets the name of the D-Bus error.
     *
     * @return the name of the error
     */
    public String getErrorName() {
        return errorName;
    }
}
