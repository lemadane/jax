package com.lemadane.jax.xml.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.lemadane.jax.convert.DefaultValueProvider;
import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.MissingDataException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.RecordFactory;
import com.lemadane.jax.reflect.RecordIntrospector;
import com.lemadane.jax.reflect.RecordMetadata;

/**
* Internal XML reader that parses XML into Java records with strict or partial mode.
*/
public final class XmlRecordReader {

  static final int MAX_NESTING_DEPTH = 256;

  XmlRecordReader() {
  }

  public static <T extends Record> T parse(Class<T> recordType, String xml, boolean strict) {
    if (recordType == null) {
      throw new NullPointerException("recordType must not be null");
    }
    if (xml == null) {
      throw new NullPointerException("xml must not be null");
    }

    RecordMetadata metadata = RecordIntrospector.inspect(recordType);
    XmlElement root = parseXmlDocument(xml);
    String rootName = root.name();
    String decodedRootName = DataNameCodec.decode(rootName);
    if (!decodedRootName.equals(metadata.decodedRecordName())
        && !rootName.equals(metadata.recordSimpleName())
        && !decodedRootName.equals(metadata.recordSimpleName())) {
      throw new XmlMappingException(
          "Root XML element <" + rootName + "> does not match requested record type "
              + metadata.recordType().getName() + " (expected <" + metadata.recordSimpleName() + ">)"
      );
    }
    return readRecord(metadata, root, "/" + rootName, strict);
  }

