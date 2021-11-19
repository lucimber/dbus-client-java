package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.UInt32;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractMessage implements Message {

    private UInt32 serial;
    private List<DBusType> payload;
    private Signature signature;

    AbstractMessage(final UInt32 serial) {
        this.serial = Objects.requireNonNull(serial);
    }

    @Override
    public UInt32 getSerial() {
        return serial;
    }

    @Override
    public void setSerial(final UInt32 serial) {
        this.serial = Objects.requireNonNull(serial);
    }

    @Override
    public List<DBusType> getPayload() {
        return payload == null ? Collections.emptyList() : new ArrayList<>(payload);
    }

    @Override
    public void setPayload(final List<? extends DBusType> payload) {
        this.payload = (payload == null) ? null : new ArrayList<>(payload);
    }

    @Override
    public Optional<Signature> getSignature() {
        return Optional.ofNullable(signature);
    }

    @Override
    public void setSignature(final Signature signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return String.format("AbstractMessage{serial=%s, signature=%s}", serial, getSignature());
    }
}
