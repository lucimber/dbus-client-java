/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * D-Bus uses a string-based type encoding mechanism called Signatures to describe the number
 * and types of arguments required by methods and signals.
 * Signatures are used for interface declaration/documentation, data marshalling, and validity checking.
 * Their string encoding uses a simple, though expressive, format
 * and a basic understanding of it is required for effective D-Bus use.
 *
 * @see <a href="https://pythonhosted.org/txdbus/dbus_overview.html">DBus Overview (Key Components)</a>
 */
public final class Signature implements DBusBasicType {

  private static final short MAX_SIGNATURE_LENGTH = 255;
  private final Node rootNode;

  private Signature(final Node rootNode) {
    this.rootNode = Objects.requireNonNull(rootNode);
  }

  /**
   * Constructs a new {@link Signature} instance by parsing a {@link CharSequence}.
   *
   * @param sequence The sequence composed of one or multiple single complete types.
   * @return A new instance of {@link Signature}.
   * @throws SignatureException If the given {@link CharSequence} is not well-formed.
   */
  public static Signature valueOf(final CharSequence sequence) throws SignatureException {
    Objects.requireNonNull(sequence, "sequence must not be null");
    if (sequence.length() == 0) {
      throw new SignatureException("sequence must not be empty");
    }
    if (sequence.length() > MAX_SIGNATURE_LENGTH) {
      throw new SignatureException("Sequence exceeds maximum allowed length.");
    }
    final TypeCode[] codes = TypeCodeLexer.produceTokens(sequence);
    return parse(codes);
  }

  private static int countCode(final TypeCode[] codes, final TypeCode code) {
    int quantity = 0;
    for (TypeCode typeCode : codes) {
      if (typeCode == code) {
        quantity++;
      }
    }
    return quantity;
  }

  private static Signature parse(final TypeCode[] codes) {
    ensureBracketBalance(codes);
    final Node rootNode = new Node();
    Node node = rootNode;
    for (int i = 0; i < codes.length; i++) {
      final TypeCode code = codes[i];
      if (code == TypeCode.ARRAY) {
        final Optional<Node> optNode = parseArrayTypeCode(node, code, i, codes.length - 1);
        if (optNode.isPresent()) {
          node = optNode.get();
        }
      } else if (code == TypeCode.DICT_ENTRY_START) {
        node = parseDictEntryStartTypeCode(node, i, codes.length - 1);
      } else if (code == TypeCode.DICT_ENTRY_END) {
        node = parseDictEntryEndTypeCode(node, i);
      } else if (code == TypeCode.STRUCT_START) {
        node = parseStructStartTypeCode(node, i, codes.length - 1);
      } else if (code == TypeCode.STRUCT_END) {
        node = parseStructEndTypeCode(node, i);
      } else {
        final Optional<Node> optNode = parseTypeCode(node, code, i, codes.length - 1);
        if (optNode.isPresent()) {
          node = optNode.get();
        }
      }
    }
    if (rootNode.type == null && rootNode.children != null && rootNode.children.size() == 1) {
      final Node child = rootNode.children.get(0);
      rootNode.type = child.type;
      rootNode.children = new ArrayList<>();
      for (Node subChild : child.children) {
        rootNode.children.add(deepClone(subChild, rootNode));
      }
    }
    return new Signature(rootNode);
  }

  private static void ensureBracketBalance(final TypeCode[] codes) {
    final int numDictEntryStart = countCode(codes, TypeCode.DICT_ENTRY_START);
    final int numDictEntryEnd = countCode(codes, TypeCode.DICT_ENTRY_END);
    if (numDictEntryStart != numDictEntryEnd) {
      throw new SignatureException("mismatch of dict-entry braces");
    }
    final int numStructStart = countCode(codes, TypeCode.STRUCT_START);
    final int numStructEnd = countCode(codes, TypeCode.STRUCT_END);
    if (numStructStart != numStructEnd) {
      throw new SignatureException("mismatch of struct parentheses");
    }
  }

  private static Optional<Node> parseTypeCode(final Node node, final TypeCode code,
                                              final int idx, final int lastIdx) {
    final Type type = TypeUtils.getTypeFromCode(code)
            .orElseThrow(() -> new SignatureException("can not map code to type: " + code));
    if (idx == lastIdx && node.children == null && node.type == null) {
      node.type = type;
    } else {
      node.addChild(type);
      if (node.type == Type.ARRAY) {
        return Optional.ofNullable(node.parent);
      }
    }
    return Optional.empty();
  }

  private static Node parseStructEndTypeCode(final Node node, final int idx) {
    if (node.type != Type.STRUCT) {
      final String msg = "Error at position %d."
              + " Missing opening parenthesis of struct.";
      throw new SignatureException(String.format(msg, idx));
    }
    if (node.children == null || node.children.isEmpty()) {
      final String msg = "Error at position %d."
              + " Empty structures are not allowed.";
      throw new SignatureException(String.format(msg, idx));
    }
    if (node.parent != null && node.parent.type == Type.ARRAY) {
      Node refNode = node.parent;
      while (refNode.type == Type.ARRAY) {
        refNode = refNode.parent;
      }
      return refNode;
    } else {
      return node.parent;
    }
  }

