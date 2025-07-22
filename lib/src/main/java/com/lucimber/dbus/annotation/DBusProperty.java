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
 * Marks a field or getter/setter method as a D-Bus property.
 * 
 * <p>This annotation can be applied to:</p>
 * <ul>
 *   <li>Fields - the field will be exposed as a read-write property</li>
 *   <li>Getter methods - creates a read-only property</li>
 *   <li>Setter methods - creates a write-only property</li>
 *   <li>Both getter and setter - creates a read-write property</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @DBusProperty(name = "Version")
 * private String version = "1.0.0";
 * 
 * @DBusProperty
 * public int getCount() {
 *     return count;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DBusProperty {
  /**
   * The D-Bus property name. If not specified, the field name or
   * method name (without get/set prefix) is used.
   * 
   * @return the D-Bus property name
   */
  String name() default "";
  
  /**
   * The access mode for the property.
   * 
   * @return the property access mode
   */
  Access access() default Access.AUTO;
  
  /**
   * Property access modes.
   */
  enum Access {
    /**
     * Automatically determine access based on available methods/field visibility.
     */
    AUTO,
    /**
     * Read-only property.
     */
    READ,
    /**
     * Write-only property.
     */
    WRITE,
    /**
     * Read-write property.
     */
    READWRITE
  }
}