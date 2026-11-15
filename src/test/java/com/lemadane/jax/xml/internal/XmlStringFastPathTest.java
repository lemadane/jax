package com.lemadane.jax.xml.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlStringFastPathTest {
  @Test
  void preservesPlainUnicodeAndTextWhitespaceWithoutCopying() {
    for (String value : List.of("", "plain", "Industriestraße", "粤B-8921X", "cargo 🚢")) {
      assertThat(XmlObjectWriter.escapeXml(value, false)).isSameAs(value);
      assertThat(XmlObjectWriter.escapeXml(value, true)).isSameAs(value);
    }
    String text = "\t\"quoted\"\n";
    assertThat(XmlObjectWriter.escapeXml(text, false)).isSameAs(text);
    assertThat(XmlObjectWriter.escapeXml(text, true)).isEqualTo("&#x9;&quot;quoted&quot;&#xA;");
  }

  @Test
  void escapesAtEveryPositionWithoutLosingUnicodeOrPrefixText() {
    for (String prefix : List.of("", "cargo", "🚢é")) {
      for (String suffix : List.of("", "plain", "🚢")) {
        assertThat(XmlObjectWriter.escapeXml(prefix + "&<>\"\t\n\r" + suffix, true))
            .isEqualTo(prefix + "&amp;&lt;&gt;&quot;&#x9;&#xA;&#xD;" + suffix);
        assertThat(XmlObjectWriter.escapeXml(prefix + "&<>\"\t\n\r" + suffix, false))
            .isEqualTo(prefix + "&amp;&lt;&gt;\"\t\n&#xD;" + suffix);
      }
    }
  }

  @Test
  void validatesTheEntireInputBeforeAndAfterTheFirstEscape() {
    for (char invalid : new char[] {0, 1, 0xD800, 0xDC00, 0xFFFE, 0xFFFF}) {
      for (String prefix : List.of("", "plain", "already & escaped")) {
        for (boolean attribute : List.of(false, true)) {
          assertThatThrownBy(() -> XmlObjectWriter.escapeXml(prefix + invalid + "tail", attribute))
              .isInstanceOf(XmlMappingException.class)
              .hasMessageContaining(String.format("U+%04X", (int) invalid));
        }
      }
    }
  }
}
