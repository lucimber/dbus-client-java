/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * An ordered collection of elements.
 *
 * @param <ElementT> The element's data type.
 */
public final class DBusArray<ElementT extends DBusType>
        implements List<ElementT>, DBusContainerType {

    private final ArrayList<ElementT> delegate;
    private final DBusSignature signature;

    /**
     * Constructs a new instance.
     *
     * @param signature a {@link DBusSignature}; must describe an array
     */
    public DBusArray(final DBusSignature signature) {
        this.signature = Objects.requireNonNull(signature);
        if (!signature.isArray()) {
            throw new IllegalArgumentException("signature must describe an array");
        }
        this.delegate = new ArrayList<>();
    }

    /**
     * Constructs a new instance from another one.
     *
     * @param other a {@link DBusArray}
     */
    public DBusArray(final DBusArray<ElementT> other) {
        Objects.requireNonNull(other);
        delegate = new ArrayList<>(other.delegate);
        signature = other.signature;
    }

    // Factory methods for common array types

    /**
     * Creates an array of strings.
     *
     * @param strings the strings to add to the array
     * @return a new DBusArray containing DBusString elements
     */
    public static DBusArray<DBusString> ofStrings(final String... strings) {
        final DBusArray<DBusString> array = new DBusArray<>(DBusSignature.valueOf("as"));
        for (final String str : strings) {
            array.add(DBusString.valueOf(str));
        }
        return array;
    }

    /**
     * Creates an array of 32-bit integers.
     *
     * @param integers the integers to add to the array
     * @return a new DBusArray containing DBusInt32 elements
     */
    public static DBusArray<DBusInt32> ofInt32s(final int... integers) {
        final DBusArray<DBusInt32> array = new DBusArray<>(DBusSignature.valueOf("ai"));
        for (final int i : integers) {
            array.add(DBusInt32.valueOf(i));
        }
        return array;
    }

    /**
     * Creates an array of 64-bit integers.
     *
     * @param longs the longs to add to the array
     * @return a new DBusArray containing DBusInt64 elements
     */
    public static DBusArray<DBusInt64> ofInt64s(final long... longs) {
        final DBusArray<DBusInt64> array = new DBusArray<>(DBusSignature.valueOf("ax"));
        for (final long l : longs) {
            array.add(DBusInt64.valueOf(l));
        }
        return array;
    }

    /**
     * Creates an array of booleans.
     *
     * @param booleans the booleans to add to the array
     * @return a new DBusArray containing DBusBoolean elements
     */
    public static DBusArray<DBusBoolean> ofBooleans(final boolean... booleans) {
        final DBusArray<DBusBoolean> array = new DBusArray<>(DBusSignature.valueOf("ab"));
        for (final boolean b : booleans) {
            array.add(DBusBoolean.valueOf(b));
        }
        return array;
    }

    /**
     * Creates an array of doubles.
     *
     * @param doubles the doubles to add to the array
     * @return a new DBusArray containing DBusDouble elements
     */
    public static DBusArray<DBusDouble> ofDoubles(final double... doubles) {
        final DBusArray<DBusDouble> array = new DBusArray<>(DBusSignature.valueOf("ad"));
        for (final double d : doubles) {
            array.add(DBusDouble.valueOf(d));
        }
        return array;
    }

    /**
     * Creates an array of object paths.
     *
     * @param paths the paths to add to the array
     * @return a new DBusArray containing DBusObjectPath elements
     */
    public static DBusArray<DBusObjectPath> ofObjectPaths(final String... paths) {
        final DBusArray<DBusObjectPath> array = new DBusArray<>(DBusSignature.valueOf("ao"));
        for (final String path : paths) {
            array.add(DBusObjectPath.valueOf(path));
        }
        return array;
    }

    /**
     * Creates an empty array with the specified element signature.
     *
     * @param elementSignature the signature of the array elements (e.g., "s" for strings)
     * @param <T> the element type
     * @return a new empty DBusArray
     */
    @SuppressWarnings("unchecked")
    public static <T extends DBusType> DBusArray<T> empty(final String elementSignature) {
        return new DBusArray<>(DBusSignature.valueOf("a" + elementSignature));
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
    public boolean contains(final Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<ElementT> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public void add(final int index, final ElementT element) {
        delegate.add(index, element);
    }

    @Override
    public boolean add(final ElementT value) {
        return delegate.add(value);
    }

    @Override
    public ElementT remove(final int index) {
        return delegate.remove(index);
    }

    @Override
    public boolean remove(final Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends ElementT> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends ElementT> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public ElementT get(final int index) {
        return delegate.get(index);
    }

    @Override
    public ElementT set(final int index, final ElementT element) {
        return delegate.set(index, element);
    }

    @Override
    public int indexOf(final Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<ElementT> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<ElementT> listIterator(final int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<ElementT> subList(final int fromIndex, final int toIndex) {
        return delegate.subList(fromIndex, toIndex);
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
    public List<ElementT> getDelegate() {
        return new ArrayList<>(delegate);
    }

    @Override
    public void forEach(final Consumer<? super ElementT> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(final UnaryOperator<ElementT> operator) {
        delegate.replaceAll(operator);
    }

    @Override
    public void sort(final Comparator<? super ElementT> c) {
        delegate.sort(c);
    }

    @Override
    public Spliterator<ElementT> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean removeIf(final Predicate<? super ElementT> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public Stream<ElementT> stream() {
        return delegate.stream();
    }

    @Override
    public Stream<ElementT> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DBusArray<?> dBusArray = (DBusArray<?>) o;
        return delegate.equals(dBusArray.delegate) && signature.equals(dBusArray.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, signature);
    }
}
