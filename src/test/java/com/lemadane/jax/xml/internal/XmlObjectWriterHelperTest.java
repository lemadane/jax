package com.lemadane.jax.xml.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class XmlObjectWriterHelperTest {

  @DisplayName("Writes XML element trees with the built-in XML namespace")
  @Test
  void writesElementTreesUsingTheBuiltInXmlNamespace() {
    XmlElement root = new XmlElement("root");
    XmlElement language = new XmlElement("xml:lang");
    language.addText("en");
    root.addChild(language);

    StringBuilder output = new StringBuilder();
    XmlObjectWriter.writeElement(root, output);

    assertThat(output).hasToString("<root><xml:lang>en</xml:lang></root>");
  }
}
