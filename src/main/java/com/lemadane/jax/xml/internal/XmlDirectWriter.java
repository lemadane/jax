package com.lemadane.jax.xml.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.PojoPropertyReader;
import com.lemadane.jax.reflect.RecordIntrospector;

/** Emits XML without retaining an intermediate element tree. */
final class XmlDirectWriter {
   XmlDirectWriter() {
  }

  static void writeObject(Object value, String name, StringBuilder output, Set<Object> visited,
      int depth, Map<String, String> inheritedNamespaces) {
    XmlObjectWriter.requireDepth(depth);
    enter(value, visited, value.getClass().getName());
    try {
      Map<String, String> attributes = value instanceof Record ? Map.of() : XmlObjectWriter.readAttributes(value);
      Map<String, String> namespaces = XmlObjectWriter.writeStart(name, attributes, output, inheritedNamespaces);
      output.append('>');
      int contentStart = output.length();
      String text = null;
      if (value instanceof Record record) {
        for (var component : RecordIntrospector.inspect(record.getClass()).components()) {
          Object property;
          try {
            property = component.accessor().invoke(record);
          } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new XmlMappingException("Failed to access record component " + component.name()
                + " on " + record.getClass().getName(), e);
          }
          writeProperty(component.decodedName(), property, output, visited, depth + 1, namespaces);
        }
      } else {
        try {
          for (var property : PojoPropertyReader.read(value)) {
            if (!XmlObjectWriter.isXmlAttribute(value.getClass(), property.name())) {
              writeProperty(DataNameCodec.decode(property.name()), property.value(), output, visited, depth + 1, namespaces);
            }
          }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
          throw new XmlMappingException("Failed to access POJO properties on " + value.getClass().getName(), e);
        }
        text = XmlTextSupport.read(value, output.length() != contentStart);
        if (text != null) {
          output.append(XmlObjectWriter.escapeText(text));
        }
      }
      if (output.length() == contentStart && text == null) {
        output.setLength(contentStart - 1);
        output.append("/>");
      } else {
        close(name, output);
      }
    } finally {
      visited.remove(value);
    }
  }

   static void writeProperty(String name, Object value, StringBuilder output, Set<Object> visited,
      int depth, Map<String, String> namespaces) {
    XmlObjectWriter.requireDepth(depth);
    if (value == null) {
      return;
    }
    if (value instanceof Optional<?> optional) {
      if (optional.isEmpty()) {
        marker(name, XmlObjectReader.NULL_VALUE_ATTRIBUTE, output, namespaces);
      } else {
        Object inner = optional.get();
        if (isSequence(inner)) {
          wrapper(name, inner, output, visited, depth + 1, namespaces);
        } else {
          writeProperty(name, inner, output, visited, depth + 1, namespaces);
        }
      }
    } else if (isSequence(value)) {
      enter(value, visited, value instanceof Collection<?> ? "collection instance" : "array instance");
      try {
        if (value instanceof Collection<?> collection) {
          if (collection.isEmpty()) {
            marker(name, XmlObjectReader.EMPTY_LIST_ATTRIBUTE, output, namespaces);
          } else {
            for (Object item : collection) {
              writeItem(name, item, output, visited, depth + 1, namespaces);
            }
          }
        } else {
          int length = Array.getLength(value);
          if (length == 0) {
            marker(name, XmlObjectReader.EMPTY_LIST_ATTRIBUTE, output, namespaces);
          } else {
            for (int i = 0; i < length; i++) {
              writeItem(name, Array.get(value, i), output, visited, depth + 1, namespaces);
            }
          }
        }
      } finally {
        visited.remove(value);
      }
    } else if (XmlObjectWriter.isStructured(value)) {
      writeObject(value, name, output, visited, depth, namespaces);
    } else {
      XmlObjectWriter.writeStart(name, Map.of(), output, namespaces);
      output.append('>').append(XmlObjectWriter.escapeText(value instanceof Enum<?> e ? e.name() : value.toString()));
      close(name, output);
    }
  }

   static void writeItem(String name, Object item, StringBuilder output, Set<Object> visited,
      int depth, Map<String, String> namespaces) {
    if (item == null) {
      marker(name, XmlObjectReader.NULL_VALUE_ATTRIBUTE, output, namespaces);
    } else if (isSequence(item)) {
      wrapper(name, item, output, visited, depth, namespaces);
    } else {
      writeProperty(name, item, output, visited, depth, namespaces);
    }
  }

   static void wrapper(String name, Object value, StringBuilder output, Set<Object> visited,
      int depth, Map<String, String> namespaces) {
    XmlObjectWriter.writeStart(name, Map.of(), output, namespaces);
    output.append('>');
    writeProperty(name, value, output, visited, depth, namespaces);
    close(name, output);
  }

   static void marker(String name, String attribute, StringBuilder output, Map<String, String> namespaces) {
    XmlObjectWriter.writeStart(name, Map.of(attribute, "true"), output, namespaces);
    output.append("/>");
  }

   static boolean isSequence(Object value) {
    return value instanceof Collection<?> || value.getClass().isArray();
  }

   static void enter(Object value, Set<Object> visited, String kind) {
    if (!visited.add(value)) {
      throw new XmlMappingException("Cycle detected in object graph for " + kind);
    }
  }

   static void close(String name, StringBuilder output) {
    output.append("</").append(name).append('>');
  }
}
