package com.lemadane.jax.xml.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.RecordIntrospector;
import com.lemadane.jax.reflect.RecordMetadata;
import com.lemadane.jax.reflect.PojoPropertyReader;

/**
* Serializes Java objects directly and supports writing Jax-owned XML element trees.
*/
public final class XmlObjectWriter {

  static final int MAX_SERIALIZATION_DEPTH = 256;

  XmlObjectWriter() {
  }

  public static String stringify(Object object) {
    if (object == null) {
      throw new NullPointerException("object must not be null");
    }
    if (!isStructured(object)) {
      throw new XmlMappingException("XML root value must be a Java record or POJO: " + object.getClass().getName());
    }
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    StringBuilder output = new StringBuilder();
    XmlDirectWriter.writeObject(object, rootName(object), output, visited, 1,
        Map.of("xml", "http://www.w3.org/XML/1998/namespace"));
    return output.toString();
  }

  static String rootName(Object object) {
    if (object instanceof Record) {
      return RecordIntrospector.inspect(object.getClass()).decodedRecordName();
    }
    return DataNameCodec.decode(object.getClass().getSimpleName());
  }

  static XmlElement toElement(Object object, String elementName, Set<Object> visited) {
    return toElement(object, elementName, visited, 1);
  }

  static XmlElement toElement(Object object, String elementName, Set<Object> visited, int depth) {
    requireDepth(depth);
    if (!visited.add(object)) {
      throw new XmlMappingException("Cycle detected in object graph for " + object.getClass().getName());
    }
    try {
      XmlElement element = new XmlElement(elementName);
      if (object instanceof Record record) {
        appendRecordProperties(record, element, visited, depth);
      } else {
        appendPojoAttributes(object, element);
        appendPojoProperties(object, element, visited, depth);
        XmlTextSupport.append(object, element);
      }
      return element;
    } finally {
      visited.remove(object);
    }
  }

  static void appendPojoAttributes(Object object, XmlElement element) {
    readAttributes(object, element.attributes);
  }

  static Map<String, String> readAttributes(Object object) {
    var fields = XmlObjectReader.attributeFields(object.getClass());
    if (fields.isEmpty()) {
      return Map.of();
    }
    Map<String, String> attributes = new LinkedHashMap<>();
    readAttributes(object, attributes);
    return attributes;
  }

   static void readAttributes(Object object, Map<String, String> attributes) {
    for (Field field : XmlObjectReader.attributeFields(object.getClass())) {
      try {
        if (!field.canAccess(object) && !field.trySetAccessible()) {
          throw new XmlMappingException("Cannot access annotated XML attribute field " + field.getName()
              + " on " + object.getClass().getName());
        }
        Object value = field.get(object);
        if (value instanceof Optional<?> optional) {
          value = optional.orElse(null);
        }
        if (value != null) {
          attributes.put(xmlAttributeName(field), value.toString());
        }
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new XmlMappingException(
            "Failed to access XML attribute field " + field.getName() + " on " + object.getClass().getName(),
            e
        );
      }
    }
  }

  static void appendRecordProperties(Record record, XmlElement parent, Set<Object> visited) {
    appendRecordProperties(record, parent, visited, 1);
  }

  static void appendRecordProperties(Record record, XmlElement parent, Set<Object> visited, int depth) {
    requireDepth(depth);
    RecordMetadata metadata = RecordIntrospector.inspect(record.getClass());
    for (RecordMetadata.ComponentMetadata component : metadata.components()) {
      Object value;
      try {
        value = component.accessor().invoke(record);
      } catch (ReflectiveOperationException | IllegalArgumentException e) {
        throw new XmlMappingException(
            "Failed to access record component " + component.name() + " on " + record.getClass().getName(),
            e
        );
      }
      appendProperty(parent, component.decodedName(), value, visited, depth + 1);
    }
  }

  static void appendPojoProperties(Object object, XmlElement parent, Set<Object> visited) {
    appendPojoProperties(object, parent, visited, 1);
  }

