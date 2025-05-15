/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature;

import com.lucimber.dbus.protocol.signature.ast.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a D-Bus type signature (e.g. "aya{it}ib(i((dq)(u)))")
 * into a tree of {@link TypeDescriptor} nodes.
 *
 * <p>This is a recursive-descent implementation with basic validation.</p>
 *
 * @since 2.0
 */
public final class SignatureParser {

  private static final int MAX_SIGNATURE_LENGTH = 255;
  private static final String VALID_SIG_REGEX = "^[aybnqiuxtdsoghv\\{\\}\\(\\)]+$";

  private final String sig;
  private int pos;

  /**
   * Creates a parser for the given signature.
   *
   * @param signature a non-null D-Bus type signature
   * @throws SignatureParseException if signature is null, too long, or contains invalid characters
   */
  public SignatureParser(final String signature) {
    if (signature == null) {
      throw new SignatureParseException("Signature must not be null");
    }
    if (signature.length() > MAX_SIGNATURE_LENGTH) {
      throw new SignatureParseException(
            "Signature length exceeds " + MAX_SIGNATURE_LENGTH + ": " + signature);
    }
    if (!signature.matches(VALID_SIG_REGEX)) {
      throw new SignatureParseException(
            "Signature contains invalid characters: " + signature);
    }
    this.sig = signature;
    this.pos = 0;
  }

  /**
   * Parses the entire signature string.
   *
   * @return a single {@link TypeDescriptor}, or a {@link Struct}
   *         if multiple top-level types were present
   * @throws SignatureParseException on malformed signature
   */
  public TypeDescriptor parse() {
    List<TypeDescriptor> types = new ArrayList<>();
    while (pos < sig.length()) {
      types.add(parseSingle());
    }
    if (pos != sig.length()) {
      throw new SignatureParseException(
            "Extra unparsed characters at position " + pos + " in " + sig);
    }
    return types.size() == 1 ? types.get(0) : new Struct(types);
  }

  private TypeDescriptor parseSingle() {
    char c = sig.charAt(pos++);
    return switch (c) {
      case 'a' -> {
        // array: next element type
        TypeDescriptor elem = parseSingle();
        yield new Array(elem);
      }
      case '{' -> {
        // dict entry: key then value, then '}'
        TypeDescriptor key = parseSingle();
        TypeDescriptor val = parseSingle();
        expect('}');
        yield new Dict(key, val);
      }
      case '(' -> {
        // struct: members until ')'
        List<TypeDescriptor> members = new ArrayList<>();
        while (peek() != ')') {
          members.add(parseSingle());
        }
        expect(')');
        yield new Struct(members);
      }
      case 'v' -> Variant.INSTANCE;
      default -> // basic type
            new Basic(c);
    };
  }

  private char peek() {
    if (pos >= sig.length()) {
      throw new SignatureParseException(
            "Unexpected end of signature: " + sig);
    }
    return sig.charAt(pos);
  }

  private void expect(final char expected) {
    if (pos >= sig.length() || sig.charAt(pos) != expected) {
      throw new SignatureParseException(
            "Expected '" + expected + "' at position " + pos + " in signature " + sig);
    }
    pos++;
  }
}
