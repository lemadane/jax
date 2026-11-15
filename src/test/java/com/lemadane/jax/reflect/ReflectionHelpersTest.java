package com.lemadane.jax.reflect;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.RecordConstructionException;

import static org.assertj.core.api.Assertions.*;

class ReflectionHelpersTest {
  record Types(List<List<Integer>> nested, Optional<String> optional, List<?> wildcard, List raw) {}
  record Validated(int value) {
    Validated {
      if (value < 0) {
        throw new IllegalArgumentException("negative");
      }
    }
  }

  public static class Accessors {
    public String value() {
    return "exact";
  }
    public String getValue() {
    return "bean";
  }
    public boolean isReady() {
    return true;
  }
    public static String ignored() {
    return "static";
  }
    public void nothing() { /* Void methods are not getters. */ }
  }

  @Test
  void resolvesNestedAndUnknownComponentTypes() {
    var components = RecordIntrospector.inspect(Types.class).components();
    assertThat(components.get(0).listElementType()).isEqualTo(List.class);
    assertThat(components.get(1).optionalValueGenericType()).isEqualTo(String.class);
    assertThat(components.get(2).listElementType()).isEqualTo(Object.class);
    assertThat(components.get(3).listElementType()).isEqualTo(Object.class);
    assertThat(components.get(3).listElementGenericType()).isEqualTo(Object.class);
    assertThatThrownBy(components::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void preservesRecordValidationFailuresAndRejectsWrongArguments() {
    var metadata = RecordIntrospector.inspect(Validated.class);
    assertThat((Validated) RecordFactory.createRecord(metadata, new Object[] {1})).isEqualTo(new Validated(1));
    assertThatThrownBy(() -> RecordFactory.createRecord(metadata, new Object[] {-1}))
        .isInstanceOf(RecordConstructionException.class).hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("negative");
    assertThatThrownBy(() -> RecordFactory.createRecord(metadata, new Object[] {"wrong"}))
        .isInstanceOf(RecordConstructionException.class).hasMessageContaining("Failed to instantiate");
  }

  @Test
  void selectsExactThenBeanThenBooleanGetters() {
    assertThat(PojoPropertyReader.findGetter(Accessors.class, "value").getName()).isEqualTo("value");
    assertThat(PojoPropertyReader.findGetter(Accessors.class, "ready").getName()).isEqualTo("isReady");
    for (String name : List.of("ignored", "nothing", "missing")) {
      assertThat(PojoPropertyReader.findGetter(Accessors.class, name)).isNull();
    }
  }
}
