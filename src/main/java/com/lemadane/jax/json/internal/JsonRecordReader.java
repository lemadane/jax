package com.lemadane.jax.json.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.convert.DefaultValueProvider;
import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.MissingDataException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.RecordFactory;
import com.lemadane.jax.reflect.RecordIntrospector;
import com.lemadane.jax.reflect.RecordMetadata;

/**
* Internal JSON reader that maps JSON AST nodes into Java records.
*/
public final class JsonRecordReader {

  JsonRecordReader() {
  }

  /**
  * Parses JSON string into a record instance.
  *
  * @param recordType target record class.
  * @param json JSON text.
  * @param strict true for strict mode, false for partial mode.
  * @param <T> record type.
  * @return parsed record instance.
  */
  public static <T extends Record> T parse(Class<T> recordType, String json, boolean strict) {
    Objects.requireNonNull(recordType, "recordType must not be null");
    Objects.requireNonNull(json, "json must not be null");

    RecordMetadata metadata = RecordIntrospector.inspect(recordType);
    JsonParser.JsonNode rootNode = JsonParser.parse(json);

    if (!(rootNode instanceof JsonParser.JsonNode.JsonObjectNode objNode)) {
      throw new JsonMappingException(
          "A JSON object is expected but " + getJsonNodeTypeDescription(rootNode) + " was supplied at $"
      );
    }

    return readRecord(metadata, objNode, "$", strict);
  }

  static <T extends Record> T readRecord(
      RecordMetadata metadata,
      JsonParser.JsonNode.JsonObjectNode objNode,
      String path,
      boolean strict
  ) {
    Map<String, JsonParser.JsonNode> members = objNode.members();

    // 1. Strict mode check: Excess JSON properties
    if (strict) {
      Set<String> declaredComponentNames = new HashSet<>();
      for (RecordMetadata.ComponentMetadata comp : metadata.components()) {
        declaredComponentNames.add(comp.decodedName());
        declaredComponentNames.add(comp.name());
      }

      for (String key : members.keySet()) {
        String decodedKey = DataNameCodec.decode(key);
        if (!declaredComponentNames.contains(decodedKey) && !declaredComponentNames.contains(key)) {
          throw new ExcessDataException(
              "Excess JSON property \"" + key + "\" at " + path + "." + key
                  + ". No matching record component exists in " + metadata.recordType().getName()
          );
        }
      }
    }

    // 2. Map values to components
    Object[] args = new Object[metadata.components().size()];

    for (int i = 0; i < metadata.components().size(); i++) {
      RecordMetadata.ComponentMetadata comp = metadata.components().get(i);
      String propPath = path + "." + comp.name();

      JsonParser.JsonNode valueNode = members.get(comp.decodedName());
      if (valueNode == null) {
        valueNode = members.get(comp.name());
      }

      if (valueNode == null) {
        if (strict) {
          throw new MissingDataException(
              "Missing JSON property \"" + comp.decodedName() + "\" required by "
                  + metadata.recordType().getName() + "." + comp.name() + " at " + path
          );
        } else {
          args[i] = DefaultValueProvider.getDefaultValue(comp);
        }
      } else {
        args[i] = readValue(comp.type(), comp.genericType(), comp, valueNode, propPath, strict);
      }
    }

    return RecordFactory.createRecord(metadata, args);
  }

