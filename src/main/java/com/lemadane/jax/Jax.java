package com.lemadane.jax;

import java.util.Objects;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.json.internal.JsonDocumentReader;
import com.lemadane.jax.json.internal.JsonObjectReader;
import com.lemadane.jax.json.internal.JsonRecordReader;
import com.lemadane.jax.json.internal.JsonRecordWriter;
import com.lemadane.jax.xml.internal.XmlObjectReader;
import com.lemadane.jax.xml.internal.XmlRecordReader;
import com.lemadane.jax.xml.internal.XmlObjectWriter;

/**
* General mapping entry points for JSON data and Java objects.
*/
public final class Jax {

  Jax() {
  }

  /**
  * Maps JSON or XML input to a Java record or mutable class. Mutable classes may use
  * a uniquely matching constructor whose parameter names are retained with {@code -parameters};
  * otherwise, a no-argument constructor and public setters or XML-mapped public fields are required.
  *
  * @param json the JSON or XML input string.
  * @param targetType the target Java class or record.
  * @param <T> the target type.
  * @return the populated object or record.
  * @throws NullPointerException if either argument is null.
  * @throws com.lemadane.jax.exceptions.DataMappingException if parsing or mapping fails.
  */
  public static <T> T toObject(String json, Class<T> targetType) {
    Objects.requireNonNull(json, "json must not be null");
    Objects.requireNonNull(targetType, "targetType must not be null");
    int firstNonWhitespace = firstNonWhitespaceIndex(json);
    if (firstNonWhitespace == -1) {
      throw new JsonMappingException("Empty JSON/XML content");
    }
    char formatMarker = json.charAt(firstNonWhitespace);
    if (formatMarker == '<') {
      if (targetType.isRecord()) {
        return targetType.cast(XmlRecordReader.parse(targetType.asSubclass(Record.class), json, true));
      }
      return XmlObjectReader.toObject(json, targetType);
    }
    if (targetType.isRecord()) {
      return targetType.cast(JsonRecordReader.parse(targetType.asSubclass(Record.class), json, true));
    }
    return JsonObjectReader.toObject(json, targetType);
  }

  /**
  * Maps JSON or XML input to a Java record or mutable class, ignoring undeclared properties or elements.
  * Missing record components use the standard partial-mapping defaults; missing mutable-class properties
  * retain constructor or Java defaults.
  *
  * @param input the JSON or XML input string.
  * @param targetType the target Java class or record.
  * @param <T> the target type.
  * @return the partially populated object or record.
  * @throws NullPointerException if either argument is null.
  * @throws com.lemadane.jax.exceptions.DataMappingException if parsing or mapping fails.
  */
  public static <T> T toPartialObject(String input, Class<T> targetType) {
    Objects.requireNonNull(input, "input must not be null");
    Objects.requireNonNull(targetType, "targetType must not be null");
    int firstNonWhitespace = firstNonWhitespaceIndex(input);
    if (firstNonWhitespace == -1) {
      throw new JsonMappingException("Empty JSON/XML content");
    }
    char formatMarker = input.charAt(firstNonWhitespace);
    if (formatMarker == '<') {
      if (targetType.isRecord()) {
        return targetType.cast(XmlRecordReader.parse(targetType.asSubclass(Record.class), input, false));
      }
      return XmlObjectReader.toObject(input, targetType, false);
    }
    if (formatMarker == '{') {
      if (targetType.isRecord()) {
        return targetType.cast(JsonRecordReader.parse(targetType.asSubclass(Record.class), input, false));
      }
      return JsonObjectReader.toObject(input, targetType, false);
    }
    throw new JsonMappingException(
        "Cannot detect input format at index " + firstNonWhitespace
            + "; expected a JSON object or XML document"
    );
  }

  static int firstNonWhitespaceIndex(String input) {
    int index = input.startsWith("\uFEFF") ? 1 : 0;
    while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
      index++;
    }
    return index == input.length() ? -1 : index;
  }

  /**
  * Converts a JSON object to a Document with inferred Java values.
  *
  * @param json the JSON input string.
  * @return an insertion-ordered document representing the JSON object.
  * @throws NullPointerException if json is null.
  * @throws com.lemadane.jax.exceptions.JsonMappingException if the JSON is invalid or its root is not an object.
  */
  public static Document toDocument(String json) {
    Objects.requireNonNull(json, "json must not be null");
    return JsonDocumentReader.toDocument(json);
  }

  /**
  * Serializes a Java record or POJO with public instance fields to JSON.
  *
  * @param object the object to serialize.
  * @return compact JSON text.
  * @throws NullPointerException if object is null.
  * @throws com.lemadane.jax.exceptions.JsonMappingException if serialization fails.
  */
  public static String toJson(Object object) {
    return JsonRecordWriter.stringifyObject(Objects.requireNonNull(object, "object must not be null"));
  }

  /**
  * Serializes a Java record or POJO to XML.
  *
  * @param object the object to serialize.
  * @return compact XML text.
  * @throws NullPointerException if object is null.
  * @throws com.lemadane.jax.exceptions.XmlMappingException if serialization fails.
  */
  public static String toXml(Object object) {
    return XmlObjectWriter.stringify(object);
  }

}
