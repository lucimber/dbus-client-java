package com.lucimber.dbus.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A struct that can hold various objects.
 * The order of the objects is given.
 */
public final class Struct implements DBusContainerType {

    private final List<DBusType> delegate;
    private final Signature signature;

    /**
     * Constructs a new struct with the signature of the values.
     *
     * @param signature a {@link Signature}
     * @param values    one or many {@link Object}s
     */
    public Struct(final Signature signature, final DBusType... values) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isStruct()) {
            throw new IllegalArgumentException("signature must describe a struct");
        }
        this.delegate = Arrays.asList(values);
    }

    /**
     * Constructs a new struct with mandatory parameter.
     *
     * @param signature a {@link Signature}
     * @param types     a {@link List} of {@link DBusType}s
     */
    public Struct(final Signature signature, final List<DBusType> types) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isStruct()) {
            throw new IllegalArgumentException("signature must describe a struct");
        }
        this.delegate = new ArrayList<>(types);
    }

    /**
     * Gets the signature of the values of this struct.
     *
     * @return a {@link Signature}
     */
    public Signature getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return signature.toString();
    }

    @Override
    public Type getType() {
        return Type.STRUCT;
    }

    @Override
    public List<DBusType> getDelegate() {
        return new ArrayList<>(delegate);
    }
}
