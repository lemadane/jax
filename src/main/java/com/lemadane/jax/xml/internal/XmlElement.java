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

   Map<String, String> attributesView;
   List<XmlElement> childrenView;
   List<Object> contentView;

  XmlElement(String name) {
    this.name = name;
  }

  String name() {
    return name;
  }

  Map<String, String> attributes() {
    if (attributesView == null) {
      attributesView = Collections.unmodifiableMap(attributes);
    }
    return attributesView;
  }

  List<XmlElement> children() {
    if (childrenView == null) {
      childrenView = Collections.unmodifiableList(children);
    }
    return childrenView;
  }

  List<Object> content() {
    if (contentView == null) {
      contentView = Collections.unmodifiableList(content);
    }
    return contentView;
  }

  boolean hasAttribute(String name) {
    return attributes.containsKey(name);
  }

  String attribute(String name) {
    return attributes.get(name);
  }

  String textContent() {
    if (content.isEmpty()) {
      return "";
    }
    if (content.size() == 1 && content.get(0) instanceof String text) {
      return text;
    }
    StringBuilder text = new StringBuilder();
    appendTextTo(text);
    return text.toString();
  }

   void appendTextTo(StringBuilder text) {
    for (Object item : content) {
      if (item instanceof String value) {
        text.append(value);
      } else if (item instanceof XmlElement child) {
        child.appendTextTo(text);
      }
    }
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
