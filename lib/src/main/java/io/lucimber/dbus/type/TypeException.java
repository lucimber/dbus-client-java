package io.lucimber.dbus.type;

public final class TypeException extends Exception {
    public TypeException(final String message) {
        super(message);
    }

    public TypeException(final Throwable cause) {
        super(cause);
    }

    public TypeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
