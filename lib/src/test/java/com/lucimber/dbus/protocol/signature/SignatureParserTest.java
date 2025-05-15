/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature;

import static org.junit.jupiter.api.Assertions.*;
import com.lucimber.dbus.protocol.signature.ast.*;
import org.junit.jupiter.api.Test;
import java.util.List;

class SignatureParserTest {

  @Test
  void testSimpleSequence() {
    TypeDescriptor desc = new SignatureParser("ayvtib").parse();
    assertNotNull(desc);
    assertInstanceOf(Struct.class, desc);
    List<TypeDescriptor> members = ((Struct) desc).members();
    assertEquals(5, members.size());
  }

  @Test
  void testArraySequence() {
    TypeDescriptor desc = new SignatureParser("a{ib}").parse();
    assertNotNull(desc);
    assertInstanceOf(Array.class, desc);
  }

  @Test
  void testComplexSequence() {
    TypeDescriptor desc = new SignatureParser("aya(vt)ib(i((dq)u))").parse();
    assertNotNull(desc);
    assertInstanceOf(Struct.class, desc);
    assertEquals(5, ((Struct) desc).members().size());
  }

  @Test
  void testComplexSequence2() {
    TypeDescriptor desc = new SignatureParser("aya{it}ib(i((dq)(u)))").parse();
    assertNotNull(desc);
    assertInstanceOf(Struct.class, desc);
    assertEquals(5, ((Struct) desc).members().size());
  }

  @Test
  void testModerateSequence() {
    TypeDescriptor desc = new SignatureParser("ay(vt)ib").parse();
    assertNotNull(desc);
    assertInstanceOf(Struct.class, desc);
    assertEquals(4, ((Struct) desc).members().size());
  }

  @Test
  void testStructSequence() {
    TypeDescriptor desc = new SignatureParser("(ii)").parse();
    assertNotNull(desc);
    assertInstanceOf(Struct.class, desc);
    assertEquals(2, ((Struct) desc).members().size());
  }

  @Test
  void testInvalidSignatureNull() {
    assertThrows(SignatureParseException.class,
          () -> new SignatureParser(null));
  }

  @Test
  void testInvalidSignatureChars() {
    assertThrows(SignatureParseException.class,
          () -> new SignatureParser("abc!"));
  }

  @Test
  void testInvalidSignatureTooLong() {
    String longSig = "a".repeat(256);
    assertThrows(SignatureParseException.class,
          () -> new SignatureParser(longSig));
  }
}