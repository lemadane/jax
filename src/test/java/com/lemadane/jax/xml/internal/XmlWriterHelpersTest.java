package com.lemadane.jax.xml.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class XmlWriterHelpersTest {
  public static class Attributes {
    @XmlAttribute
    Optional<String> id = Optional.of("42");
    @XmlAttribute
    Optional<String> absent = Optional.empty();
  }

  static Set<Object> visited() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
  }

  static String write(XmlElement element) {
    StringBuilder output = new StringBuilder();
    XmlObjectWriter.writeElement(element, output);
    return output.toString();
  }

  @Test
  void serializesEmptyNestedAndNullArrayItems() {
    XmlElement root = new XmlElement("root");
    Set<Object> seen = visited();
    XmlObjectWriter.appendProperty(root, "omitted", null, seen);
    XmlObjectWriter.appendProperty(root, "empty", new int[0], seen);
    XmlObjectWriter.appendProperty(root, "item", new Object[] {null, new int[] {1, 2}}, seen);
    assertThat(write(root)).isEqualTo("<root><empty jax-empty-list=\"true\"/>"
        + "<item jax-null=\"true\"/><item><item>1</item><item>2</item></item></root>");
    assertThat(seen).isEmpty();
  }

  @Test
  void rejectsCyclesAndCleansTraversalStateAfterFailure() {
    List<Object> cycle = new ArrayList<>();
    cycle.add(cycle);
    Object[] array = new Object[1];
    array[0] = array;
    for (Object value : List.of(cycle, array)) {
      Set<Object> seen = visited();
      assertThatThrownBy(() -> XmlObjectWriter.appendProperty(new XmlElement("root"), "item", value, seen))
          .isInstanceOf(XmlMappingException.class).hasMessageContaining("Cycle");
      assertThat(seen).isEmpty();
    }
  }

  @Test
  void readsPrivateOptionalAttributesAndOmitsAbsentValues() {
    XmlElement root = new XmlElement("root");
    XmlObjectWriter.appendPojoAttributes(new Attributes(), root);
    assertThat(root.attributes()).containsExactly(Map.entry("id", "42"));
    assertThat(XmlObjectWriter.isXmlAttribute(Attributes.class, "id")).isTrue();
    assertThat(XmlObjectWriter.isXmlAttribute(Attributes.class, "unknown")).isFalse();
  }

  @Test
  void escapesTextAndAttributeWhitespaceWithoutChangingContent() {
    assertThat(XmlObjectWriter.escapeText("<&>\"\t\n\r😀")).isEqualTo("&lt;&amp;&gt;\"\t\n&#xD;😀");
    assertThat(XmlObjectWriter.escapeAttribute("<&>\"\t\n\r😀"))
        .isEqualTo("&lt;&amp;&gt;&quot;&#x9;&#xA;&#xD;😀");
    for (int point : new int[] {0, 8, 11, 12, 0xD800, 0xFFFF, 0x110000}) {
      assertThat(XmlObjectWriter.isValidXmlCharacter(point)).isFalse();
    }
    for (int point : new int[] {9, 10, 13, 32, 0xD7FF, 0xE000, 0xFFFD, 0x10000, 0x10FFFF}) {
      assertThat(XmlObjectWriter.isValidXmlCharacter(point)).isTrue();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", ":x", "x:", "a:b:c", "1bad", "bad name", "x:1bad"})
  void rejectsInvalidQualifiedNames(String name) {
    assertThatThrownBy(() -> XmlObjectWriter.requireValidXmlQName(name)).isInstanceOf(XmlMappingException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "_name", "a:b", "éclair", "a-b.c", "名", "a·b"})
  void acceptsValidQualifiedNames(String name) {
    assertThatCode(() -> XmlObjectWriter.requireValidXmlQName(name)).doesNotThrowAnyException();
  }

  @Test
  void scopesNamespaceDeclarationsToTheirElements() {
    XmlElement root = new XmlElement("root");
    root.addAttribute("xmlns", "urn:default");
    XmlElement child = new XmlElement("p:child");
    child.addAttribute("xmlns:p", "urn:child");
    root.addChild(child);
    assertThat(write(root)).contains("xmlns=\"urn:default\"", "<p:child xmlns:p=\"urn:child\"/>");
    root.addChild(new XmlElement("p:sibling"));
    assertThatThrownBy(() -> write(root)).isInstanceOf(XmlMappingException.class).hasMessageContaining("prefix p");
  }
}
