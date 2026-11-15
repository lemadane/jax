package com.lemadane.jax.json.internal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.lemadane.jax.Document;
import com.lemadane.jax.exceptions.JsonMappingException;

import static org.assertj.core.api.Assertions.*;

class JsonValueHelpersTest {
  @ParameterizedTest
  @CsvSource({"-2147483648,Integer", "2147483647,Integer", "-2147483649,Long", "2147483648,Long",
      "-9223372036854775808,Long", "9223372036854775807,Long", "9223372036854775808,BigDecimal",
      "-9223372036854775809,BigDecimal", "1.0,BigDecimal", "1e2,BigDecimal", "1E2,BigDecimal"})
  void infersDocumentNumbersWithoutLosingPrecision(String input, String expectedType) {
    Number number = JsonDocumentReader.inferNumber((JsonParser.JsonNode.JsonNumberNode) JsonParser.parse(input));
    assertThat(number.getClass().getSimpleName()).isEqualTo(expectedType);
    assertThat(new BigDecimal(number.toString())).isEqualByComparingTo(new BigDecimal(input));
  }

  @Test
  void readsNestedDocumentsAndImmutableArrays() {
    Object value = JsonDocumentReader.readValue(JsonParser.parse("{\"child\":{\"values\":[null,true,\"x\"]}}"));
    Document document = (Document) value;
    Document child = (Document) document.get("child");
    List<?> values = (List<?>) child.get("values");
    assertThat(values).isEqualTo(java.util.Arrays.asList(null, true, "x"));
    assertThatThrownBy(values::clear).isInstanceOf(UnsupportedOperationException.class);
    for (String input : List.of("[]", "\"x\"", "1", "true", "null")) {
      assertThatThrownBy(() -> JsonDocumentReader.toDocument(input))
          .isInstanceOf(JsonMappingException.class).hasMessageContaining(JsonDocumentReader.describe(JsonParser.parse(input)));
    }
  }

  @Test
  void writesPrimitiveArraysAndSharedReferencesWithoutFalseCycles() {
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    int[] values = {1, 2};
    var node = JsonRecordWriter.toNode(List.of(values, values), visited);
    StringBuilder output = new StringBuilder();
    JsonRecordWriter.writeNode(node, output);
    assertThat(output).hasToString("[[1,2],[1,2]]");
    assertThat(visited).isEmpty();
    assertThat(JsonRecordWriter.stringifyObject(Map.of("empty", List.of()))).isEqualTo("{\"empty\":[]}");
    assertThat(JsonRecordWriter.escapeString(null)).isEmpty();
  }
}
