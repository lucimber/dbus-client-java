package io.lucimber.dbus.connection.sasl;

import java.util.HashMap;
import java.util.Map;

public enum SaslAuthMechanism {
    EXTERNAL("EXTERNAL"),
    COOKIE("DBUS_COOKIE_SHA1"),
    ANONYMOUS("ANONYMOUS");

    private static final Map<String, SaslAuthMechanism> STRING_TO_ENUM = new HashMap<>();

    static {
        for (SaslAuthMechanism mechanism : values()) {
            STRING_TO_ENUM.put(mechanism.toString(), mechanism);
        }
    }

    private final String customName;

    SaslAuthMechanism(final String customName) {
        this.customName = customName;
    }

    /**
     * Translates the custom string representation back to the corresponding enum.
     *
     * @param customName The custom string representation.
     * @return The corresponding {@link SaslAuthMechanism}.
     */
    public static SaslAuthMechanism fromString(final String customName) {
        return STRING_TO_ENUM.get(customName);
    }

    @Override
    public String toString() {
        return customName;
    }
}
