/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection.sasl;

/** Contains all SASL command names used by D-Bus. */
public enum SaslCommandName {
    /**
     * The {@code AGREE_UNIX_FD} command is sent by the server to the client.
     *
     * <p>The {@code AGREE_UNIX_FD} command indicates that the server supports Unix file descriptor
     * passing. This command may only be sent after the connection is authenticated, and the client
     * sent {@code NEGOTIATE_UNIX_FD} to enable Unix file descriptor passing. This command may only
     * be sent on transports that support Unix file descriptor passing.
     *
     * <p>On receiving {@code AGREE_UNIX_FD} the client must respond with {@code BEGIN}, followed by
     * its stream of messages, or by disconnecting. The server must not accept additional commands
     * using this protocol after the {@code BEGIN} command has been received. Further communication
     * will be a stream of D-Bus messages (optionally encrypted, as negotiated) rather than this
     * protocol.
     */
    AGREE_UNIX_FD,
    /**
     * The {@code AUTH} command is sent by the client to the server. The server replies with {@code
     * DATA}, {@code OK} or {@code REJECTED}.
     *
     * <p>If an {@code AUTH} command has no arguments, it is a request to list available mechanisms.
     * The server must respond with a {@code REJECTED} command listing the mechanisms it
     * understands, or with an error.
     *
     * <p>If an {@code AUTH} command specifies a mechanism, and the server supports said mechanism,
     * the server should begin exchanging SASL challenge-response data with the client using {@code
     * DATA} commands.
     *
     * <p>If the server does not support the mechanism given in the {@code AUTH} command, it must
     * send either a {@code REJECTED} command listing the mechanisms it does support, or an error.
     *
     * <p>If the [initial-response] argument is provided, it is intended for use with mechanisms
     * that have no initial challenge (or an empty initial challenge), as if it were the argument to
     * an initial {@code DATA} command. If the selected mechanism has an initial challenge and
     * [initial-response] was provided, the server should reject authentication by sending {@code
     * REJECTED}.
     *
     * <p>If authentication succeeds after exchanging {@code DATA} commands, an {@code OK} command
     * must be sent to the client.
     */
    AUTH,
    /**
     * The {@code BEGIN} command is sent by the client to the server. The server does not reply.
     *
     * <p>The {@code BEGIN} command acknowledges that the client has received an {@code OK} command
     * from the server and completed any feature negotiation that it wishes to do, and declares that
     * the stream of messages is about to begin.
     *
     * <p>The first octet received by the server after the \r\n of the {@code BEGIN} command from
     * the client must be the first octet of the authenticated/encrypted stream of D-Bus messages.
     *
     * <p>Unlike all other commands, the server does not reply to the {@code BEGIN} command with an
     * authentication command of its own. After the \r\n of the reply to the command before {@code
     * BEGIN}, the next octet received by the client must be the first octet of the
     * authenticated/encrypted stream of D-Bus messages.
     */
    BEGIN,
    /**
     * The {@code CANCEL} command is sent by the client to the server. The server replies with
     * {@code REJECTED}.
     *
     * <p>At any time up to sending the {@code BEGIN} command, the client may send a {@code CANCEL}
     * command. On receiving the {@code CANCEL} command, the server must send a {@code REJECTED}
     * command and abort the current authentication exchange.
     */
    CANCEL,
    /**
     * The {@code DATA} command may come from either client or server, and simply contains a
     * hex-encoded block of data to be interpreted according to the SASL mechanism in use. If sent
     * by the client, the server replies with {@code DATA}, {@code OK} or {@code REJECTED}.
     */
    DATA,
    /**
     * The {@code ERROR} command can be sent in either direction. If sent by the client, the server
     * replies with {@code REJECTED}.
     *
     * <p>The {@code ERROR} command indicates that either server or client did not know a command,
     * does not accept the given command in the current context, or did not understand the arguments
     * to the command. This allows the protocol to be extended; a client or server can send a
     * command present or permitted only in new protocol versions, and if an {@code ERROR} is
     * received instead of an appropriate response, fall back to using some other technique.
     *
     * <p>If an {@code ERROR} is sent, the server or client that sent the error must continue as if
     * the command causing the {@code ERROR} had never been received. However, the server or client
     * receiving the error should try something other than whatever caused the error; if only
     * canceling/rejecting the authentication.
     *
     * <p>If the D-Bus protocol changes incompatibly at some future time, applications implementing
     * the new protocol would probably be able to check for support of the new protocol by sending a
     * new command and receiving an {@code ERROR} from applications that don't understand it. Thus
     * the {@code ERROR} feature of the auth protocol is an escape hatch that lets us negotiate
     * extensions or changes to the D-Bus protocol in the future.
     */
    ERROR,
    /**
     * The {@code NEGOTIATE_UNIX_FD} command is sent by the client to the server. The server replies
     * with {@code AGREE_UNIX_FD} or {@code ERROR}.
     *
     * <p>The {@code NEGOTIATE_UNIX_FD} command indicates that the client supports Unix file
     * descriptor passing. This command may only be sent after the connection is authenticated, i.e.
     * after {@code OK} was received by the client. This command may only be sent on transports that
     * support Unix file descriptor passing.
     *
     * <p>On receiving {@code NEGOTIATE_UNIX_FD} the server must respond with either {@code
     * AGREE_UNIX_FD} or {@code ERROR}. It shall respond the former if the transport chosen supports
     * Unix file descriptor passing and the server supports this feature. It shall respond the
     * latter if the transport does not support Unix file descriptor passing, the server does not
     * support this feature, or the server decides not to enable file descriptor passing due to
     * security or other reasons.
     */
    NEGOTIATE_UNIX_FD,
    /**
     * The {@code OK} command is sent by the server to the client.
     *
     * <p>The {@code OK} command indicates that the client has been authenticated. The client may
     * now proceed with negotiating Unix file descriptor passing. To do that it shall send {@code
     * NEGOTIATE_UNIX_FD} to the server.
     *
     * <p>Otherwise, the client must respond to the {@code OK} command by sending a {@code BEGIN}
     * command, followed by its stream of messages, or by disconnecting. The server must not accept
     * additional commands using this protocol after the {@code BEGIN} command has been received.
     * Further communication will be a stream of D-Bus messages (optionally encrypted, as
     * negotiated) rather than this protocol.
     *
     * <p>If there is no negotiation, the first octet received by the client after the \r\n of the
     * {@code OK} command must be the first octet of the authenticated/encrypted stream of D-Bus
     * messages. If the client negotiates Unix file descriptor passing, the first octet received by
     * the client after the \r\n of the {@code AGREE_UNIX_FD} or {@code ERROR} reply must be the
     * first octet of the authenticated/encrypted stream.
     *
     * <p>The {@code OK} command has one argument, which is the GUID of the server. See the section
     * called “Server Addresses” for more on server GUIDs.
     */
    OK,
    /**
     * The {@code REJECTED} command is sent by the server to the client.
     *
     * <p>The {@code REJECTED} command indicates that the current authentication exchange has
     * failed, and further exchange of {@code DATA} is inappropriate. The client would normally try
     * another mechanism, or try providing different responses to challenges.
     *
     * <p>Optionally, the {@code REJECTED} command has a space-separated list of available auth
     * mechanisms as arguments. If a server ever provides a list of supported mechanisms, it must
     * provide the same list each time it sends a {@code REJECTED} message. Clients are free to
     * ignore all lists received after the first.
     */
    REJECTED
}
