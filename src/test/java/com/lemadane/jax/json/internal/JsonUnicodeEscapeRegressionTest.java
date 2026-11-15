package com.lemadane.jax.json.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.Jax;
import com.lemadane.jax.exceptions.JsonMappingException;

import static org.assertj.core.api.Assertions.*;

class JsonUnicodeEscapeRegressionTest {
  @ParameterizedTest
  @ValueSource(strings = {"+041", "-041", "１２３４", "١٢٣٤", " 041", "00G0", "000g"})
  void rejectsAnythingExceptFourAsciiHexDigits(String digits) {
    String escaped = "\\u" + digits;
    assertThatThrownBy(() -> Jax.toDocument("{\"value\":\"" + escaped + "\"}"))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Unicode escape");
    assertThatThrownBy(() -> Jax.toDocument("{\"" + escaped + "\":1}"))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Unicode escape");
    assertThatThrownBy(() -> new JsonParser(digits).parseHex4("$.value"))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("$.value");
  }

  @Test
  void acceptsUppercaseLowercaseAndSurrogatePairEscapes() {
    assertThat(Jax.toDocument("{\"\\u0041\":\"\\u00aF\\u00Af\\uD83D\\uDE00\"}"))
        .containsEntry("A", "¯¯😀");
    assertThat(new JsonParser("0000").parseHex4("$")).isZero();
    assertThat(new JsonParser("ffff").parseHex4("$")).isEqualTo(65535);
    assertThat(new JsonParser("FFFF").parseHex4("$")).isEqualTo(65535);
  }

  @Test
  void rejectsMalformedSecondEscapeAfterHighSurrogate() {
    assertThatThrownBy(() -> JsonParser.parse("\"\\uD800\\u+041\""))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Unicode escape");
  }
}
