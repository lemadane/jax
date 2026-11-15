package com.lemadane.jax.xml.internal;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class XmlObjectReaderHelperTest {

  public static class XmlOptional {
    public Optional<String> value;
  }

  public static class ArrayValues {
    public int[] value;
  }

  @DisplayName("Maps repeated XML elements to Java arrays")
  @Test
  void mapsRepeatedXmlElementsToArrays() {
    ArrayValues values = com.lemadane.jax.Jax.toObject(
        "<ArrayValues><value>3</value><value>5</value></ArrayValues>",
        ArrayValues.class
    );

    assertThat(values.value).containsExactly(3, 5);
  }

  @DisplayName("Recognizes empty optional XML elements")
  @Test
  void recognizesEmptyOptionalElements() {
    // Empty element without jax-null marker is Optional.of("") for String types
    XmlOptional emptyString = com.lemadane.jax.Jax.toObject(
        "<XmlOptional><value/></XmlOptional>",
        XmlOptional.class
    );
    // Empty element with jax-null="true" is Optional.empty()
    XmlOptional empty = com.lemadane.jax.Jax.toObject(
        "<XmlOptional><value jax-null=\"true\"/></XmlOptional>",
        XmlOptional.class
    );
    XmlOptional present = com.lemadane.jax.Jax.toObject(
        "<XmlOptional><value>mapped</value></XmlOptional>",
        XmlOptional.class
    );

    assertThat(emptyString.value).contains("");
    assertThat(empty.value).isEmpty();
    assertThat(present.value).contains("mapped");
    assertThat(XmlObjectReader.isEmptyElement(new XmlElement("empty"))).isTrue();
  }

  @DisplayName("Reads primitive arrays from parsed XML elements")
  @Test
  void readsPrimitiveArraysFromParsedXmlElements() {
    XmlElement root = XmlRecordReader.parseXmlDocument(
        "<root><item>2</item><item>4</item><item>8</item></root>"
    );

    int[] values = (int[]) XmlObjectReader.readArray(
        int.class,
        root.children(),
        "/root/item",
        true
    );

    assertThat(values).containsExactly(2, 4, 8);
    assertThat(root.children()).hasSize(3);
  }
}
