package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.type.DBusType;

import java.util.Objects;

public final class DecoderResultImpl<ValueT extends DBusType> implements DecoderResult<ValueT> {

    private final ValueT value;
    private int consumedBytes;

    public DecoderResultImpl(final int consumedBytes, final ValueT value) {
        this.consumedBytes = consumedBytes;
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public int getConsumedBytes() {
        return consumedBytes;
    }

    @Override
    public void setConsumedBytes(final int consumedBytes) {
        this.consumedBytes = consumedBytes;
    }

    @Override
    public ValueT getValue() {
        return value;
    }
}