  static Object readValue(
      Class<?> targetType,
      java.lang.reflect.Type genericType,
      RecordMetadata.ComponentMetadata componentMeta,
      JsonParser.JsonNode node,
      String path,
      boolean strict
  ) {
    if (node instanceof JsonParser.JsonNode.JsonNullNode) {
      if (Optional.class.isAssignableFrom(targetType)) {
        return Optional.empty();
      }
      if (targetType.isPrimitive()) {
        throw new TypeConversionException("Cannot convert null value to primitive type " + targetType.getName() + " at " + path);
      }
      return null;
    }

    if (Optional.class.isAssignableFrom(targetType)) {
      java.lang.reflect.Type valueGenericType = componentMeta != null
          ? componentMeta.optionalValueGenericType()
          : JsonObjectReader.getGenericTypeArgument(genericType);
      Class<?> valueType = JsonObjectReader.rawType(valueGenericType);
      Object val = readValue(valueType, valueGenericType, null, node, path, strict);
      return Optional.ofNullable(val);
    }

    if (List.class.isAssignableFrom(targetType)) {
      if (!(node instanceof JsonParser.JsonNode.JsonArrayNode arrNode)) {
        throw new TypeConversionException(
            "A JSON array does not match the declared List component " + targetType.getName()
                + " (found " + getJsonNodeTypeDescription(node) + ") at " + path
        );
      }
      java.lang.reflect.Type elementGenericType = componentMeta != null
          ? componentMeta.listElementGenericType()
          : JsonObjectReader.getGenericTypeArgument(genericType);
      List<Object> listValues = new ArrayList<>();
      List<JsonParser.JsonNode> elements = arrNode.elements();
      for (int j = 0; j < elements.size(); j++) {
        String itemPath = path + "[" + j + "]";
        Object item = readValue(JsonObjectReader.rawType(elementGenericType), elementGenericType, null, elements.get(j), itemPath, strict);
        listValues.add(item);
      }
      return Collections.unmodifiableList(listValues);
    }

    if (targetType.isArray()) {
      if (!(node instanceof JsonParser.JsonNode.JsonArrayNode arrayNode)) {
        throw new TypeConversionException("Expected a JSON array for " + targetType.getName() + " at " + path);
      }
      Class<?> elementType = targetType.getComponentType();
      Object array = java.lang.reflect.Array.newInstance(elementType, arrayNode.elements().size());
      for (int i = 0; i < arrayNode.elements().size(); i++) {
        Object value = readValue(elementType, elementType, null, arrayNode.elements().get(i), path + "[" + i + "]", strict);
        java.lang.reflect.Array.set(array, i, value);
      }
      return array;
    }

    if (targetType.isRecord()) {
      if (!(node instanceof JsonParser.JsonNode.JsonObjectNode objNode)) {
        throw new TypeConversionException(
            "Expected JSON object for nested record " + targetType.getName()
                + " but found " + getJsonNodeTypeDescription(node) + " at " + path
        );
      }
      RecordMetadata nestedMeta = RecordIntrospector.inspect(targetType.asSubclass(Record.class));
      return readRecord(nestedMeta, objNode, path, strict);
    }

    if (node instanceof JsonParser.JsonNode.JsonStringNode strNode) {
      return TypeConverter.convert(targetType, strNode.value(), path);
    }

    if (node instanceof JsonParser.JsonNode.JsonNumberNode numNode) {
      return TypeConverter.convert(targetType, numNode.bigDecimalValue(), path);
    }

    if (node instanceof JsonParser.JsonNode.JsonBooleanNode boolNode) {
      return TypeConverter.convert(targetType, boolNode.value(), path);
    }

    throw new TypeConversionException("Cannot convert JSON value " + node + " to type " + targetType.getName() + " at " + path);
  }

  static String getJsonNodeTypeDescription(JsonParser.JsonNode node) {
    if (node instanceof JsonParser.JsonNode.JsonObjectNode) {
      return "JSON object";
    } else if (node instanceof JsonParser.JsonNode.JsonArrayNode) {
      return "JSON array";
    } else if (node instanceof JsonParser.JsonNode.JsonStringNode) {
      return "JSON string";
    } else if (node instanceof JsonParser.JsonNode.JsonNumberNode) {
      return "JSON number";
    } else if (node instanceof JsonParser.JsonNode.JsonBooleanNode) {
      return "JSON boolean";
    } else if (node instanceof JsonParser.JsonNode.JsonNullNode) {
      return "JSON null";
    }
    return "JSON value";
  }
}
