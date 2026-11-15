package com.lemadane.jax.json.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.exceptions.JsonMappingException;

import static org.assertj.core.api.Assertions.*;

class JsonParserHelperTest {
  @ParameterizedTest
  @ValueSource(strings = {"{", "[", "[1,", "{\"x\":1,", "\"abc\\", "\"\\u12", "-", "-x",
      "1e999999999999999", "[1 2]", "{\"x\":1,}", "[1,]"})
  void rejectsTruncatedAndMalformedTokens(String input) {
    assertThatThrownBy(() -> JsonParser.parse(input)).isInstanceOf(JsonMappingException.class);
  }

  @Test
  void enforcesNumberLengthAndExponentBoundaries() {
    assertThatCode(() -> JsonParser.validateNumberSize("1e+10000", "$" )).doesNotThrowAnyException();
    assertThatCode(() -> JsonParser.validateNumberSize("1e-10000", "$" )).doesNotThrowAnyException();
    assertThatCode(() -> JsonParser.parse("1".repeat(1024))).doesNotThrowAnyException();
    assertThatThrownBy(() -> JsonParser.parse("1".repeat(1025)))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("token length");
    assertThatThrownBy(() -> JsonParser.validateNumberSize("1".repeat(1025), "$"))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("token length");
    for (String number : new String[] {"1e10001", "1e-10001", "1e999999999999"}) {
      assertThatThrownBy(() -> JsonParser.validateNumberSize(number, "$" )).isInstanceOf(JsonMappingException.class);
    }
  }

  @Test
  void parsesStringEscapesAndSurrogatePairs() {
    assertThat(new JsonParser("\"\\uD83D\\uDE00\"").parseStringValue("$")).isEqualTo("😀");
    assertThat(new JsonParser("\"\\uD800\\u0041\"").parseStringValue("$"))
        .isEqualTo(new String(new char[] {(char) 0xD800, 'A'}));
    assertThat(new JsonParser("\"\\uD800\"").parseStringValue("$"))
        .isEqualTo(String.valueOf((char) 0xD800));
    assertThat(new JsonParser("\"\\/\\b\\f\\n\\r\\t\"").parseStringValue("$"))
        .isEqualTo("/\b\f\n\r\t");
    assertThatThrownBy(() -> JsonParser.parse(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void tracksPositionAcrossWhitespaceAndExpectedTokens() {
    JsonParser parser = new JsonParser(" \t\r\n[]");
    parser.skipWhitespace();
    assertThat(parser.pos).isEqualTo(4);
    parser.expect('[', "$");
    assertThat(parser.pos).isEqualTo(5);
    assertThatThrownBy(() -> parser.expect('}', "$" )).isInstanceOf(JsonMappingException.class);
    assertThat(parser.pos).isEqualTo(5);
    parser.requireDepth("$", 255);
    assertThatThrownBy(() -> parser.requireDepth("$", 256)).isInstanceOf(JsonMappingException.class);
  }
  @Test
  void acceptsZeroButRejectsLeadingZeroIntegers() {
    for (String input : new String[] {"0", "-0", "0.5", "0e2"}) {
      var number = (JsonParser.JsonNode.JsonNumberNode) JsonParser.parse(input);
      assertThat(number.bigDecimalValue()).isEqualByComparingTo(new java.math.BigDecimal(input));
    }
    for (String input : new String[] {"00", "01", "-01"}) {
      assertThatThrownBy(() -> JsonParser.parse(input)).isInstanceOf(JsonMappingException.class);
    }
  }

}
