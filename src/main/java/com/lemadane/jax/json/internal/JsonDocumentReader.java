package com.lemadane.jax.json.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.lemadane.jax.Document;
import com.lemadane.jax.exceptions.JsonMappingException;

/**
* Converts JSON values into Jax Documents and inferred Java scalar values.
*/
public final class JsonDocumentReader {

  static final BigInteger INTEGER_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
  static final BigInteger INTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
  static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

  JsonDocumentReader() {
  }

  public static Document toDocument(String json) {
    JsonParser.JsonNode rootNode = JsonParser.parse(json);
    if (!(rootNode instanceof JsonParser.JsonNode.JsonObjectNode objectNode)) {
      throw new JsonMappingException(
          "A JSON object is expected but " + describe(rootNode) + " was supplied at $"
      );
    }
    return readObject(objectNode);
  }

  static Document readObject(JsonParser.JsonNode.JsonObjectNode objectNode) {
    Document document = new Document();
    for (Map.Entry<String, JsonParser.JsonNode> entry : objectNode.members().entrySet()) {
      document.put(entry.getKey(), readValue(entry.getValue()));
    }
    return document;
  }

  static Object readValue(JsonParser.JsonNode node) {
    if (node instanceof JsonParser.JsonNode.JsonObjectNode objectNode) {
      return readObject(objectNode);
    }
    if (node instanceof JsonParser.JsonNode.JsonArrayNode arrayNode) {
      List<Object> values = new ArrayList<>(arrayNode.elements().size());
      for (JsonParser.JsonNode item : arrayNode.elements()) {
        values.add(readValue(item));
      }
      return Collections.unmodifiableList(values);
    }
    if (node instanceof JsonParser.JsonNode.JsonStringNode stringNode) {
      return stringNode.value();
    }
    if (node instanceof JsonParser.JsonNode.JsonBooleanNode booleanNode) {
      return booleanNode.value();
    }
    if (node instanceof JsonParser.JsonNode.JsonNumberNode numberNode) {
      return inferNumber(numberNode);
    }
    if (node instanceof JsonParser.JsonNode.JsonNullNode) {
      return null;
    }
    throw new JsonMappingException("Unsupported JSON value");
  }

  static Number inferNumber(JsonParser.JsonNode.JsonNumberNode numberNode) {
    String rawNumber = numberNode.rawNumber();
    if (rawNumber.indexOf('.') >= 0 || rawNumber.indexOf('e') >= 0 || rawNumber.indexOf('E') >= 0) {
      return numberNode.bigDecimalValue();
    }

    BigInteger integer = new BigInteger(rawNumber);
    if (integer.compareTo(INTEGER_MIN) >= 0 && integer.compareTo(INTEGER_MAX) <= 0) {
      return integer.intValue();
    }
    if (integer.compareTo(LONG_MIN) >= 0 && integer.compareTo(LONG_MAX) <= 0) {
      return integer.longValue();
    }
    return numberNode.bigDecimalValue();
  }

  static String describe(JsonParser.JsonNode node) {
    if (node instanceof JsonParser.JsonNode.JsonArrayNode) {
      return "JSON array";
    }
    if (node instanceof JsonParser.JsonNode.JsonStringNode) {
      return "JSON string";
    }
    if (node instanceof JsonParser.JsonNode.JsonNumberNode) {
      return "JSON number";
    }
    if (node instanceof JsonParser.JsonNode.JsonBooleanNode) {
      return "JSON boolean";
    }
    return "JSON null";
  }
}