  private static Optional<Node> parseArrayTypeCode(final Node node, final TypeCode code,
                                                   final int idx, final int lastIdx) {
    if (idx == lastIdx) {
      final String msg = "Error at position %d."
              + " Array type code must be followed by a single complete type.";
      throw new SignatureException(String.format(msg, idx));
    }
    final Type type = TypeUtils.getTypeFromCode(code)
            .orElseThrow(() -> new SignatureException("can not map code to type: " + code));
    if (idx == lastIdx - 1 && node.type == null && node.children == null) {
      node.type = type;
      return Optional.empty();
    } else {
      final Node child = node.addChild(type);
      return Optional.of(child);
    }
  }

  private static Node parseDictEntryStartTypeCode(final Node node, final int idx,
                                                  final int lastIdx) {
    if (node.type != null && node.type != Type.ARRAY) {
      final String msg = "Error at position %d."
              + " Dict-entry must occur only as an array element type.";
      throw new SignatureException(String.format(msg, idx));
    }
    if (idx == lastIdx) {
      final String msg = "Error at position %d."
              + " Missing closing curly bracket of dict-entry.";
      throw new SignatureException(String.format(msg, idx));
    }
    return node.addChild(Type.DICT_ENTRY);
  }

  private static Node parseDictEntryEndTypeCode(final Node node, final int idx) {
    if (node.type != Type.DICT_ENTRY) {
      final String msg = "Error at position %d."
              + " Missing opening curly bracket of dict-entry.";
      throw new SignatureException(String.format(msg, idx));
    }
    if (node.children == null || node.children.size() != 2) {
      final String msg = "Error at position %d."
              + " Dict-entry must consists of two single complete types.";
      throw new SignatureException(String.format(msg, idx));
    }
    // Maybe: Move checks to parseTypeCode method
    final Node keyNode = node.children.get(0);
    if (keyNode.type == Type.ARRAY
            || keyNode.type == Type.DICT_ENTRY
            || keyNode.type == Type.STRUCT
            || keyNode.type == Type.VARIANT) {
      final String msg = "Error at position %d."
              + " The first single complete type of a dict-entry must be a basic type.";
      throw new SignatureException(String.format(msg, idx));
    }
    if (node.parent != null && node.parent.type == Type.ARRAY) {
      Node refNode = node.parent;
      while (refNode.type == Type.ARRAY) {
        refNode = refNode.parent;
      }
      return refNode;
    } else {
      return node.parent;
    }
  }

  private static Node parseStructStartTypeCode(final Node node, final int idx, final int lastIdx) {
    if (idx == lastIdx) {
      final String msg = "Error at position %d."
              + " Missing closing parenthesis of struct.";
      throw new SignatureException(String.format(msg, idx));
    }
    return node.addChild(Type.STRUCT);
  }

  private static void appendTypeCodeToStringBuilder(final StringBuilder builder, final Node node) {
    if (node.type == null) {
      if (node.children != null) {
        for (Node child : node.children) {
          appendTypeCodeToStringBuilder(builder, child);
        }
      }
    } else {
      if (node.type == Type.ARRAY) {
        appendArrayToStringBuilder(builder, node);
      } else if (node.type == Type.DICT_ENTRY) {
        appendDictEntryToStringBuilder(builder, node);
      } else if (node.type == Type.STRUCT) {
        appendStructToStringBuilder(builder, node);
      } else {
        builder.append(node.type.getCode().getChar());
      }
    }
  }

  private static void appendStructToStringBuilder(final StringBuilder builder, final Node node) {
    builder.append(TypeCode.STRUCT_START.getChar());
    if (node.children != null) {
      for (Node child : node.children) {
        appendTypeCodeToStringBuilder(builder, child);
      }
    }
    builder.append(TypeCode.STRUCT_END.getChar());
  }

  private static void appendDictEntryToStringBuilder(final StringBuilder builder, final Node node) {
    builder.append(TypeCode.DICT_ENTRY_START.getChar());
    if (node.children != null) {
      for (Node child : node.children) {
        appendTypeCodeToStringBuilder(builder, child);
      }
    }
    builder.append(TypeCode.DICT_ENTRY_END.getChar());
  }

  private static void appendArrayToStringBuilder(final StringBuilder builder, final Node node) {
    builder.append(TypeCode.ARRAY.getChar());
    if (node.children != null) {
      for (Node child : node.children) {
        appendTypeCodeToStringBuilder(builder, child);
      }
    }
  }

