package com.lucimber.dbus.connection.sasl;

import java.util.Objects;

public final class SaslCookieAuthConfig implements SaslAuthConfig {

    private final String absCookieDirPath;
    private final String identity;

    public SaslCookieAuthConfig(final String identity, final String absCookieDirPath) {
        this.identity = Objects.requireNonNull(identity);
        this.absCookieDirPath = Objects.requireNonNull(absCookieDirPath);
    }

    @Override
    public SaslAuthMechanism getAuthMechanism() {
        return SaslAuthMechanism.COOKIE;
    }

    public String getAbsCookieDirPath() {
        return absCookieDirPath;
    }

    public String getIdentity() {
        return identity;
    }
}
