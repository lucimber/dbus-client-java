package io.lucimber.dbus.connection.impl;

import io.lucimber.dbus.connection.Handler;
import io.lucimber.dbus.connection.HandlerContext;
import io.lucimber.dbus.message.InboundMessage;
import io.lucimber.dbus.message.InboundMethodCall;
import io.lucimber.dbus.message.OutboundMethodReturn;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.UInt32;
import io.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A pipeline handler which implements the {@code org.freedesktop.DBus.Peer} interface.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces">
 * D-Bus Specification (Standard Interfaces)</a>
 */
public final class DbusPeerHandler implements Handler {

    private static final String DEBUG_MSG_HANDLING = "Handling %s.";
    private static final String DEBUG_MSG_PASSING = "Passing on %s to next inbound handler.";
    private static final DBusString INTERFACE = DBusString.valueOf("org.freedesktop.DBus.Peer");
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UUID machineId;

    /**
     * Constructs a new instance of this pipeline handler.
     *
     * @param machineId A hex-encoded UUID representing the identity of the machine the process is running on.
     */
    public DbusPeerHandler(final UUID machineId) {
        this.machineId = Objects.requireNonNull(machineId);
    }

    private static void respondToPing(final HandlerContext ctx, final InboundMethodCall methodCall) {
        final DBusString destination = methodCall.getSender();
        final UInt32 serial = ctx.getPipeline().getConnection().getNextSerial();
        final UInt32 replySerial = methodCall.getSerial();
        final OutboundMethodReturn methodReturn = new OutboundMethodReturn(destination, serial, replySerial);
        LoggerUtils.trace(LOGGER, methodReturn::toString);
        ctx.passOutboundMessage(methodReturn);
    }

    private static void passInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        LoggerUtils.debug(LOGGER, () -> String.format(DEBUG_MSG_PASSING, msg));
        ctx.passInboundMessage(msg);
    }

    private void handleInboundMethodCall(final HandlerContext ctx, final InboundMethodCall methodCall) {
        if (methodCall.getInterfaceName().orElse(DBusString.valueOf("")).equals(INTERFACE)) {
            if (methodCall.getName().equals(DBusString.valueOf("Ping"))) {
                LoggerUtils.debug(LOGGER, () -> String.format(DEBUG_MSG_HANDLING, methodCall));
                respondToPing(ctx, methodCall);
            } else if (methodCall.getName().equals(DBusString.valueOf("GetMachineId"))) {
                respondWithMachineId(ctx, methodCall);
            } else {
                passInboundMessage(ctx, methodCall);
            }
        } else {
            passInboundMessage(ctx, methodCall);
        }
    }

    private void respondWithMachineId(final HandlerContext ctx, final InboundMethodCall methodCall) {
        final DBusString destination = methodCall.getSender();
        final UInt32 serial = ctx.getPipeline().getConnection().getNextSerial();
        final UInt32 replySerial = methodCall.getSerial();
        final OutboundMethodReturn methodReturn = new OutboundMethodReturn(destination, serial, replySerial);
        final List<DBusType> payload = new ArrayList<>();
        payload.add(DBusString.valueOf(machineId.toString()));
        methodReturn.setPayload(payload);
        LoggerUtils.trace(LOGGER, methodReturn::toString);
        ctx.passOutboundMessage(methodReturn);
    }

    @Override
    public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(msg, "msg must not be null");
        if (msg instanceof InboundMethodCall) {
            handleInboundMethodCall(ctx, (InboundMethodCall) msg);
        } else {
            passInboundMessage(ctx, msg);
        }
    }
}
