package com.lemadane.jax.json.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lemadane.jax.exceptions.JsonMappingException;

/**
* Clean internal JSON recursive-descent parser.
*/
public final class JsonParser {

  static final int MAX_NESTING_DEPTH = 256;
  static final int MAX_NUMBER_TOKEN_LENGTH = 1024;
  static final int MAX_ABSOLUTE_EXPONENT = 10000;

  public sealed interface JsonNode {
    record JsonObjectNode(Map<String, JsonNode> members) implements JsonNode {}
    record JsonArrayNode(List<JsonNode> elements) implements JsonNode {}
    record JsonStringNode(String value) implements JsonNode {}
    record JsonNumberNode(String rawNumber, BigDecimal bigDecimalValue) implements JsonNode {}
    record JsonBooleanNode(boolean value) implements JsonNode {}
    record JsonNullNode() implements JsonNode {}
  }

  static final JsonNode.JsonBooleanNode TRUE_NODE = new JsonNode.JsonBooleanNode(true);
  static final JsonNode.JsonBooleanNode FALSE_NODE = new JsonNode.JsonBooleanNode(false);
  static final JsonNode.JsonNullNode NULL_NODE = new JsonNode.JsonNullNode();

  final String json;
  final int length;
  int pos;

  JsonParser(String json) {
    this.json = json;
    this.length = json.length();
    this.pos = 0;
  }

  public static JsonNode parse(String json) {
    if (json == null) {
      throw new NullPointerException("json must not be null");
    }
    JsonParser parser = new JsonParser(json);
    parser.skipWhitespace();
    if (parser.pos >= parser.length) {
      throw new JsonMappingException("Empty JSON content");
    }
    JsonNode root = parser.parseValue(new StringBuilder("$"), 0);
    parser.skipWhitespace();
    if (parser.pos < parser.length) {
      throw new JsonMappingException("Trailing non-whitespace content after root JSON value at index " + parser.pos);
    }
    return root;
  }

  JsonNode parseValue(CharSequence path, int depth) {
    skipWhitespace();
    if (pos >= length) {
      throw new JsonMappingException("Unexpected end of JSON input at " + path);
    }

    char c = json.charAt(pos);
    if (c == '{') {
      requireDepth(path, depth);
      return parseObject(path, depth + 1);
    } else if (c == '[') {
      requireDepth(path, depth);
      return parseArray(path, depth + 1);
    } else if (c == '"') {
      return parseString(path);
    } else if (c == 't' || c == 'f') {
      return parseBoolean(path);
    } else if (c == 'n') {
      return parseNull(path);
    } else if (c == '-' || (c >= '0' && c <= '9')) {
      return parseNumber(path);
    } else {
      throw new JsonMappingException("Unexpected character '" + c + "' at " + path);
    }
  }

  void requireDepth(CharSequence path, int depth) {
    if (depth >= MAX_NESTING_DEPTH) {
      throw new JsonMappingException(
          "Maximum JSON nesting depth of " + MAX_NESTING_DEPTH + " exceeded at " + path
      );
    }
  }

  JsonNode.JsonObjectNode parseObject(CharSequence path, int depth) {
    expect('{', path);
    skipWhitespace();
    Map<String, JsonNode> members = new LinkedHashMap<>();

    if (pos < length && json.charAt(pos) == '}') {
      pos++;
      return new JsonNode.JsonObjectNode(members);
    }

    StringBuilder currentPath = path instanceof StringBuilder builder ? builder : new StringBuilder(path);
    int parentLength = currentPath.length();
    while (pos < length) {
      skipWhitespace();
      if (pos >= length || json.charAt(pos) != '"') {
        throw new JsonMappingException("Expected JSON property key string at " + path);
      }

      String key = parseStringValue(path);
      currentPath.append('.').append(key);
      try {
        if (members.containsKey(key)) {
          throw new JsonMappingException("Duplicate JSON key \"" + key + "\" encountered at " + currentPath);
        }
        skipWhitespace();
        expect(':', currentPath);
        members.put(key, parseValue(currentPath, depth));
      } finally {
        currentPath.setLength(parentLength);
      }

      skipWhitespace();
      if (pos < length && json.charAt(pos) == ',') {
        pos++;
      } else if (pos < length && json.charAt(pos) == '}') {
        pos++;
        return new JsonNode.JsonObjectNode(members);
      } else {
        throw new JsonMappingException("Expected ',' or '}' in object at " + path);
      }
    }

    throw new JsonMappingException("Unterminated JSON object at " + path);
  }

