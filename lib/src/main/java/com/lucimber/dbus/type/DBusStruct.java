/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A struct that can hold various objects. The order of the objects is given. */
public final class DBusStruct implements DBusContainerType {

    private final List<DBusType> delegate;
    private final DBusSignature signature;

    /**
     * Constructs a new struct with the signature of the values.
     *
     * @param signature a {@link DBusSignature}
     * @param values one or many {@link Object}s
     */
    public DBusStruct(final DBusSignature signature, final DBusType... values) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isStruct()) {
            throw new IllegalArgumentException("signature must describe a struct");
        }
        this.delegate = Arrays.asList(values);
    }

    /**
     * Constructs a new struct with mandatory parameter.
     *
     * @param signature a {@link DBusSignature}
     * @param types a {@link List} of {@link DBusType}s
     */
    public DBusStruct(final DBusSignature signature, final List<DBusType> types) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isStruct()) {
            throw new IllegalArgumentException("signature must describe a struct");
        }
        this.delegate = new ArrayList<>(types);
    }

    /**
     * Gets the signature of the values of this struct.
     *
     * @return a {@link DBusSignature}
     */
    public DBusSignature getSignature() {
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
