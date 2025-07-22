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
 * Marks a method as a D-Bus method.
 * 
 * <p>This annotation is used to expose Java methods as D-Bus methods.
 * The method name in D-Bus will match the Java method name unless
 * explicitly specified.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @DBusMethod(name = "GetStatus")
 * public String getStatus() {
 *     return "active";
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DBusMethod {
  /**
   * The D-Bus method name. If not specified, the Java method name is used.
   * 
   * @return the D-Bus method name
   */
  String name() default "";
}