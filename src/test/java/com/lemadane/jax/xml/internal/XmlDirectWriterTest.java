package com.lemadane.jax.xml.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class XmlDirectWriterTest {
  record Envelope(Object value) {}
  record Broken(String value) {
    @Override
    public String value() {
      throw new IllegalArgumentException("broken accessor");
    }
  }

  public static class log$colon$Root {
    @XmlAttribute public String xmlns$colon$log = "urn:root";
    @XmlAttribute public String quoted = "\"\t\n\r&<>";
    public Object log$colon$item;
  }

  public static class TextValue {
    @XmlAttribute public Optional<String> unit = Optional.of("kg");
    @XmlText public String text = "&<>\r😀";
  }

  public static class Rebound {
    @XmlAttribute public String xmlns$colon$log = "urn:child";
    public String log$colon$value = "nested";
  }

  public static class BrokenGetter {
     String value;
    public String getValue() {
      throw new IllegalArgumentException("broken getter");
    }
  }

  static String treeWrite(Object value) {
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    XmlElement tree = XmlObjectWriter.toElement(value, XmlObjectWriter.rootName(value), visited);
    StringBuilder output = new StringBuilder();
    XmlObjectWriter.writeElement(tree, output);
    return output.toString();
  }

  @Test
  void matchesTreeForNamespacesAttributesTextAndRepeatedElements() {
    log$colon$Root root = new log$colon$Root();
    root.log$colon$item = List.of(new Rebound(), new TextValue(), "sibling");
    assertThat(Jax.toXml(root)).isEqualTo(treeWrite(root))
        .contains("xmlns:log=\"urn:child\"", "<log:item>sibling</log:item>");
  }

  @Test
  void preservesEmptyAndNullMarkersAndNestedSequenceWrappers() {
    Object value = new Envelope(Arrays.asList(null, Optional.empty(), Optional.of(List.of("a", "b")),
        List.of(), new Object[0], new int[] {1, 2}, new Object[] {null, List.of(), new String[0]},
        List.of(List.of("nested"))));
    assertThat(Jax.toXml(value)).isEqualTo(treeWrite(value));
    assertThat(Jax.toXml(new Envelope(null))).isEqualTo("<Envelope/>");
    TextValue text = new TextValue();
    text.text = "";
    assertThat(Jax.toXml(text)).isEqualTo("<TextValue unit=\"kg\"></TextValue>");
    text.text = null;
    text.unit = Optional.empty();
    assertThat(Jax.toXml(text)).isEqualTo("<TextValue/>");
  }

  @Test
  void rejectsFailingAccessorsAndUnboundPrefixes() {
    assertThatThrownBy(() -> Jax.toXml(new Broken("bad"))).isInstanceOf(XmlMappingException.class).hasMessageContaining("record component");
    assertThatThrownBy(() -> Jax.toXml(new BrokenGetter())).isInstanceOf(XmlMappingException.class).hasMessageContaining("POJO properties");
    assertThatThrownBy(() -> treeWrite(new Broken("bad"))).isInstanceOf(XmlMappingException.class).hasMessageContaining("record component");
    assertThatThrownBy(() -> treeWrite(new BrokenGetter())).isInstanceOf(XmlMappingException.class).hasMessageContaining("POJO properties");
    log$colon$Root root = new log$colon$Root();
    root.xmlns$colon$log = null;
    assertThatThrownBy(() -> Jax.toXml(root)).isInstanceOf(XmlMappingException.class).hasMessageContaining("namespace declaration");
  }
}
