package com.lemadane.jax.xml.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class XmlElement {

  final String name;
  final Map<String, String> attributes = new LinkedHashMap<>();
  final List<XmlElement> children = new ArrayList<>();
  final List<Object> content = new ArrayList<>();

  XmlElement(String name) {
    this.name = name;
  }

  String name() {
    return name;
  }

  Map<String, String> attributes() {
    return Collections.unmodifiableMap(attributes);
  }

  List<XmlElement> children() {
    return Collections.unmodifiableList(children);
  }

  List<Object> content() {
    return Collections.unmodifiableList(content);
  }

  boolean hasAttribute(String name) {
    return attributes.containsKey(name);
  }

  String attribute(String name) {
    return attributes.get(name);
  }

  String textContent() {
    StringBuilder text = new StringBuilder();
    for (Object item : content) {
      if (item instanceof String value) {
        text.append(value);
      } else if (item instanceof XmlElement child) {
        text.append(child.textContent());
      }
    }
    return text.toString();
  }

  boolean isEmpty() {
    return children.isEmpty() && textContent().isEmpty();
  }

  boolean hasNonWhitespaceText() {
    for (Object item : content) {
      if (item instanceof String value && !value.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  void addAttribute(String name, String value) {
    attributes.put(name, value);
  }

  void addChild(XmlElement child) {
    children.add(child);
    content.add(child);
  }

  void addText(String text) {
    content.add(text);
  }
}
