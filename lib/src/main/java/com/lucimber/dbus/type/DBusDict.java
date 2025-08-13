/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
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

    // Factory methods for common dictionary types

    /**
     * Creates a dictionary with string keys and string values.
     *
     * @return a new empty DBusDict with string keys and values
     */
    public static DBusDict<DBusString, DBusString> ofStringToString() {
        return new DBusDict<>(DBusSignature.valueOf("a{ss}"));
    }

    /**
     * Creates a dictionary with string keys and variant values.
     *
     * @return a new empty DBusDict with string keys and variant values
     */
    public static DBusDict<DBusString, DBusVariant> ofStringToVariant() {
        return new DBusDict<>(DBusSignature.valueOf("a{sv}"));
    }

    /**
     * Creates a dictionary with string keys and int32 values.
     *
     * @return a new empty DBusDict with string keys and int32 values
     */
    public static DBusDict<DBusString, DBusInt32> ofStringToInt32() {
        return new DBusDict<>(DBusSignature.valueOf("a{si}"));
    }

    /**
     * Creates a dictionary with string keys and int64 values.
     *
     * @return a new empty DBusDict with string keys and int64 values
     */
    public static DBusDict<DBusString, DBusInt64> ofStringToInt64() {
        return new DBusDict<>(DBusSignature.valueOf("a{sx}"));
    }

    /**
     * Creates a dictionary with string keys and boolean values.
     *
     * @return a new empty DBusDict with string keys and boolean values
     */
    public static DBusDict<DBusString, DBusBoolean> ofStringToBoolean() {
        return new DBusDict<>(DBusSignature.valueOf("a{sb}"));
    }

    /**
     * Creates a dictionary with int32 keys and string values.
     *
     * @return a new empty DBusDict with int32 keys and string values
     */
    public static DBusDict<DBusInt32, DBusString> ofInt32ToString() {
        return new DBusDict<>(DBusSignature.valueOf("a{is}"));
    }

    /**
     * Creates a dictionary with object path keys and string values.
     *
     * @return a new empty DBusDict with object path keys and string values
     */
    public static DBusDict<DBusObjectPath, DBusString> ofObjectPathToString() {
        return new DBusDict<>(DBusSignature.valueOf("a{os}"));
    }

    /**
     * Creates a dictionary with string keys and string values from a Java Map.
     *
     * @param map the Java map to convert
     * @return a new DBusDict containing the map entries
     */
    public static DBusDict<DBusString, DBusString> fromStringMap(final Map<String, String> map) {
        final DBusDict<DBusString, DBusString> dict = ofStringToString();
        map.forEach((k, v) -> dict.put(DBusString.valueOf(k), DBusString.valueOf(v)));
        return dict;
    }

    /**
     * Creates a dictionary with string keys and variant values from a Java Map.
     *
     * @param map the Java map to convert
     * @return a new DBusDict containing the map entries as variants
     */
    public static DBusDict<DBusString, DBusVariant> fromVariantMap(final Map<String, Object> map) {
        final DBusDict<DBusString, DBusVariant> dict = ofStringToVariant();
        map.forEach(
                (k, v) -> {
                    if (v instanceof String) {
                        dict.put(
                                DBusString.valueOf(k),
                                DBusVariant.valueOf(DBusString.valueOf((String) v)));
                    } else if (v instanceof Integer) {
                        dict.put(
                                DBusString.valueOf(k),
                                DBusVariant.valueOf(DBusInt32.valueOf((Integer) v)));
                    } else if (v instanceof Long) {
                        dict.put(
                                DBusString.valueOf(k),
                                DBusVariant.valueOf(DBusInt64.valueOf((Long) v)));
                    } else if (v instanceof Boolean) {
                        dict.put(
                                DBusString.valueOf(k),
                                DBusVariant.valueOf(DBusBoolean.valueOf((Boolean) v)));
                    } else if (v instanceof Double) {
                        dict.put(
                                DBusString.valueOf(k),
                                DBusVariant.valueOf(DBusDouble.valueOf((Double) v)));
                    }
                    // Add more type conversions as needed
                });
        return dict;
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
