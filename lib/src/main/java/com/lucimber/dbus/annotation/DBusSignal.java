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
 * Marks a method as a D-Bus signal emitter.
 *
 * <p>This annotation is used to mark methods that emit D-Bus signals. The method should typically
 * return void and its parameters define the signal arguments.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @DBusSignal(name = "StatusChanged")
 * public void emitStatusChanged(String oldStatus, String newStatus) {
 *     // Signal emission handled by framework
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DBusSignal {
    /**
     * The D-Bus signal name. If not specified, the Java method name is used.
     *
     * @return the D-Bus signal name
     */
    String name() default "";
}
