/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * D-Bus message codec framework for encoding and decoding D-Bus wire protocol data.
 *
 * <p>This package provides a comprehensive codec framework that handles the conversion between
 * D-Bus types and their wire protocol representation. It combines both encoding (Java objects to
 * bytes) and decoding (bytes to Java objects) functionality.
 *
 * <h2>Package Structure</h2>
 *
 * <p>The codec package is organized into two sub-packages:
 *
 * <ul>
 *   <li><strong>{@link com.lucimber.dbus.codec.encoder}</strong> - Encoding D-Bus types to wire
 *       format
 *   <li><strong>{@link com.lucimber.dbus.codec.decoder}</strong> - Decoding wire format to D-Bus
 *       types
 * </ul>
 *
 * <h2>Core Concepts</h2>
 *
 * <h3>Encoders</h3>
 *
 * <p>Encoders convert D-Bus types into their binary wire protocol representation:
 *
 * <pre>{@code
 * Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(ByteOrder.LITTLE_ENDIAN);
 * DBusString value = DBusString.valueOf("Hello");
 * EncoderResult<ByteBuffer> result = encoder.encode(value, 0);
 * ByteBuffer buffer = result.getBuffer();
 * }</pre>
 *
 * <h3>Decoders</h3>
 *
 * <p>Decoders convert binary data back into D-Bus types:
 *
 * <pre>{@code
 * Decoder<ByteBuffer, DBusString> decoder = new StringDecoder(ByteOrder.LITTLE_ENDIAN);
 * ByteBuffer buffer = ... // wire format data
 * DecoderResult<DBusString> result = decoder.decode(buffer, 0);
 * DBusString value = result.getValue();
 * }</pre>
 *
 * <h3>Factory Pattern</h3>
 *
 * <p>Both encoders and decoders use factory patterns for type-based instantiation:
 *
 * <pre>{@code
 * EncoderFactory encoderFactory = new DefaultEncoderFactory(ByteOrder.LITTLE_ENDIAN);
 * Encoder encoder = encoderFactory.create(Type.STRING);
 *
 * DecoderFactory decoderFactory = new DefaultDecoderFactory(ByteOrder.LITTLE_ENDIAN);
 * Decoder decoder = decoderFactory.create(TypeCode.STRING);
 * }</pre>
 *
 * <h2>Wire Protocol Details</h2>
 *
 * <p>The codec framework handles all D-Bus wire protocol requirements:
 *
 * <ul>
 *   <li><strong>Alignment:</strong> Proper padding for type alignment (1, 2, 4, or 8 bytes)
 *   <li><strong>Byte Order:</strong> Support for both little-endian and big-endian encoding
 *   <li><strong>Type Signatures:</strong> Encoding and validation of D-Bus type signatures
 *   <li><strong>Array Lengths:</strong> Proper length encoding for arrays and dicts
 *   <li><strong>Struct Alignment:</strong> 8-byte alignment for struct boundaries
 * </ul>
 *
 * <h2>Supported Types</h2>
 *
 * <p>The codec framework supports all D-Bus types:
 *
 * <ul>
 *   <li><strong>Basic Types:</strong> BYTE, BOOLEAN, INT16, UINT16, INT32, UINT32, INT64, UINT64,
 *       DOUBLE, STRING, OBJECT_PATH, SIGNATURE, UNIX_FD
 *   <li><strong>Container Types:</strong> ARRAY, STRUCT, VARIANT, DICT_ENTRY
 * </ul>
 *
 * <h2>Error Handling</h2>
 *
 * <p>The codec framework uses specific exceptions for error conditions:
 *
 * <ul>
 *   <li>{@code EncoderException} - Thrown when encoding fails
 *   <li>{@code DecoderException} - Thrown when decoding fails
 * </ul>
 *
 * <h2>Usage in Message Processing</h2>
 *
 * <p>The codec framework is primarily used by the Netty transport layer for message serialization:
 *
 * <pre>{@code
 * // In FrameEncoder
 * Encoder<DBusArray<DBusStruct>, ByteBuffer> encoder =
 *     new ArrayEncoder<>(order, signature);
 * EncoderResult<ByteBuffer> result = encoder.encode(headerFields, offset);
 *
 * // In FrameDecoder
 * Decoder<ByteBuffer, DBusArray<DBusStruct>> decoder =
 *     new ArrayDecoder<>(order, signature);
 * DecoderResult<DBusArray<DBusStruct>> result = decoder.decode(buffer, offset);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>Encoder and decoder instances are NOT thread-safe. Create separate instances for each thread
 * or use synchronization. The factory classes are thread-safe.
 *
 * @see com.lucimber.dbus.codec.encoder
 * @see com.lucimber.dbus.codec.decoder
 * @see com.lucimber.dbus.type
 * @see com.lucimber.dbus.netty
 * @since 1.0.0
 */
package com.lucimber.dbus.codec;