  static void appendPojoProperties(Object object, XmlElement parent, Set<Object> visited, int depth) {
    requireDepth(depth);
    try {
      for (PojoPropertyReader.Property property : PojoPropertyReader.read(object)) {
        if (!isXmlAttribute(object.getClass(), property.name())) {
          appendProperty(parent, DataNameCodec.decode(property.name()), property.value(), visited, depth + 1);
        }
      }
    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
      throw new XmlMappingException("Failed to access POJO properties on " + object.getClass().getName(), e);
    }
  }

   static final ClassValue<Map<String, Boolean>> XML_FIELDS = new ClassValue<>() {
    @Override
    protected Map<String, Boolean> computeValue(Class<?> type) {
      Map<String, Boolean> fields = new HashMap<>();
      for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
        for (Field field : current.getDeclaredFields()) {
          fields.putIfAbsent(field.getName(), field.isAnnotationPresent(XmlAttribute.class) || field.isAnnotationPresent(XmlText.class));
        }
      }
      return Map.copyOf(fields);
    }
  };

  static boolean isXmlAttribute(Class<?> type, String fieldName) {
    return XML_FIELDS.get(type).getOrDefault(fieldName, false);
  }

  static void appendProperty(XmlElement parent, String name, Object value, Set<Object> visited) {
    appendProperty(parent, name, value, visited, 1);
  }

  static void appendProperty(XmlElement parent, String name, Object value, Set<Object> visited, int depth) {
    requireDepth(depth);
    if (value == null) {
      return;
    }
    if (value instanceof Optional<?> optional) {
      if (optional.isPresent()) {
        Object innerValue = optional.get();
        if ((innerValue instanceof Collection<?> || innerValue.getClass().isArray()) && !(innerValue instanceof String)) {
          XmlElement wrapper = new XmlElement(name);
          appendProperty(wrapper, name, innerValue, visited, depth + 1);
          parent.addChild(wrapper);
        } else {
          appendProperty(parent, name, innerValue, visited, depth + 1);
        }
      } else {
        XmlElement nullOptional = new XmlElement(name);
        nullOptional.addAttribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE, "true");
        parent.addChild(nullOptional);
      }
      return;
    }
    if (value instanceof Collection<?> collection) {
      if (!visited.add(collection)) {
        throw new XmlMappingException("Cycle detected in object graph for collection instance");
      }
      try {
        if (collection.isEmpty()) {
          XmlElement emptyList = new XmlElement(name);
          emptyList.addAttribute(XmlObjectReader.EMPTY_LIST_ATTRIBUTE, "true");
          parent.addChild(emptyList);
          return;
        }
        for (Object item : collection) {
          if (item == null) {
            XmlElement nullItem = new XmlElement(name);
            nullItem.addAttribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE, "true");
            parent.addChild(nullItem);
          } else if (item instanceof Collection<?> || item.getClass().isArray()) {
            XmlElement nestedCollection = new XmlElement(name);
            appendProperty(nestedCollection, name, item, visited, depth + 1);
            parent.addChild(nestedCollection);
          } else {
            appendProperty(parent, name, item, visited, depth + 1);
          }
        }
      } finally {
        visited.remove(collection);
      }
      return;
    }
    if (value.getClass().isArray()) {
      if (!visited.add(value)) {
        throw new XmlMappingException("Cycle detected in object graph for array instance");
      }
      try {
        int length = Array.getLength(value);
        if (length == 0) {
          XmlElement emptyList = new XmlElement(name);
          emptyList.addAttribute(XmlObjectReader.EMPTY_LIST_ATTRIBUTE, "true");
          parent.addChild(emptyList);
          return;
        }
        for (int i = 0; i < length; i++) {
          Object item = Array.get(value, i);
          if (item == null) {
            XmlElement nullItem = new XmlElement(name);
            nullItem.addAttribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE, "true");
            parent.addChild(nullItem);
          } else if (item instanceof Collection<?> || item.getClass().isArray()) {
            XmlElement nestedCollection = new XmlElement(name);
            appendProperty(nestedCollection, name, item, visited, depth + 1);
            parent.addChild(nestedCollection);
          } else {
            appendProperty(parent, name, item, visited, depth + 1);
          }
        }
      } finally {
        visited.remove(value);
      }
      return;
    }
    if (isStructured(value)) {
      parent.addChild(toElement(value, name, visited, depth));
    } else {
      XmlElement child = new XmlElement(name);
      child.addText(value instanceof Enum<?> enumValue ? enumValue.name() : value.toString());
      parent.addChild(child);
    }
  }

  static void requireDepth(int depth) {
    if (depth > MAX_SERIALIZATION_DEPTH) {
      throw new XmlMappingException("Maximum XML serialization depth of " + MAX_SERIALIZATION_DEPTH + " exceeded");
    }
  }

  static void writeElement(XmlElement element, StringBuilder output) {
    writeElement(element, output, Map.of("xml", "http://www.w3.org/XML/1998/namespace"));
  }

  static void writeElement(XmlElement element, StringBuilder output, Map<String, String> inheritedNamespaces) {
    Map<String, String> namespaces = writeStart(element.name(), element.attributes(), output, inheritedNamespaces);
    if (element.content().isEmpty()) {
      output.append("/>");
      return;
    }
    output.append('>');
    for (Object item : element.content()) {
      if (item instanceof XmlElement child) {
        writeElement(child, output, namespaces);
      } else {
        output.append(escapeText((String) item));
      }
    }
    output.append("</").append(element.name()).append('>');
  }

  static Map<String, String> writeStart(String name, Map<String, String> attributes,
      StringBuilder output, Map<String, String> inheritedNamespaces) {
    requireValidXmlQName(name);
    Map<String, String> namespaces = inheritedNamespaces;
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String attributeName = attribute.getKey();
      if (attributeName.equals("xmlns") || attributeName.startsWith("xmlns:")) {
        if (namespaces == inheritedNamespaces) {
          namespaces = new HashMap<>(inheritedNamespaces);
        }
        namespaces.put(attributeName.equals("xmlns") ? "" : attributeName.substring("xmlns:".length()), attribute.getValue());
      }
    }
    requireBoundPrefix(name, namespaces);
    for (String attributeName : attributes.keySet()) {
      requireValidXmlQName(attributeName);
      if (!attributeName.equals("xmlns") && !attributeName.startsWith("xmlns:")) {
        requireBoundPrefix(attributeName, namespaces);
      }
    }
    output.append('<').append(name);
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      output.append(' ').append(attribute.getKey()).append("=\"")
          .append(escapeAttribute(attribute.getValue())).append('"');
    }
    return namespaces;
  }

  static void requireBoundPrefix(String name, Map<String, String> namespaces) {
    int colon = name.indexOf(':');
    if (colon > 0) {
      String prefix = name.substring(0, colon);
      if (!namespaces.containsKey(prefix)) {
        throw new XmlMappingException(
            "Cannot serialize XML name " + name + " without a namespace declaration for prefix " + prefix
        );
      }
    }
  }

  static void requireValidXmlQName(String name) {
    int colon = name.indexOf(':');
    if (colon != name.lastIndexOf(':')
        || colon == 0
        || colon == name.length() - 1
        || !(colon < 0
            ? isValidXmlNamePart(name)
            : isValidXmlNamePart(name.substring(0, colon)) && isValidXmlNamePart(name.substring(colon + 1)))) {
      throw new XmlMappingException("Invalid XML name: " + name);
    }
  }

  static boolean isValidXmlNamePart(String name) {
    if (name.isEmpty()) {
      return false;
    }
    int offset = 0;
    int first = name.codePointAt(offset);
    if (!isXmlNameStartCharacter(first)) {
      return false;
    }
    offset += Character.charCount(first);
    while (offset < name.length()) {
      int codePoint = name.codePointAt(offset);
      if (!isXmlNameCharacter(codePoint)) {
        return false;
      }
      offset += Character.charCount(codePoint);
    }
    return true;
  }

  static boolean isXmlNameStartCharacter(int codePoint) {
    return codePoint == '_'
        || codePoint >= 'A' && codePoint <= 'Z'
        || codePoint >= 'a' && codePoint <= 'z'
        || codePoint >= 0xC0 && codePoint <= 0xD6
        || codePoint >= 0xD8 && codePoint <= 0xF6
        || codePoint >= 0xF8 && codePoint <= 0x2FF
        || codePoint >= 0x370 && codePoint <= 0x37D
        || codePoint >= 0x37F && codePoint <= 0x1FFF
        || codePoint >= 0x200C && codePoint <= 0x200D
        || codePoint >= 0x2070 && codePoint <= 0x218F
        || codePoint >= 0x2C00 && codePoint <= 0x2FEF
        || codePoint >= 0x3001 && codePoint <= 0xD7FF
        || codePoint >= 0xF900 && codePoint <= 0xFDCF
        || codePoint >= 0xFDF0 && codePoint <= 0xFFFD
        || codePoint >= 0x10000 && codePoint <= 0xEFFFF;
  }

  static boolean isXmlNameCharacter(int codePoint) {
    return isXmlNameStartCharacter(codePoint)
        || codePoint == '-'
        || codePoint == '.'
        || codePoint >= '0' && codePoint <= '9'
        || codePoint == 0xB7
        || codePoint >= 0x0300 && codePoint <= 0x036F
        || codePoint >= 0x203F && codePoint <= 0x2040;
  }

  static String xmlAttributeName(AnnotatedElement member) {
    XmlAttribute annotation = member.getAnnotation(XmlAttribute.class);
    if (annotation.value().isEmpty() && member instanceof Field field) {
      return DataNameCodec.decode(field.getName());
    }
    if (annotation.value().isEmpty()) {
      throw new XmlMappingException("@XmlAttribute is only supported on fields for XML serialization");
    }
    return annotation.value();
  }

  static boolean isStructured(Object value) {
    if (value instanceof Record) {
      return true;
    }
    Class<?> type = value.getClass();
    return !type.isPrimitive()
        && !type.isEnum()
        && !type.getPackageName().startsWith("java.")
        && !type.isArray()
        && !Collection.class.isAssignableFrom(type);
  }

  static String escapeText(String input) {
    return escapeXml(input, false);
  }

  static String escapeAttribute(String input) {
    return escapeXml(input, true);
  }

  static String escapeXml(String input, boolean attribute) {
    StringBuilder escaped = null;
    for (int offset = 0; offset < input.length();) {
      int codePoint = input.codePointAt(offset);
      if (!isValidXmlCharacter(codePoint)) {
        throw new XmlMappingException("XML 1.0 does not permit character U+"
            + String.format("%04X", codePoint));
      }
      String replacement = switch (codePoint) {
        case '&' -> "&amp;";
        case '<' -> "&lt;";
        case '>' -> "&gt;";
        case '"' -> attribute ? "&quot;" : null;
        case '\t' -> attribute ? "&#x9;" : null;
        case '\n' -> attribute ? "&#xA;" : null;
        case '\r' -> "&#xD;";
        default -> null;
      };
      if (replacement != null) {
        if (escaped == null) {
          escaped = new StringBuilder(input.length());
          escaped.append(input, 0, offset);
        }
        escaped.append(replacement);
      } else if (escaped != null) {
        escaped.appendCodePoint(codePoint);
      }
      offset += Character.charCount(codePoint);
    }
    return escaped == null ? input : escaped.toString();
  }

  static boolean isValidXmlCharacter(int codePoint) {
    return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
        || codePoint >= 0x20 && codePoint <= 0xD7FF
        || codePoint >= 0xE000 && codePoint <= 0xFFFD
        || codePoint >= 0x10000 && codePoint <= 0x10FFFF;
  }
}
