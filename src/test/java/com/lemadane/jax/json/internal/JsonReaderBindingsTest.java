package com.lemadane.jax.json.internal;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonReaderBindingsTest {

  public static class InheritedNames {
    String namespaced;

    @JsonProperty
    public void ns$colon$value(String value) {
      namespaced = value;
    }
  }

  public static class SetterChoices extends InheritedNames {
    String chosen;
    String url;

    @JsonProperty
    public void value(String value) {
      chosen = "exact:" + value;
    }

    @JsonProperty
    public void setValue(String value) {
      chosen = "bean:" + value;
    }

    @JsonProperty
    public void setValue(int value) {
      chosen = "number:" + value;
    }

    @JsonProperty
    public void ambiguous(String value) { /* An unused overload must not reject the class. */ }

    @JsonProperty
    public void ambiguous(int value) { /* An unused overload must not reject the class. */ }

    @JsonProperty
    public void setURL(String value) {
      url = value;
    }
  }

  public static class GenericBase<T> {
    @JsonProperty
    public void item(T value) { /* The concrete override must be selected. */ }
  }

  public static class GenericChild extends GenericBase<String> {
    String item;

    @Override
    @JsonProperty
    public void item(String value) {
      item = value;
    }
  }

  public static class GenericCollections {
    List<Optional<Integer>> numbers;
    Optional<List<GenericChild>> children;

    @JsonProperty
    public void setNumbers(List<Optional<Integer>> value) {
      numbers = value;
    }

    @JsonProperty
    public void children(Optional<List<GenericChild>> value) {
      children = value;
    }
  }

  public static class DefaultOnly {
    static int constructions;
    String value;

    DefaultOnly() {
      constructions++;
    }

    @JsonProperty
    public void value(String newValue) {
      value = newValue;
    }
  }

  public static class ConstructorChoices {
    static int constructions;
    final String selected;
    String extra;

    public ConstructorChoices() {
      constructions++;
      selected = "default";
    }

    public ConstructorChoices(String name) {
      constructions++;
      selected = "name:" + name;
    }

    public ConstructorChoices(String name, int age) {
      constructions++;
      selected = "name-and-age:" + name + ":" + age;
    }

    @JsonProperty
    public void extra(String value) {
      extra = value;
    }
  }

  @Test
  void inheritedEncodedNamesAndExactNamesKeepTheirPriorityAfterCacheWarmup() {
    for (int i = 0; i < 2; i++) {
      SetterChoices value = JsonObjectReader.toObject("""
          {"value":"selected","ns:value":"inherited"}
          """, SetterChoices.class);
      assertThat(value.chosen).isEqualTo("exact:selected");
      assertThat(value.namespaced).isEqualTo("inherited");
      assertThat(JsonObjectReader.toObject("""
          {"ns$colon$value":"encoded"}
          """, SetterChoices.class).namespaced).isEqualTo("encoded");
    }
    for (String name : List.of("URL", "uRL", "setURL")) {
      assertThat(JsonObjectReader.toObject("{\"" + name + "\":\"address\"}", SetterChoices.class).url)
          .isEqualTo("address");
    }
  }

  @Test
  void onlyAccessedOverloadsFailAndBeanAmbiguityDoesNotOverrideAnExactSetter() {
    assertThat(JsonObjectReader.toObject("{}", SetterChoices.class)).isNotNull();
    assertThat(JsonObjectReader.toObject("""
        {"value":"works"}
        """, SetterChoices.class).chosen).isEqualTo("exact:works");
    for (String name : List.of("ambiguous", "Value", "setValue")) {
      for (boolean strict : List.of(true, false)) {
        assertThatThrownBy(() -> JsonObjectReader.toObject("{\"" + name + "\":1}", SetterChoices.class, strict))
            .isInstanceOf(JsonMappingException.class).hasMessageContaining("Multiple setters")
            .hasMessageContaining("$." + name);
      }
    }
    assertThatThrownBy(() -> JsonObjectReader.toObject("""
        {"unknown":1}
        """, SetterChoices.class)).isInstanceOf(ExcessDataException.class);
    assertThat(JsonObjectReader.toObject("""
        {"unknown":1,"value":"known"}
        """, SetterChoices.class, false).chosen).isEqualTo("exact:known");
  }

  @Test
  void cachedGenericParameterTypesRetainNestedCollectionsAndIgnoreCompilerBridgeMethods() {
    for (int i = 0; i < 2; i++) {
      GenericCollections value = JsonObjectReader.toObject("""
          {"numbers":[1,null,3],"children":[{"item":"first"},{"item":"second"}]}
          """, GenericCollections.class);
      assertThat(value.numbers).containsExactly(Optional.of(1), Optional.empty(), Optional.of(3));
      assertThat(value.children.orElseThrow()).extracting(child -> child.item).containsExactly("first", "second");
    }
  }

  @Test
  void cachedDefaultConstructorStillCreatesEachInstanceAndPreservesValidationOrder() {
    DefaultOnly.constructions = 0;
    DefaultOnly first = JsonObjectReader.toObject("""
        {"value":"first"}
        """, DefaultOnly.class);
    DefaultOnly second = JsonObjectReader.toObject("{}", DefaultOnly.class);
    assertThat(first).isNotSameAs(second);
    assertThat(first.value).isEqualTo("first");
    assertThat(second.value).isNull();
    assertThat(DefaultOnly.constructions).isEqualTo(2);

    assertThatThrownBy(() -> JsonObjectReader.toObject("""
        {"unknown":1}
        """, DefaultOnly.class)).isInstanceOf(ExcessDataException.class);
    assertThat(DefaultOnly.constructions).isEqualTo(3);
    assertThatThrownBy(() -> JsonObjectReader.toObject("{", DefaultOnly.class)).isInstanceOf(JsonMappingException.class);
    assertThat(DefaultOnly.constructions).isEqualTo(3);
  }

  @Test
  void parameterizedConstructorsKeepPriorityOverDefaultAndAmbiguityPrecedesConstruction() {
    ConstructorChoices.constructions = 0;
    assertThat(JsonObjectReader.toObject("{}", ConstructorChoices.class).selected).isEqualTo("default");
    ConstructorChoices selected = JsonObjectReader.toObject("""
        {"name":"Ada","extra":"remaining"}
        """, ConstructorChoices.class);
    assertThat(selected.selected).isEqualTo("name:Ada");
    assertThat(selected.extra).isEqualTo("remaining");
    assertThat(ConstructorChoices.constructions).isEqualTo(2);

    assertThatThrownBy(() -> JsonObjectReader.toObject("""
        {"name":"Ada","age":37}
        """, ConstructorChoices.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Multiple constructors");
    assertThat(ConstructorChoices.constructions).isEqualTo(2);
    assertThat(JsonObjectReader.toObject("{}", ConstructorChoices.class).selected).isEqualTo("default");
    assertThat(ConstructorChoices.constructions).isEqualTo(3);
  }
}
