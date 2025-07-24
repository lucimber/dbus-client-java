/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection.sasl;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** A SASL message exchanged between a D-Bus instance (server) and a client application. */
public final class SaslMessage {

    private static final Set<SaslCommandName> COMMANDS_WITHOUT_ARGS =
            EnumSet.of(
                    SaslCommandName.BEGIN,
                    SaslCommandName.CANCEL,
                    SaslCommandName.NEGOTIATE_UNIX_FD,
                    SaslCommandName.AGREE_UNIX_FD);

    private final SaslCommandName commandName;
    private final String commandArgs;

    /**
     * Constructs a new {@code SaslMessage}.
     *
     * @param commandName the name of the command (not null)
     * @param commandArgs the optional args of the command (nullable)
     * @throws NullPointerException if {@code commandName} is null
     * @throws IllegalArgumentException if {@code commandArgs} is blank or not allowed
     */
    public SaslMessage(SaslCommandName commandName, String commandArgs) {
        this.commandName = Objects.requireNonNull(commandName, "Command name must not be null");
        this.commandArgs = commandArgs;
        validateCommand();
    }

    /**
     * Returns the name of the SASL command.
     *
     * @return the command name
     */
    public SaslCommandName getCommandName() {
        return commandName;
    }

    /**
     * Returns the argument(s) of the SASL command, if present.
     *
     * @return an {@link Optional} containing the command args
     */
    public Optional<String> getCommandArgs() {
        return Optional.ofNullable(commandArgs);
    }

    @Override
    public String toString() {
        return commandArgs == null ? commandName.name() : commandName.name() + " " + commandArgs;
    }

    private void validateCommand() {
        if (commandArgs != null && commandArgs.isBlank()) {
            throw new IllegalArgumentException("Command args must not be a blank string");
        }

        if (COMMANDS_WITHOUT_ARGS.contains(commandName) && commandArgs != null) {
            throw new IllegalArgumentException(
                    "Command args must be null for command: " + commandName.name());
        }
    }
}
