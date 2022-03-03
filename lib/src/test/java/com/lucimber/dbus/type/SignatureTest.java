package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class SignatureTest {

  private static final String ARRAY_SEQUENCE = "a{ib}";
  private static final String COMPLEX_SEQUENCE = "aya(vt)ib(i((dq)u))";
  private static final String COMPLEX_SEQUENCE_2 = "aya{it}ib(i((dq)(u)))";
  private static final String MODERATE_SEQUENCE = "ay(vt)ib";
  private static final String SIMPLE_SEQUENCE = "ayvtib";
  private static final String STRUCT_SEQUENCE = "(ii)";

  @Test
  public void succeedWithParsingSimpleSequence() throws SignatureException {
    final Signature signature = Signature.valueOf(SIMPLE_SEQUENCE);
    final int expectedQuantity = 5;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(SIMPLE_SEQUENCE, signature.toString());
  }

  @Test
  public void succeedWithParsingModerateSequence() throws SignatureException {
    final Signature signature = Signature.valueOf(MODERATE_SEQUENCE);
    final int expectedQuantity = 4;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(MODERATE_SEQUENCE, signature.toString());
  }

  @Test
  public void succeedWithParsingComplexSequence() throws SignatureException {
    final Signature signature = Signature.valueOf(COMPLEX_SEQUENCE);
    final int expectedQuantity = 5;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(COMPLEX_SEQUENCE, signature.toString());
  }

  @Test
  public void succeedWithParsingComplexSequence2() throws SignatureException {
    final Signature signature = Signature.valueOf(COMPLEX_SEQUENCE_2);
    final int expectedQuantity = 5;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(COMPLEX_SEQUENCE_2, signature.toString());
  }

  @Test
  public void succeedWithParsingStructSequence() throws SignatureException {
    final Signature signature = Signature.valueOf(STRUCT_SEQUENCE);
    final int expectedQuantity = 1;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(STRUCT_SEQUENCE, signature.toString());
  }

  @Test
  public void succeedWithParsingDictionary() throws SignatureException {
    final Signature signature = Signature.valueOf(ARRAY_SEQUENCE);
    final int expectedQuantity = 1;
    assertEquals(expectedQuantity, signature.getQuantity());
    assertEquals(ARRAY_SEQUENCE, signature.toString());
    final Signature subSignature = signature.subContainer();
    assertEquals("{ib}", subSignature.toString());
    final Signature subSubSig = subSignature.subContainer();
    assertEquals("ib", subSubSig.toString());
  }

  @Test
  public void equalsAndHashCode() {
    final Signature a = Signature.valueOf("a{oa{sa{sv}}}");
    final Signature b = Signature.valueOf("a{oa{sa{sv}}}");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void failDueToEmptyStruct() {
    final String sequence = "so()";
    assertThrows(SignatureException.class, () -> Signature.valueOf(sequence));
  }

  @Test
  public void failDueToEmptyDictEntry() {
    final String sequence = "a{}";
    assertThrows(SignatureException.class, () -> Signature.valueOf(sequence));
  }

  @Test
  public void failDueToMisplacedDictEntry() {
    final String sequence = "n{}t";
    assertThrows(SignatureException.class, () -> Signature.valueOf(sequence));
  }
}
