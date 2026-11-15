package com.lemadane.jax.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lemadane.jax.Document;
import com.lemadane.jax.Jax;
import com.lemadane.jax.exceptions.JsonMappingException;

class JsonDocumentMappingTest {

  @DisplayName(
    "Maps nested JSON to a Document and infers scalar value types")
  @Test
  void mapsNestedJsonToDocumentAndInfersValueTypes() {
  Document document = Jax.toDocument("""
        {
          "name": "Ada",
          "age": 37,
          "large": 2147483648,
          "tooLarge": 9223372036854775808,
          "ratio": 1.25,
          "active": true,
          "missing": null,
          "address": {"city": "London"},
          "tags": ["math", "logic"]
        }
        """);

    assertThat(document.get("name")).isEqualTo("Ada");
    assertThat(
      document.get("age"))
      .isInstanceOf(Integer.class)
      .isEqualTo(37);
    assertThat(
      document.get("large"))
      .isInstanceOf(Long.class)
      .isEqualTo(2147483648L);
    assertThat(
      document.get("tooLarge"))
      .isEqualTo(new BigDecimal("9223372036854775808"));
    assertThat(
      document.get("ratio"))
      .isEqualTo(new BigDecimal("1.25"));
    assertThat(document.get("active")).isEqualTo(true);
    assertThat(document).containsEntry("missing", null);

    Document address = (Document) document.get("address");
    assertThat(address.get("city")).isEqualTo("London");
    assertThat(
      document.get("tags"))
      .isEqualTo(List.of("math", "logic"));
    }

    @DisplayName("Requires a JSON object at the document root")
    @Test
    void requiresAnObjectAtTheJsonRoot() {
    assertThatThrownBy(() -> Jax.toDocument("[1, 2]"))
      .isInstanceOf(JsonMappingException.class)
      .hasMessageContaining("A JSON object is expected");
    }
  }
