package com.lemadane.jax.json.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.NameEncodingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.PojoPropertyReader;
import com.lemadane.jax.reflect.RecordIntrospector;

/** Writes values directly; the tree writer remains available for AST consumers. */
final class JsonDirectWriter {
   JsonDirectWriter() {
  }

  // The tree writer historically keeps the last value for a decoded-name collision.
  // Keep that uncommon path intact, including validation of overwritten values.
   static final ClassValue<Boolean> COLLIDING_NAMES = new ClassValue<>() {
    @Override
    protected Boolean computeValue(Class<?> type) {
      Set<String> names = new HashSet<>();
      for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
        for (Field field : current.getDeclaredFields()) {
          if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
            try {
              if (!names.add(DataNameCodec.decode(field.getName()))) {
                return true;
              }
            } catch (NameEncodingException | IllegalArgumentException e) {
              // An unreadable  field may have an invalid encoded name.
              // Let the original traversal decide whether that field is used.
              return true;
            }
          }
        }
      }
      return false;
    }
  };

  static void write(Object value, StringBuilder output, Set<Object> visited, int depth) {
    JsonRecordWriter.requireDepth(depth);
    if (value == null) {
      output.append("null");
    } else if (value instanceof Optional<?> optional) {
      if (optional.isPresent()) {
        write(optional.get(), output, visited, depth + 1);
      } else {
        output.append("null");
      }
    } else if (value instanceof Collection<?> || value.getClass().isArray()) {
      enter(value, visited);
      try {
        output.append('[');
        boolean first = true;
        if (value instanceof Collection<?> collection) {
          for (Object item : collection) {
            if (!first) {
              output.append(',');
            }
            first = false;
            write(item, output, visited, depth + 1);
          }
        } else {
          int length = Array.getLength(value);
          for (int i = 0; i < length; i++) {
            if (i > 0) {
              output.append(',');
            }
            write(Array.get(value, i), output, visited, depth + 1);
          }
        }
        output.append(']');
      } finally {
        visited.remove(value);
      }
    } else if (value instanceof Map<?, ?> map) {
      if (new HashSet<>(map.keySet()).size() != map.size()) {
        JsonRecordWriter.writeNode(JsonRecordWriter.toNode(value, visited, depth), output);
        return;
      }
      enter(value, visited);
      try {
        output.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (!(entry.getKey() instanceof String key)) {
            throw new JsonMappingException("JSON object map keys must be strings");
          }
          if (!first) {
            output.append(',');
          }
          first = false;
          quote(key, output);
          output.append(':');
          write(entry.getValue(), output, visited, depth + 1);
        }
        output.append('}');
      } finally {
        visited.remove(value);
      }
    } else if (value instanceof String || value instanceof Character) {
      quote(value.toString(), output);
    } else if (value instanceof Boolean) {
      output.append(value);
    } else if (value instanceof Number number) {
      writeNumber(number, output);
    } else if (value instanceof Enum<?> enumeration) {
      quote(enumeration.name(), output);
    } else if (value instanceof Record || !value.getClass().getPackageName().startsWith("java.")) {
      writeObject(value, output, visited, depth);
    } else {
      quote(value.toString(), output);
    }
  }

   static void writeNumber(Number number, StringBuilder output) {
    if (number instanceof Float f && !Float.isFinite(f)
        || number instanceof Double d && !Double.isFinite(d)) {
      throw new JsonMappingException("Non-finite number value " + number + " is not supported in JSON");
    }
    String raw = number.toString();
    // Primitive wrappers and BigInteger provide valid numeric text. Preserve the
    // old validation for BigDecimal (including extreme scales) and custom Numbers.
    Class<?> type = number.getClass();
    if (type != Integer.class && type != Long.class && type != Double.class && type != Float.class
        && type != Short.class && type != Byte.class && type != BigInteger.class) {
      new BigDecimal(raw);
    }
    output.append(raw);
  }

   static void writeObject(Object value, StringBuilder output, Set<Object> visited, int depth) {
    if (COLLIDING_NAMES.get(value.getClass())) {
      JsonRecordWriter.writeNode(JsonRecordWriter.toNode(value, visited, depth), output);
      return;
    }
    enter(value, visited);
    try {
      output.append('{');
      boolean first = true;
      if (value instanceof Record record) {
        for (var component : RecordIntrospector.inspect(record.getClass()).components()) {
          Object property;
          try {
            property = component.accessor().invoke(record);
          } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new JsonMappingException("Failed to access record component " + component.name()
                + " on " + record.getClass().getName(), e);
          }
          if (!first) {
            output.append(',');
          }
          first = false;
          quote(component.decodedName(), output);
          output.append(':');
          write(property, output, visited, depth + 1);
        }
      } else {
        try {
          for (var property : PojoPropertyReader.read(value)) {
            if (!first) {
              output.append(',');
            }
            first = false;
            quote(DataNameCodec.decode(property.name()), output);
            output.append(':');
            write(property.value(), output, visited, depth + 1);
          }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
          throw new JsonMappingException("Failed to access POJO properties on " + value.getClass().getName(), e);
        }
      }
      output.append('}');
    } finally {
      visited.remove(value);
    }
  }

   static void enter(Object value, Set<Object> visited) {
    if (!visited.add(value)) {
      String kind = value instanceof Record ? "record instance of " : "";
      throw new JsonMappingException("Cycle detected in object graph for " + kind + value.getClass().getName());
    }
  }

   static void quote(String value, StringBuilder output) {
    output.append('"').append(JsonRecordWriter.escapeString(value)).append('"');
  }
}
