package com.lemadane.jax.json.internal;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.TypeConversionException;

import static org.assertj.core.api.Assertions.*;

class JsonObjectReaderHelperTest {
  public static class Types<T> {
    public List<List<Integer>> nested;
    public Optional<List<Integer>> optional;
    public List<?> wildcard;
    public T[] array;
    public Optional raw;
  }

  public static class Named {
    final String value;
    public Named(String display$space$name) {
    value = display$space$name;
  }
  }

  public static class ThrowsConstructor {
    public ThrowsConstructor(String value) {
    throw new IllegalStateException(value);
  }
  }

  public static class ThrowsDefault {
    public ThrowsDefault() {
    throw new IllegalStateException("default failed");
  }
  }

  public abstract static class AbstractTarget {}
  public record RecordTarget(String value) {}

  public static class Setters {
    @JsonProperty
    public void value(String value) {
    throw new IllegalStateException(value);
  }
    @JsonProperty
    public void setValue(String value) { /* Exact setter must take precedence. */ }
    @JsonProperty
    public void ambiguous(String value) { /* Overload fixture. */ }
    @JsonProperty
    public void ambiguous(int value) { /* Overload fixture. */ }
    public void unannotated(String value) { /* Must not be selected. */ }
    @JsonProperty
    public static void ignored(String value) { /* Must not be selected. */ }
  }

  static Type type(String name) throws Exception {
    return Types.class.getField(name).getGenericType();
  }

  static Object read(Class<?> target, Type generic, String json) {
    return JsonObjectReader.readValue(target, generic, JsonParser.parse(json), "$.value", true);
  }

  @Test
  void resolvesNestedRawWildcardAndGenericArrayTypes() throws Exception {
    assertThat(JsonObjectReader.getTypeArgument(type("optional"))).isEqualTo(List.class);
    assertThat(JsonObjectReader.getTypeArgument(type("raw"))).isEqualTo(Object.class);
    assertThat(JsonObjectReader.rawType(type("array"))).isEqualTo(Object[].class);
    assertThat(JsonObjectReader.getTypeArgument(type("wildcard"))).isEqualTo(Object.class);
    assertThat(read(List.class, type("nested"), "[[1,2],[3]]"))
        .isEqualTo(List.of(List.of(1, 2), List.of(3)));
    assertThat(read(Optional.class, type("optional"), "[1,2]"))
        .isEqualTo(Optional.of(List.of(1, 2)));
    assertThat(read(Optional.class, type("optional"), "null")).isEqualTo(Optional.empty());
    assertThat(read(String.class, String.class, "null")).isNull();
    assertThat((int[]) read(int[].class, int[].class, "[2,4]")).containsExactly(2, 4);
    assertThatThrownBy(() -> read(int.class, int.class, "null"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("$.value");
  }

  @Test
  void convertsUntypedScalarsAndImmutableArrays() {
    assertThat(read(Object.class, Object.class, "\"text\"")).isEqualTo("text");
    assertThat(read(Object.class, Object.class, "12.5")).isEqualTo(new BigDecimal("12.5"));
    assertThat(read(Object.class, Object.class, "true")).isEqualTo(true);
    List<?> values = (List<?>) read(Object.class, Object.class, "[null,false,3]");
    assertThat(values).isEqualTo(java.util.Arrays.asList(null, false, new BigDecimal("3")));
    assertThatThrownBy(values::clear).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> read(Object.class, Object.class, "{}"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("declared target type");
    assertThat(read(boolean.class, boolean.class, "true")).isEqualTo(true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"[]", "\"text\"", "42", "true", "null"})
  void rejectsNonObjectRootsWithDescriptions(String input) {
    assertThatThrownBy(() -> JsonObjectReader.toObject(input, Named.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("expected");
    assertThat(JsonObjectReader.describe(JsonParser.parse(input))).startsWith("JSON ");
  }

  @Test
  void rejectsMismatchedCollectionAndScalarShapes() {
    assertThatThrownBy(() -> read(List.class, List.class, "true"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("array");
    assertThatThrownBy(() -> read(int[].class, int[].class, "false"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("array");
    assertThatThrownBy(() -> read(String.class, String.class, "[]"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("JSON array");
    assertThat(JsonObjectReader.describe(JsonParser.parse("{}"))).isEqualTo("JSON object");
  }

  @Test
  void matchesEncodedConstructorNamesAndReportsMissingConstructors() {
    assertThat(JsonObjectReader.toObject("{\"display name\":\"Ada\"}", Named.class).value).isEqualTo("Ada");
    var properties = Map.of("display$space$name", JsonParser.parse("1"));
    assertThat(JsonObjectReader.findPropertyName(properties, "display name")).isEqualTo("display$space$name");
    assertThat(JsonObjectReader.findPropertyName(properties, "absent")).isNull();
    assertThatThrownBy(() -> JsonObjectReader.toObject("{}", Named.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("no-argument constructor");
    assertThatThrownBy(() -> JsonObjectReader.createInstance(AbstractTarget.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("concrete");
    assertThatThrownBy(() -> JsonObjectReader.createInstance(Runnable.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("concrete");
    assertThatThrownBy(() -> JsonObjectReader.createInstance(RecordTarget.class))
        .isInstanceOf(RecordConstructionException.class);
    assertThatThrownBy(() -> JsonObjectReader.toObject("{}", RecordTarget.class))
        .isInstanceOf(RecordConstructionException.class);
  }

  @Test
  void preservesConstructorAndSetterFailureCauses() {
    assertThatThrownBy(() -> JsonObjectReader.toObject("{\"value\":\"bad\"}", ThrowsConstructor.class))
        .isInstanceOf(JsonMappingException.class).hasCauseInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> JsonObjectReader.createInstance(ThrowsDefault.class))
        .isInstanceOf(JsonMappingException.class).hasCauseInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> JsonObjectReader.toObject("{\"value\":\"bad\"}", Setters.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("$.value")
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void selectsOnlyUnambiguousOptedInSetters() {
    assertThat(JsonObjectReader.findSetter(Setters.class, "value", "$").getName()).isEqualTo("value");
    for (String name : List.of("unannotated", "ignored", "", "missing")) {
      assertThat(JsonObjectReader.findSetter(Setters.class, name, "$")).isNull();
    }
    assertThatThrownBy(() -> JsonObjectReader.findSetter(Setters.class, "ambiguous", "$"))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Multiple setters");
  }
  @Test
  void reportsClosedModuleConstructorAccess() {
    assertThatThrownBy(() -> JsonObjectReader.createInstance(Void.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("No accessible no-argument constructor");
  }

}
