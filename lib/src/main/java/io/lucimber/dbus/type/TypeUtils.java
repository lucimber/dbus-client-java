package io.lucimber.dbus.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TypeUtils {

    private static final Map<Character, TypeAlignment> CHAR_TO_ALIGNMENT = new HashMap<>();
    private static final Map<Character, TypeCode> CHAR_TO_CODE = new HashMap<>();
    private static final Map<Character, Type> CHAR_TO_TYPE = new HashMap<>();
    private static final Map<TypeCode, Type> CODE_TO_TYPE = new HashMap<>();

    static {
        for (final Type type : Type.values()) {
            CHAR_TO_ALIGNMENT.put(type.getCode().getChar(), type.getAlignment());
            CHAR_TO_CODE.put(type.getCode().getChar(), type.getCode());
            CHAR_TO_TYPE.put(type.getCode().getChar(), type);
            CODE_TO_TYPE.put(type.getCode(), type);
        }
    }

    private TypeUtils() {
        // Utility class
    }

    /**
     * Gets the type's alignment from its char.
     *
     * @param c a {@link Character}
     * @return an {@link Optional} of {@link TypeAlignment}
     */
    public static Optional<TypeAlignment> getAlignmentFromChar(final char c) {
        return Optional.ofNullable(CHAR_TO_ALIGNMENT.get(c));
    }

    /**
     * Gets the type's code from its char.
     *
     * @param c a {@link Character}
     * @return an {@link Optional} of {@link TypeCode}
     */
    public static Optional<TypeCode> getCodeFromChar(final char c) {
        return Optional.ofNullable(CHAR_TO_CODE.get(c));
    }

    /**
     * Gets the type from its type char.
     *
     * @param c a {@link Character}
     * @return an {@link Optional} of {@link Type}
     */
    public static Optional<Type> getTypeFromChar(final char c) {
        return Optional.ofNullable(CHAR_TO_TYPE.get(c));
    }

    /**
     * Gets the type from its type code.
     *
     * @param code a {@link TypeCode}
     * @return an {@link Optional} of {@link Type}
     */
    public static Optional<Type> getTypeFromCode(final TypeCode code) {
        return Optional.ofNullable(CODE_TO_TYPE.get(code));
    }
}