  private static Node deepClone(final Node node, final Node newParent) {
    final Node clonedNode = new Node();
    clonedNode.parent = newParent;
    clonedNode.type = node.type;
    if (node.children != null) {
      clonedNode.children = new ArrayList<>();
      for (Node child : node.children) {
        clonedNode.children.add(deepClone(child, clonedNode));
      }
    }
    return clonedNode;
  }

  /**
   * Gets a list of signatures that are a subset of this signature.
   * Each signature describes a single complete type.
   *
   * @return a {@link List} of {@link Signature}s
   */
  public List<Signature> getChildren() {
    return rootNode.children == null ? Collections.emptyList() : rootNode.children.stream()
            .map(child -> deepClone(child, null))
            .map(Signature::new)
            .collect(Collectors.toList());
  }

  /**
   * Gets the number of single complete types of which this signature consists of.
   *
   * @return an {@link Integer}
   */
  public int getQuantity() {
    return rootNode.type != null ? 1 : rootNode.children == null ? 1 : rootNode.children.size();
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    appendTypeCodeToStringBuilder(builder, rootNode);
    return builder.toString();
  }

  @Override
  public Type getType() {
    return Type.SIGNATURE;
  }

  @Override
  public String getDelegate() {
    return toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Signature signature = (Signature) o;
    return Objects.equals(rootNode, signature.rootNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rootNode);
  }

  /**
   * Returns {@code TRUE}, if this signature describes a dictionary and {@code FALSE} otherwise.
   * A dictionary is an array of dict-entries.
   *
   * @return a {@link Boolean}
   */
  public boolean isDictionary() {
    if (rootNode.children == null || rootNode.children.size() != 1) {
      return false;
    }
    return rootNode.type == Type.ARRAY && rootNode.children.get(0).type == Type.DICT_ENTRY;
  }

  /**
   * Returns {@code TRUE}, if this signature describes an array and {@code FALSE} otherwise.
   *
   * @return a {@link Boolean}
   */
  public boolean isArray() {
    if (rootNode.children == null || rootNode.children.size() != 1) {
      return false;
    }
    return rootNode.type == Type.ARRAY && rootNode.children.get(0).type != Type.DICT_ENTRY;
  }

  /**
   * Returns {@code TRUE}, if this signature describes a dictionary-entry and {@code FALSE} otherwise.
   * A dictionary-entry is a key-value pair.
   *
   * @return a {@link Boolean}
   */
  public boolean isDictionaryEntry() {
    return rootNode.type == Type.DICT_ENTRY;
  }

  /**
   * Returns {@code TRUE}, if this signature describes a struct and {@code FALSE} otherwise.
   *
   * @return a {@link Boolean}
   */
  public boolean isStruct() {
    return rootNode.type == Type.STRUCT;
  }

  /**
   * Returns {@code TRUE}, if this signature describes a variant and {@code FALSE} otherwise.
   *
   * @return a {@link Boolean}
   */
  public boolean isVariant() {
    return rootNode.type == Type.VARIANT;
  }

  /**
   * Returns a new signature without the enclosing container description.
   * The signature must describe an array, a dict-entry or a struct type.
   *
   * @return a {@link Signature}
   * @throws IllegalArgumentException If the signature does not describe an array, a dict-entry or a struct type.
   */
  public Signature subContainer() {
    if (rootNode.children == null
            || !(rootNode.type == Type.ARRAY
            || rootNode.type == Type.DICT_ENTRY
            || rootNode.type == Type.STRUCT)) {
      throw new IllegalArgumentException("Signature must describe an array, a dict-entry or a struct type.");
    }
    if (rootNode.children.size() == 1) {
      final Node clonedChild = deepClone(rootNode.children.get(0), null);
      return new Signature(clonedChild);
    } else {
      final Node subRootNode = new Node();
      subRootNode.children = new ArrayList<>();
      for (Node child : rootNode.children) {
        final Node clonedChild = deepClone(child, subRootNode);
        subRootNode.children.add(clonedChild);
      }
      return new Signature(subRootNode);
    }
  }

  /**
   * Returns {@code TRUE}, if this signature describes a container and {@code FALSE} otherwise.
   * A container can be an ARRAY, a DICT-ENTRY, a STRUCT or a VARIANT.
   *
   * @return a {@link Boolean}
   * @throws IllegalArgumentException If this signature consists of more than one single complete type.
   */
  public boolean isContainerType() {
    return rootNode.type != null
            && (rootNode.type == Type.ARRAY
            || rootNode.type == Type.DICT_ENTRY
            || rootNode.type == Type.STRUCT
            || rootNode.type == Type.VARIANT);
  }

  private static final class Node {
    private Node parent = null;
    private Type type = null;
    private List<Node> children = null;

    Node addChild(final Type code) {
      if (children == null) {
        children = new ArrayList<>();
      }
      final Node child = new Node();
      child.parent = this;
      child.type = code;
      children.add(child);
      return child;
    }

    @Override
    public String toString() {
      return "Node";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final Node node = (Node) o;
      return type == node.type
              && Objects.equals(children, node.children);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, children);
    }
  }
}