  JsonNode.JsonArrayNode parseArray(CharSequence path, int depth) {
    expect('[', path);
    skipWhitespace();
    List<JsonNode> elements = new ArrayList<>();

    if (pos < length && json.charAt(pos) == ']') {
      pos++;
      return new JsonNode.JsonArrayNode(elements);
    }

    int index = 0;
    StringBuilder currentPath = path instanceof StringBuilder builder ? builder : new StringBuilder(path);
    int parentLength = currentPath.length();
    while (pos < length) {
      currentPath.append('[').append(index).append(']');
      try {
        elements.add(parseValue(currentPath, depth));
      } finally {
        currentPath.setLength(parentLength);
      }
      index++;

      skipWhitespace();
      if (pos < length && json.charAt(pos) == ',') {
        pos++;
      } else if (pos < length && json.charAt(pos) == ']') {
        pos++;
        return new JsonNode.JsonArrayNode(elements);
      } else {
        throw new JsonMappingException("Expected ',' or ']' in array at " + path);
      }
    }

    throw new JsonMappingException("Unterminated JSON array at " + path);
  }

  JsonNode.JsonStringNode parseString(CharSequence path) {
    return new JsonNode.JsonStringNode(parseStringValue(path));
  }

  String parseStringValue(CharSequence path) {
    expect('"', path);
    int start = pos;
    while (pos < length) {
      char c = json.charAt(pos);
      if (c == '"') {
        String value = json.substring(start, pos);
        pos++;
        return value;
      }
      if (c == '\\') {
        break;
      }
      if (c < 0x20) {
        throw new JsonMappingException("Unescaped control character 0x" + Integer.toHexString(c) + " in string at " + path);
      }
      pos++;
    }
    StringBuilder sb = new StringBuilder().append(json, start, pos);

    while (pos < length) {
      char c = json.charAt(pos++);
      if (c == '"') {
        return sb.toString();
      } else if (c == '\\') {
        if (pos >= length) {
          throw new JsonMappingException("Unterminated escape sequence in string at " + path);
        }
        char esc = json.charAt(pos++);
        switch (esc) {
          case '"' -> sb.append('"');
          case '\\' -> sb.append('\\');
          case '/' -> sb.append('/');
          case 'b' -> sb.append('\b');
          case 'f' -> sb.append('\f');
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case 't' -> sb.append('\t');
          case 'u' -> {
            int codePoint = parseHex4(path);
            if (Character.isHighSurrogate((char) codePoint)) {
              // Check for trailing low surrogate
              if (pos + 5 < length && json.charAt(pos) == '\\' && json.charAt(pos + 1) == 'u') {
                pos += 2;
                int low = parseHex4(path);
                if (Character.isLowSurrogate((char) low)) {
                  sb.appendCodePoint(Character.toCodePoint((char) codePoint, (char) low));
                } else {
                  sb.append((char) codePoint);
                  sb.append((char) low);
                }
              } else {
                sb.append((char) codePoint);
              }
            } else {
              sb.append((char) codePoint);
            }
          }
          default -> throw new JsonMappingException("Invalid escape character '\\" + esc + "' at " + path);
        }
      } else if (c < 0x20) {
        throw new JsonMappingException("Unescaped control character 0x" + Integer.toHexString(c) + " in string at " + path);
      } else {
        sb.append(c);
      }
    }

    throw new JsonMappingException("Unterminated string at " + path);
  }

