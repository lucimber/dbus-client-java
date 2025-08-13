/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.type;

import java.util.HashMap;
import java.util.Map;

/** A straightforward lexer for type codes embedded in a sequence of characters. */
final class TypeCodeLexer {

    private static final Map<Character, TypeCode> CHAR_TO_ENUM = new HashMap<>();

    static {
        for (TypeCode code : TypeCode.values()) {
            CHAR_TO_ENUM.put(code.getChar(), code);
        }
    }

    private TypeCodeLexer() {
        // Utility class
    }

    /**
     * Produces tokens from a sequence of characters.
     *
     * @param sequence a {@link CharSequence}
     * @return an array of {@link TypeCode}s
     */
    static TypeCode[] produceTokens(final CharSequence sequence) {
        final TypeCode[] codes = new TypeCode[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            final TypeCode code = CHAR_TO_ENUM.get(sequence.charAt(i));
            if (code == null) {
                final String msg =
                        String.format("Sequence contains invalid code at position %d", i);
                throw new IllegalArgumentException(msg);
            } else {
                codes[i] = code;
            }
        }
        return codes;
    }
}
