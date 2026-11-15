package com.lemadane.jax.json.internal;

import java.time.LocalDate;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.TypeConversionException;

import static org.assertj.core.api.Assertions.*;

class JsonMappingContractTest {
  record Value(String value) {}

  public static class Pojo {
    public String value = "text";
  }

  @Test
  void writesRecordAndPojoNodesThroughDirectHelpers() {
    Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    assertThat(JsonRecordWriter.recordToNode(new Value("text"), seen)).isEqualTo(JsonParser.parse("{\"value\":\"text\"}"));
    assertThat(JsonRecordWriter.pojoToNode(new Pojo(), seen)).isEqualTo(JsonParser.parse("{\"value\":\"text\"}"));
    assertThat(seen).isEmpty();
    assertThat(JsonRecordWriter.stringifyObject(LocalDate.of(2026, 1, 1))).isEqualTo("\"2026-01-01\"");
  }

  @Test
  void rejectsIncompatibleJsonShapesForRecordValues() {
    assertThatThrownBy(() -> JsonRecordReader.readValue(int[].class, int[].class, null,
        JsonParser.parse("1"), "$.value", true))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("array");
    assertThatThrownBy(() -> JsonRecordReader.readValue(String.class, String.class, null,
        JsonParser.parse("{}"), "$.value", true))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("$.value");
    assertThat(JsonRecordReader.getJsonNodeTypeDescription(JsonParser.parse("{}"))).isEqualTo("JSON object");
  }

  @Test
  void preservesNullableScalarsAndRejectsFloatOverflow() {
    assertThat(TypeConverter.convert(String.class, null, "$.value")).isNull();
    assertThatThrownBy(() -> TypeConverter.convert(float.class, "1e100", "$.value"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("$.value");
  }
}
