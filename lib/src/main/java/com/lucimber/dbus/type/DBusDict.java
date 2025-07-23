/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An object that maps keys to values.
 *
 * @param <KeyT> The key's data type.
 * @param <ValueT> The value's data type.
 */
public final class DBusDict<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Map<KeyT, ValueT>, DBusContainerType {

    private final HashMap<KeyT, ValueT> delegate;
    private final DBusSignature signature;

    /**
     * Constructs a new instance.
     *
     * @param signature a {@link DBusSignature}; must describe a dictionary
     */
    public DBusDict(final DBusSignature signature) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isDictionary()) {
            throw new IllegalArgumentException("Signature must describe a dictionary.");
        }
        final DBusSignature subSignature = signature.subContainer().subContainer();
        final List<DBusSignature> singleTypes = subSignature.getChildren();
        if (singleTypes.get(0).isContainerType()) {
            throw new IllegalArgumentException("Key must be a basic D-Bus type.");
        }
        if (singleTypes.get(1).isDictionaryEntry()) {
            throw new IllegalArgumentException("Dict-entry not allowed as value.");
        }
        this.delegate = new HashMap<>();
    }

    /**
     * Constructs a new instance from another.
     *
     * @param other a {@link DBusDict}
     */
    public DBusDict(final DBusDict<KeyT, ValueT> other) {
        delegate = new HashMap<>(other.delegate);
        signature = other.signature;
    }

    @Override
    public DBusSignature getSignature() {
        return signature;
    }

    @Override
    public Type getType() {
        return Type.ARRAY;
    }

    @Override
    public Map<KeyT, ValueT> getDelegate() {
        return new HashMap<>(delegate);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public ValueT get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public ValueT put(final KeyT key, final ValueT value) {
        return delegate.put(key, value);
    }

    @Override
    public ValueT remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(final Map<? extends KeyT, ? extends ValueT> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<KeyT> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<ValueT> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<KeyT, ValueT>> entrySet() {
        return delegate.entrySet();
    }

    /**
     * Returns a set view of the mappings contained in this map.
     *
     * @return a set
     */
    public Set<DBusDictEntry<KeyT, ValueT>> dictionaryEntrySet() {
        final DBusSignature subSig = signature.subContainer();
        return delegate.entrySet().stream()
                .map(e -> new DBusDictEntry<>(subSig, e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