  int parseHex4(CharSequence path) {
    if (pos + 4 > length) {
      throw new JsonMappingException("Invalid Unicode escape sequence at " + path);
    }
    int value = 0;
    for (int i = 0; i < 4; i++) {
      char digit = json.charAt(pos++);
      int hex;
      if (digit >= '0' && digit <= '9') {
        hex = digit - '0';
      } else if (digit >= 'a' && digit <= 'f') {
        hex = digit - 'a' + 10;
      } else if (digit >= 'A' && digit <= 'F') {
        hex = digit - 'A' + 10;
      } else {
        throw new JsonMappingException("Invalid hex in Unicode escape at " + path);
      }
      value = (value << 4) | hex;
    }
    return value;
  }

  JsonNode.JsonBooleanNode parseBoolean(CharSequence path) {
    if (json.startsWith("true", pos)) {
      pos += 4;
      return TRUE_NODE;
    } else if (json.startsWith("false", pos)) {
      pos += 5;
      return FALSE_NODE;
    }
    throw new JsonMappingException("Invalid boolean literal at " + path);
  }

  JsonNode.JsonNullNode parseNull(CharSequence path) {
    if (json.startsWith("null", pos)) {
      pos += 4;
      return NULL_NODE;
    }
    throw new JsonMappingException("Invalid null literal at " + path);
  }

  JsonNode.JsonNumberNode parseNumber(CharSequence path) {
    int start = pos;
    if (json.charAt(pos) == '-') {
      pos++;
    }
    if (pos >= length) {
      throw new JsonMappingException("Invalid number at " + path);
    }
    if (json.charAt(pos) == '0') {
      pos++;
    } else if (json.charAt(pos) >= '1' && json.charAt(pos) <= '9') {
      while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
        pos++;
      }
    } else {
      throw new JsonMappingException("Invalid number format at " + path);
    }

    if (pos < length && json.charAt(pos) == '.') {
      pos++;
      int fracStart = pos;
      while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
        pos++;
      }
      if (pos == fracStart) {
        throw new JsonMappingException("Expected digits after decimal point at " + path);
      }
    }

    if (pos < length && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
      pos++;
      if (pos < length && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
        pos++;
      }
      int expStart = pos;
      while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
        pos++;
      }
      if (pos == expStart) {
        throw new JsonMappingException("Expected digits after exponent at " + path);
      }
    }

    if (pos - start > MAX_NUMBER_TOKEN_LENGTH) {
      throw new JsonMappingException(
          "JSON number exceeds maximum token length of " + MAX_NUMBER_TOKEN_LENGTH + " at " + path
      );
    }
    String raw = json.substring(start, pos);
    validateNumberSize(raw, path);
    try {
      BigDecimal bd = new BigDecimal(raw);
      return new JsonNode.JsonNumberNode(raw, bd);
    } catch (NumberFormatException e) {
      throw new JsonMappingException("Invalid numeric value \"" + raw + "\" at " + path, e);
    }
  }

  static void validateNumberSize(String rawNumber, CharSequence path) {
    if (rawNumber.length() > MAX_NUMBER_TOKEN_LENGTH) {
      throw new JsonMappingException(
          "JSON number exceeds maximum token length of " + MAX_NUMBER_TOKEN_LENGTH + " at " + path
      );
    }
    int exponentIndex = Math.max(rawNumber.indexOf('e'), rawNumber.indexOf('E'));
    if (exponentIndex < 0) {
      return;
    }
    try {
      int exponent = Integer.parseInt(rawNumber.substring(exponentIndex + 1));
      if (Math.abs((long) exponent) > MAX_ABSOLUTE_EXPONENT) {
        throw new JsonMappingException(
            "JSON number exponent exceeds maximum absolute value of " + MAX_ABSOLUTE_EXPONENT + " at " + path
        );
      }
    } catch (NumberFormatException e) {
      throw new JsonMappingException("Invalid JSON number exponent at " + path, e);
    }
  }

  void expect(char expected, CharSequence path) {
    if (pos >= length || json.charAt(pos) != expected) {
      throw new JsonMappingException("Expected '" + expected + "' at " + path);
    }
    pos++;
  }

  void skipWhitespace() {
    while (pos < length) {
      char c = json.charAt(pos);
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        pos++;
      } else {
        break;
      }
    }
  }
}
