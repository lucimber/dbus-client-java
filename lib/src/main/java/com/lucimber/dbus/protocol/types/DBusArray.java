/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.protocol.types;

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
public final class DBusArray<ElementT extends DBusType> implements List<ElementT>, DBusContainerType {

  private final ArrayList<ElementT> delegate;
  private final Signature signature;

  /**
   * Constructs a new instance.
   *
   * @param signature a {@link Signature}; must describe an array
   */
  public DBusArray(final Signature signature) {
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
  public Signature getSignature() {
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
