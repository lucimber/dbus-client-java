/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StandardInterfaceHandlerTest {

    @Mock private Context context;

    @Mock private InboundMethodCall methodCall;

    @Mock private InboundSignal signal;

    @Captor private ArgumentCaptor<OutboundMethodReturn> returnCaptor;

    @Captor private ArgumentCaptor<OutboundError> errorCaptor;

    private StandardInterfaceHandler handler;
    private TestService testService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testService = new TestService();
        handler = new StandardInterfaceHandler("/com/example/TestService", testService);
    }

    @Test
    void testHandleNonMethodCall() {
        // When: a non-method call message is received
        handler.handleInboundMessage(context, signal);

        // Then: it should be propagated
        verify(context).propagateInboundMessage(signal);
        verifyNoMoreInteractions(context);
    }

    @Test
    void testHandleDifferentObjectPath() {
        // Given: a method call for a different object path
        when(methodCall.getObjectPath()).thenReturn(DBusObjectPath.valueOf("/different/path"));

        // When: handling the message
        handler.handleInboundMessage(context, methodCall);

        // Then: it should be propagated
        verify(context).propagateInboundMessage(methodCall);
        verifyNoMoreInteractions(context);
    }

    @Test
    void testIntrospectMethod() {
        // Given: an Introspect method call
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Introspectable")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Introspect"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(42));

        // When: handling the introspect call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return XML introspection data
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        assertEquals(42, reply.getReplySerial().getDelegate());
        assertNotNull(reply.getPayload());
        assertEquals(1, reply.getPayload().size());

        DBusString xml = (DBusString) reply.getPayload().get(0);
        assertTrue(xml.toString().contains("<!DOCTYPE node"));
        assertTrue(xml.toString().contains("org.freedesktop.DBus.Introspectable"));
        assertTrue(xml.toString().contains("com.example.TestService"));
        assertTrue(xml.toString().contains("version"));
        assertTrue(xml.toString().contains("enabled"));
    }

    @Test
    void testIntrospectUnknownMethod() {
        // Given: an unknown method on Introspectable interface
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Introspectable")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("UnknownMethod"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(43));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return an error
        verify(context)
                .propagateOutboundMessage(errorCaptor.capture(), any(CompletableFuture.class));
        OutboundError error = errorCaptor.getValue();

        assertEquals(43, error.getReplySerial().getDelegate());
        assertEquals("org.freedesktop.DBus.Error.UnknownMethod", error.getErrorName().toString());
    }

    @Test
    void testPropertiesGet() {
        // Given: a Properties.Get method call
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Get"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(44));
        when(methodCall.getPayload())
                .thenReturn(
                        Arrays.asList(
                                DBusString.valueOf("com.example.TestService"),
                                DBusString.valueOf("version")));

        // When: handling the get property call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return the property value
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        assertEquals(44, reply.getReplySerial().getDelegate());
        assertNotNull(reply.getPayload());
        assertEquals(1, reply.getPayload().size());

        DBusVariant variant = (DBusVariant) reply.getPayload().get(0);
        DBusString value = (DBusString) variant.getDelegate();
        assertEquals("1.0.0", value.toString());
    }

    @Test
    void testPropertiesGetUnknownProperty() {
        // Given: a Get call for unknown property
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Get"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(45));
        when(methodCall.getPayload())
                .thenReturn(
                        Arrays.asList(
                                DBusString.valueOf("com.example.TestService"),
                                DBusString.valueOf("unknownProperty")));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return an error
        verify(context)
                .propagateOutboundMessage(errorCaptor.capture(), any(CompletableFuture.class));
        OutboundError error = errorCaptor.getValue();

        assertEquals(45, error.getReplySerial().getDelegate());
        assertEquals("org.freedesktop.DBus.Error.UnknownProperty", error.getErrorName().toString());
    }

    @Test
    void testPropertiesGetInvalidArgs() {
        // Given: a Get call with missing arguments
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Get"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(46));
        when(methodCall.getPayload())
                .thenReturn(Arrays.asList(DBusString.valueOf("com.example.TestService")));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return an error
        verify(context)
                .propagateOutboundMessage(errorCaptor.capture(), any(CompletableFuture.class));
        OutboundError error = errorCaptor.getValue();

        assertEquals(46, error.getReplySerial().getDelegate());
        assertEquals("org.freedesktop.DBus.Error.InvalidArgs", error.getErrorName().toString());
    }

    @Test
    void testPropertiesGetAll() {
        // Given: a Properties.GetAll method call
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("GetAll"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(47));
        when(methodCall.getPayload())
                .thenReturn(Arrays.asList(DBusString.valueOf("com.example.TestService")));

        // When: handling the get all properties call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return all properties
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        assertEquals(47, reply.getReplySerial().getDelegate());
        assertNotNull(reply.getPayload());
        assertEquals(1, reply.getPayload().size());

        DBusDict<DBusString, DBusVariant> dict =
                (DBusDict<DBusString, DBusVariant>) reply.getPayload().get(0);
        assertEquals(2, dict.size());

        DBusVariant versionVariant = dict.get(DBusString.valueOf("version"));
        assertNotNull(versionVariant);
        assertEquals("1.0.0", ((DBusString) versionVariant.getDelegate()).toString());

        DBusVariant enabledVariant = dict.get(DBusString.valueOf("enabled"));
        assertNotNull(enabledVariant);
        assertTrue(((DBusBoolean) enabledVariant.getDelegate()).getDelegate());
    }

    @Test
    void testPropertiesGetAllUnknownInterface() {
        // Given: a GetAll call for unknown interface
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("GetAll"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(48));
        when(methodCall.getPayload())
                .thenReturn(Arrays.asList(DBusString.valueOf("com.unknown.Interface")));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return empty dict
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        DBusDict<DBusString, DBusVariant> dict =
                (DBusDict<DBusString, DBusVariant>) reply.getPayload().get(0);
        assertTrue(dict.isEmpty());
    }

    @Test
    void testPeerPing() {
        // Given: a Peer.Ping method call
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(java.util.Optional.of(DBusString.valueOf("org.freedesktop.DBus.Peer")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Ping"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(49));

        // When: handling the ping call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return empty reply
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        assertEquals(49, reply.getReplySerial().getDelegate());
        assertTrue(reply.getPayload() == null || reply.getPayload().isEmpty());
    }

    @Test
    void testPeerGetMachineId() {
        // Given: a Peer.GetMachineId method call
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(java.util.Optional.of(DBusString.valueOf("org.freedesktop.DBus.Peer")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("GetMachineId"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(50));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should return machine ID
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        assertEquals(50, reply.getReplySerial().getDelegate());
        assertNotNull(reply.getPayload());
        assertEquals(1, reply.getPayload().size());

        DBusString machineId = (DBusString) reply.getPayload().get(0);
        assertNotNull(machineId.toString());
        assertEquals(32, machineId.toString().length());
    }

    @Test
    void testUnknownInterface() {
        // Given: a call to unknown interface
        when(methodCall.getObjectPath())
                .thenReturn(DBusObjectPath.valueOf("/com/example/TestService"));
        when(methodCall.getInterfaceName())
                .thenReturn(java.util.Optional.of(DBusString.valueOf("com.unknown.Interface")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("SomeMethod"));

        // When: handling the call
        handler.handleInboundMessage(context, methodCall);

        // Then: should propagate the message
        verify(context).propagateInboundMessage(methodCall);
        verifyNoMoreInteractions(context);
    }

    @Test
    void testIntegerPropertyConversion() {
        // Given: a service with integer property
        TestServiceWithTypes service = new TestServiceWithTypes();
        StandardInterfaceHandler handlerWithTypes = new StandardInterfaceHandler("/test", service);

        when(methodCall.getObjectPath()).thenReturn(DBusObjectPath.valueOf("/test"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Get"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(51));
        when(methodCall.getPayload())
                .thenReturn(
                        Arrays.asList(
                                DBusString.valueOf("com.example.TypedService"),
                                DBusString.valueOf("count")));

        // When: getting the integer property
        handlerWithTypes.handleInboundMessage(context, methodCall);

        // Then: should return integer variant
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        DBusVariant variant = (DBusVariant) reply.getPayload().get(0);
        DBusInt32 value = (DBusInt32) variant.getDelegate();
        assertEquals(42, value.getDelegate());
    }

    @Test
    void testBooleanPropertyConversion() {
        // Given: getting boolean property
        TestServiceWithTypes service = new TestServiceWithTypes();
        StandardInterfaceHandler handlerWithTypes = new StandardInterfaceHandler("/test", service);

        when(methodCall.getObjectPath()).thenReturn(DBusObjectPath.valueOf("/test"));
        when(methodCall.getInterfaceName())
                .thenReturn(
                        java.util.Optional.of(
                                DBusString.valueOf("org.freedesktop.DBus.Properties")));
        when(methodCall.getMember()).thenReturn(DBusString.valueOf("Get"));
        when(methodCall.getSerial()).thenReturn(DBusUInt32.valueOf(52));
        when(methodCall.getPayload())
                .thenReturn(
                        Arrays.asList(
                                DBusString.valueOf("com.example.TypedService"),
                                DBusString.valueOf("active")));

        // When: getting the boolean property
        handlerWithTypes.handleInboundMessage(context, methodCall);

        // Then: should return boolean variant
        verify(context)
                .propagateOutboundMessage(returnCaptor.capture(), any(CompletableFuture.class));
        OutboundMethodReturn reply = returnCaptor.getValue();

        DBusVariant variant = (DBusVariant) reply.getPayload().get(0);
        DBusBoolean value = (DBusBoolean) variant.getDelegate();
        assertFalse(value.getDelegate());
    }

    // Test service class
    @DBusInterface("com.example.TestService")
    static class TestService {
        @DBusProperty private String version = "1.0.0";

        @DBusProperty private boolean enabled = true;
    }

    // Test service with different types
    @DBusInterface("com.example.TypedService")
    static class TestServiceWithTypes {
        @DBusProperty private Integer count = 42;

        @DBusProperty private Boolean active = false;

        @DBusProperty
        private Object complexObject =
                new Object() {
                    @Override
                    public String toString() {
                        return "ComplexObject";
                    }
                };
    }
}
