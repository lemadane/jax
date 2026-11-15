package com.lemadane.jax.json.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.PojoPropertyReader;
import com.lemadane.jax.reflect.RecordIntrospector;
import com.lemadane.jax.reflect.RecordMetadata;
import com.lemadane.jax.json.internal.JsonParser.JsonNode;

/**
* Internal JSON writer that converts Java records into compact JSON strings.
*/
public final class JsonRecordWriter {

  JsonRecordWriter() {
  }

  public static String stringifyObject(Object object) {
    Objects.requireNonNull(object, "object must not be null");
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    JsonNode node = toNode(object, visited);
    StringBuilder sb = new StringBuilder();
    writeNode(node, sb);
    return sb.toString();
  }

  static JsonNode toNode(Object val, Set<Object> visited) {
    if (val == null) {
      return new JsonNode.JsonNullNode();
    }

    if (val instanceof Optional<?> opt) {
      return opt.isPresent() ? toNode(opt.get(), visited) : new JsonNode.JsonNullNode();
    }

    if (val instanceof Collection<?> || val.getClass().isArray()) {
      if (!visited.add(val)) {
        throw new JsonMappingException("Cycle detected in object graph for " + val.getClass().getName());
      }
      try {
        List<JsonNode> elements = new ArrayList<>();
        if (val instanceof Collection<?> values) {
          for (Object item : values) {
            elements.add(toNode(item, visited));
          }
        } else {
          for (int i = 0; i < Array.getLength(val); i++) {
            elements.add(toNode(Array.get(val, i), visited));
          }
        }
        return new JsonNode.JsonArrayNode(elements);
      } finally {
        visited.remove(val);
      }
    }

    if (val instanceof Map<?, ?> map) {
      if (!visited.add(val)) {
        throw new JsonMappingException("Cycle detected in object graph for " + val.getClass().getName());
      }
      try {
        Map<String, JsonNode> members = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (!(entry.getKey() instanceof String key)) {
            throw new JsonMappingException("JSON object map keys must be strings");
          }
          members.put(key, toNode(entry.getValue(), visited));
        }
        return new JsonNode.JsonObjectNode(members);
      } finally {
        visited.remove(val);
      }
    }

    if (val instanceof String || val instanceof Character) {
      return new JsonNode.JsonStringNode(val.toString());
    }

    if (val instanceof Boolean) {
      return new JsonNode.JsonBooleanNode((Boolean) val);
    }

    if (val instanceof Number num) {
      if (num instanceof Float f) {
        if (f.isNaN() || f.isInfinite()) {
          throw new JsonMappingException("Non-finite number value " + f + " is not supported in JSON");
        }
      } else if (num instanceof Double d) {
        if (d.isNaN() || d.isInfinite()) {
          throw new JsonMappingException("Non-finite number value " + d + " is not supported in JSON");
        }
      }
      return new JsonNode.JsonNumberNode(num.toString(), new java.math.BigDecimal(num.toString()));
    }

    if (val instanceof Enum<?> e) {
      return new JsonNode.JsonStringNode(e.name());
    }

    if (val instanceof Record record) {
      return recordToNode(record, visited);
    }

    if (!val.getClass().getPackageName().startsWith("java.")) {
      return pojoToNode(val, visited);
    }
    return new JsonNode.JsonStringNode(val.toString());
  }

  static JsonNode recordToNode(Record record, Set<Object> visited) {
    if (!visited.add(record)) {
      throw new JsonMappingException("Cycle detected in object graph for record instance of " + record.getClass().getName());
    }
    try {
      RecordMetadata metadata = RecordIntrospector.inspect(record.getClass());
      Map<String, JsonNode> members = new LinkedHashMap<>();
      for (RecordMetadata.ComponentMetadata component : metadata.components()) {
        Object value;
        try {
          value = component.accessor().invoke(record);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
          throw new JsonMappingException(
              "Failed to access record component " + component.name() + " on " + record.getClass().getName(),
              e
          );
        }
        members.put(component.decodedName(), toNode(value, visited));
      }
      return new JsonNode.JsonObjectNode(members);
    } finally {
      visited.remove(record);
    }
  }

  static JsonNode pojoToNode(Object object, Set<Object> visited) {
    if (!visited.add(object)) {
      throw new JsonMappingException("Cycle detected in object graph for " + object.getClass().getName());
    }
    try {
      Map<String, JsonNode> members = new LinkedHashMap<>();
      try {
        for (PojoPropertyReader.Property property : PojoPropertyReader.read(object)) {
          members.put(DataNameCodec.decode(property.name()), toNode(property.value(), visited));
        }
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
        throw new JsonMappingException("Failed to access POJO properties on " + object.getClass().getName(), e);
      }
      return new JsonNode.JsonObjectNode(members);
    } finally {
      visited.remove(object);
    }
  }

  static void writeNode(JsonNode node, StringBuilder sb) {
    if (node instanceof JsonNode.JsonObjectNode objectNode) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<String, JsonNode> entry : objectNode.members().entrySet()) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        sb.append('"').append(escapeString(entry.getKey())).append("\":");
        writeNode(entry.getValue(), sb);
      }
      sb.append('}');
    } else if (node instanceof JsonNode.JsonArrayNode arrayNode) {
      sb.append('[');
      for (int i = 0; i < arrayNode.elements().size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        writeNode(arrayNode.elements().get(i), sb);
      }
      sb.append(']');
    } else if (node instanceof JsonNode.JsonStringNode stringNode) {
      sb.append('"').append(escapeString(stringNode.value())).append('"');
    } else if (node instanceof JsonNode.JsonNumberNode numberNode) {
      sb.append(numberNode.rawNumber());
    } else if (node instanceof JsonNode.JsonBooleanNode booleanNode) {
      sb.append(booleanNode.value());
    } else {
      sb.append("null");
    }
  }

  static String escapeString(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            String hex = Integer.toHexString(c);
            sb.append("\\u").append("0".repeat(4 - hex.length())).append(hex);
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }
}
