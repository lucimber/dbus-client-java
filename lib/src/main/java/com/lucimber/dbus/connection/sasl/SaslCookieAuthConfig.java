package com.lucimber.dbus.connection.sasl;

import java.util.Objects;

/**
 * {@code DBUS_COOKIE_SHA1} is a D-Bus-specific SASL mechanism.
 * Its reference implementation is part of the reference implementation of D-Bus.
 *
 * <p>This mechanism is designed to establish that a client has the ability
 * to read a private file owned by the user being authenticated.
 * If the client can prove that it has access to a secret cookie stored in this file,
 * then the client is authenticated.
 * Thus the security of {@code DBUS_COOKIE_SHA1} depends on a secure home directory.
 * This is the recommended authentication mechanism for platforms
 * and configurations where EXTERNAL cannot be used.
 *
 * <p>Throughout this description, "hex encoding" must output the digits from a to f in lower-case;
 * the digits A to F must not be used in the {@code DBUS_COOKIE_SHA1} mechanism.
 *
 * <p>Authentication proceeds as follows:
 *
 * <ul>
 * <li>The client sends the username it would like to authenticate as, hex-encoded.
 * <li>The server sends the name of its "cookie context" (see below); a space character;
 * the integer ID of the secret cookie the client must demonstrate knowledge of; a space character;
 * then a randomly-generated challenge string, all of this hex-encoded into one, single string.
 * <li>The client locates the cookie and generates its own randomly-generated challenge string.
 * The client then concatenates the server's decoded challenge, a ":" character, its own challenge,
 * another ":" character, and the cookie. It computes the SHA-1 hash of this composite string as a hex digest.
 * It concatenates the client's challenge string, a space character, and the SHA-1 hex digest,
 * hex-encodes the result and sends it back to the server.
 * <li>The server generates the same concatenated string used by the client and computes its SHA-1 hash.
 * It compares the hash with the hash received from the client; if the two hashes match, the client is authenticated.
 * </ul>
 *
 * <p>Each server has a "cookie context," which is a name that identifies a set of cookies
 * that apply to that server. A sample context might be "org_freedesktop_session_bus".
 * Context names must be valid ASCII, nonzero length, and may not contain the characters slash ("/"),
 * backslash ("\"), space (" "), newline ("\n"), carriage return ("\r"), tab ("\t"), or period (".").
 * There is a default context, "org_freedesktop_general" that's used by servers that do not specify otherwise.
 *
 * <p>Cookies are stored in a user's home directory, in the directory ~/.dbus-keyrings/.
 * This directory must not be readable or writable by other users.
 * If it is, clients and servers must ignore it.
 * The directory contains cookie files named after the cookie context.
 *
 * <p>A cookie file contains one cookie per line. Each line has three space-separated fields:
 *
 * <ul>
 * <li>The cookie ID number, which must be a non-negative integer and may not be used twice in the same file.
 * <li>The cookie's creation time, in UNIX seconds-since-the-epoch format.
 * <li>The cookie itself, a hex-encoded random block of bytes. The cookie may be of any length,
 * though obviously security increases as the length increases.
 * </ul>
 *
 * <p>Only server processes modify the cookie file. They must do so with this procedure:
 *
 * <ul>
 * <li>Create a lockfile name by appending ".lock" to the name of the cookie file.
 * The server should attempt to create this file using O_CREAT | O_EXCL.
 * If file creation fails, the lock fails. Servers should retry for a reasonable period of time,
 * then they may choose to delete an existing lock to keep users from having to manually delete a stale lock.
 * <li>Once the lockfile has been created, the server loads the cookie file.
 * It should then delete any cookies that are old (the timeout can be fairly short),
 * or more than a reasonable time in the future (so that cookies never accidentally become permanent,
 * if the clock was set far into the future at some point).
 * If no recent keys remain, the server may generate a new key.
 * <li>The pruned and possibly added-to cookie file must be resaved atomically
 * (using a temporary file which is rename()'d).
 * <li>The lock must be dropped by deleting the lockfile.
 * </ul>
 *
 * <p>Clients need not lock the file in order to load it, because servers are required to save the file atomically.
 */
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
