/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java interface or class as a D-Bus interface.
 *
 * <p>This annotation is used to specify the D-Bus interface name that the annotated type
 * represents. It's used by the introspection and property discovery mechanisms.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @DBusInterface("com.example.MyService")
 * public interface MyService {
 *     @DBusMethod
 *     String doSomething(String input);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DBusInterface {
    /**
     * The D-Bus interface name.
     *
     * @return the interface name in D-Bus dotted notation (e.g., "com.example.Service")
     */
    String value();
}
