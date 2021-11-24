package com.lucimber.dbus.type;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

/**
 * A map entry (key-value pair).
 *
 * @param <KeyT>   The key's data type.
 * @param <ValueT> The value's data type.
 */
public final class DictEntry<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Map.Entry<KeyT, ValueT>, DBusContainerType {

  private final KeyT key;
  private final Signature signature;
  private ValueT value;

  /**
   * Constructs a new instance.
   *
   * @param signature a {@link Signature}; must describe an dict-entry
   * @param key       a {@link DBusBasicType}
   */
  public DictEntry(final Signature signature, final KeyT key) {
    this(signature, key, null);
  }

  /**
   * Constructs a new instance.
   *
   * @param signature a {@link Signature}; must describe an dict-entry
   * @param key       a {@link DBusBasicType}
   * @param value     a {@link DBusType}
   */
  public DictEntry(final Signature signature, final KeyT key, final ValueT value) {
    this.signature = Objects.requireNonNull(signature);
    if (!signature.isDictionaryEntry()) {
      throw new IllegalArgumentException("Signature must describe a dictionary entry.");
    }
    this.key = Objects.requireNonNull(key);
    this.value = value;
  }

  /**
   * Constructs a new instance from another.
   *
   * @param other a {@link DictEntry}
   */
  public DictEntry(final DictEntry<KeyT, ValueT> other) {
    signature = other.signature;
    key = other.key;
    value = other.value;
  }

  @Override
  public Type getType() {
    return Type.DICT_ENTRY;
  }

  @Override
  public Signature getSignature() {
    return signature;
  }

  @Override
  public Map.Entry<KeyT, ValueT> getDelegate() {
    return new AbstractMap.SimpleEntry<>(key, value);
  }

  @Override
  public KeyT getKey() {
    return key;
  }

  @Override
  public ValueT getValue() {
    return value;
  }

  @Override
  public ValueT setValue(final ValueT value) {
    final ValueT oldValue = this.value;
    this.value = value;
    return oldValue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DictEntry<?, ?> that = (DictEntry<?, ?>) o;
    return key.equals(that.key) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return key + "=" + value;
  }
}