  static XmlElement parseXmlDocument(String xml) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
    factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
      throw new XMLStreamException("External XML entities are disabled");
    });

    try {
      XMLStreamReader reader = factory.createXMLStreamReader(new java.io.StringReader(xml));
      try {
        ArrayDeque<XmlElement> stack = new ArrayDeque<>();
        XmlElement root = null;
        while (reader.hasNext()) {
          int event = reader.next();
          if (event == XMLStreamConstants.DTD) {
            throw new XMLStreamException("DOCTYPE declarations are disallowed");
          }
          if (event == XMLStreamConstants.START_ELEMENT) {
            if (stack.size() >= MAX_NESTING_DEPTH) {
              throw new XMLStreamException("Maximum XML nesting depth of " + MAX_NESTING_DEPTH + " exceeded");
            }
            XmlElement element = new XmlElement(qualifiedName(reader.getPrefix(), reader.getLocalName()));
            for (int i = 0; i < reader.getAttributeCount(); i++) {
              element.addAttribute(
                  qualifiedName(reader.getAttributePrefix(i), reader.getAttributeLocalName(i)),
                  reader.getAttributeValue(i)
              );
            }
            for (int i = 0; i < reader.getNamespaceCount(); i++) {
              String prefix = reader.getNamespacePrefix(i);
              element.addAttribute(prefix == null || prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix,
                  reader.getNamespaceURI(i));
            }
            if (stack.isEmpty()) {
              if (root != null) {
                throw new XMLStreamException("Multiple document elements");
              }
              root = element;
            } else {
              stack.peek().addChild(element);
            }
            stack.push(element);
          } else if (event == XMLStreamConstants.END_ELEMENT) {
            if (stack.isEmpty()) {
              throw new XMLStreamException("Unexpected end element");
            }
            stack.pop();
          } else if (event == XMLStreamConstants.CHARACTERS
              || event == XMLStreamConstants.CDATA
              || event == XMLStreamConstants.SPACE
              || event == XMLStreamConstants.ENTITY_REFERENCE) {
            if (!stack.isEmpty()) {
              stack.peek().addText(reader.getText());
            }
          }
        }
        if (root == null || !stack.isEmpty()) {
          throw new XMLStreamException("XML document has no complete root element");
        }
        return root;
      } finally {
        reader.close();
      }
    } catch (XMLStreamException | IllegalArgumentException e) {
      throw new XmlMappingException("Failed to parse XML document: " + e.getMessage(), e);
    }
  }

  static String qualifiedName(String prefix, String localName) {
    return prefix == null || prefix.isEmpty() ? localName : prefix + ":" + localName;
  }

  static <T extends Record> T readRecord(
      RecordMetadata metadata,
      XmlElement element,
      String path,
      boolean strict
  ) {
    Map<String, List<XmlElement>> childrenByName = new HashMap<>();
    if (strict && metadata.components().isEmpty() && element.hasNonWhitespaceText()) {
      throw new XmlMappingException("Mixed content cannot be mapped safely at path " + path);
    }
    for (XmlElement child : element.children()) {
      String name = child.name();
      childrenByName.computeIfAbsent(DataNameCodec.decode(name), ignored -> new ArrayList<>()).add(child);
    }

    if (strict) {
      Set<String> declaredNames = new HashSet<>();
      for (RecordMetadata.ComponentMetadata component : metadata.components()) {
        declaredNames.add(component.decodedName());
        declaredNames.add(component.name());
      }
      for (XmlElement child : element.children()) {
        String name = child.name();
        String decodedName = DataNameCodec.decode(name);
        if (!declaredNames.contains(decodedName) && !declaredNames.contains(name)) {
          throw new ExcessDataException(
              "Excess XML element <" + name + "> at " + path
                  + ". No matching record component exists in " + metadata.recordType().getName()
          );
        }
      }
    }

    Object[] arguments = new Object[metadata.components().size()];
    for (int i = 0; i < metadata.components().size(); i++) {
      RecordMetadata.ComponentMetadata component = metadata.components().get(i);
      String componentPath = path + "/" + component.name();
      List<XmlElement> matches = childrenByName.get(component.decodedName());
      if (matches == null || matches.isEmpty()) {
        matches = childrenByName.get(component.name());
      }

      if (matches == null || matches.isEmpty()) {
        if (strict) {
          throw new MissingDataException(
              "Missing XML element <" + component.decodedName() + "> required by "
                  + metadata.recordType().getName() + "." + component.name() + " at " + path
          );
        }
        arguments[i] = DefaultValueProvider.getDefaultValue(component);
      } else if (component.isList()) {
        if (matches.size() == 1 && XmlObjectReader.isEmptyCollectionMarker(matches.get(0))) {
          XmlObjectReader.validateMarkerAttributes(matches.get(0), componentPath);
          arguments[i] = List.of();
          continue;
        }
        List<Object> values = new ArrayList<>();
        java.lang.reflect.Type elementGenericType = component.listElementGenericType();
        Class<?> elementType = XmlObjectReader.rawType(elementGenericType);
        for (XmlElement match : matches) {
          values.add(readValue(
              elementType,
              elementGenericType,
              match,
              componentPath,
              strict
          ));
        }
        arguments[i] = java.util.Collections.unmodifiableList(values);
      } else if (component.type().isArray()) {
        arguments[i] = XmlObjectReader.readArray(
            component.type().getComponentType(),
            matches,
            componentPath,
            strict
        );
      } else {
        if (matches.size() > 1) {
          throw new XmlMappingException(
              "Multiple XML elements <" + component.decodedName() + "> mapped to non-List component "
                  + metadata.recordType().getName() + "." + component.name() + " at " + path
          );
        }
        arguments[i] = readValue(
            component.type(),
            component.genericType(),
            matches.get(0),
            componentPath,
            strict
        );
      }
    }
    return RecordFactory.createRecord(metadata, arguments);
  }

  static Object readValue(Class<?> targetType, java.lang.reflect.Type genericType, XmlElement element, String path, boolean strict) {
    String elementName = element.name();
    XmlObjectReader.validateMarkerAttributes(element, path);
    
    // Handle Optional first so that NULL_VALUE_ATTRIBUTE is properly wrapped in Optional.empty()
    if (Optional.class.isAssignableFrom(targetType)) {
      java.lang.reflect.Type valueGenericType = new RecordMetadata.ComponentMetadata(
          "optional", "optional", targetType, genericType, null
      ).optionalValueGenericType();
      Class<?> valueType = XmlObjectReader.rawType(valueGenericType);
      if (element.hasAttribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE)
          && "true".equals(element.attribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE))) {
        return Optional.empty();
      }
      if (XmlObjectReader.isEmptyCollectionMarker(element)) {
        return Optional.of(XmlObjectReader.readValue(valueType, valueGenericType, element, path, strict));
      }
      if (element.isEmpty()) {
        if (valueType == String.class) {
          return Optional.of("");
        }
        if (List.class.isAssignableFrom(valueType) || valueType.isArray()) {
          return Optional.empty();
        }
        if (valueType.isRecord()) {
          return Optional.empty();
        }
        return Optional.empty();
      }
      return Optional.ofNullable(XmlObjectReader.readValue(valueType, valueGenericType, element, path, strict));
    }
    
    if (element.hasAttribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE)
        && "true".equals(element.attribute(XmlObjectReader.NULL_VALUE_ATTRIBUTE))) {
      if (targetType.isPrimitive()) {
        throw new TypeConversionException(
            "Cannot map null XML collection item to primitive type " + targetType.getName() + " at " + path
        );
      }
      return null;
    }
    if (XmlObjectReader.isEmptyCollectionMarker(element)
        && !List.class.isAssignableFrom(targetType) && !targetType.isArray()) {
      throw new TypeConversionException("Empty-list marker does not match " + targetType.getName() + " at " + path);
    }
    if (targetType.isRecord()) {
      return readRecord(
          RecordIntrospector.inspect(targetType.asSubclass(Record.class)),
          element,
          path,
          strict
      );
    }
    if (List.class.isAssignableFrom(targetType)) {
      return XmlObjectReader.readList(genericType, element.children(), path, strict);
    }
    if (targetType.isArray()) {
      return XmlObjectReader.readArray(targetType.getComponentType(), element.children(), path, strict);
    }
    if (element.isEmpty()) {
      if (targetType == String.class) {
        return "";
      }
      if (strict || targetType.isPrimitive()) {
        String message = targetType.isPrimitive()
            ? "Empty XML element <" + elementName + "> cannot be mapped to primitive type "
                + targetType.getName() + " at " + path
            : "Empty XML element <" + elementName + "> cannot be mapped to type "
                + targetType.getName() + " at " + path;
        throw new TypeConversionException(message);
      }
      return null;
    }
    return TypeConverter.convert(targetType, element.textContent(), path);
  }

}
