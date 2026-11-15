package com.lemadane.jax;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class ReflectionBoundaryTest {
  static class HiddenSetter {
    @JsonProperty
    public void value(String value) { /* Non-public declaring class. */ }
  }

  public static class ConstructorField {
    public final String value;

    public ConstructorField(String value) {
      this.value = "constructed:" + value;
    }
  }

  public static class ConstructorFallback {
    public String value = "default";

    public ConstructorFallback() { /* Fallback when parameter is absent. */ }
    public ConstructorFallback(@XmlAttribute String id) {
      value = id;
    }
  }

  public static class ElementConstructorFallback {
    public String value = "default";

    public ElementConstructorFallback() { /* Fallback when element is absent. */ }
    public ElementConstructorFallback(String value) {
      this.value = value;
    }
  }

  public static class IncompatibleField {
    public java.util.ArrayList<Integer> values;
  }

  public static class AttributeValue {
    @XmlAttribute
    public Object id = new Object() {
      @Override
      public String toString() {
        throw new IllegalArgumentException("attribute conversion failed");
      }
    };
    @XmlAttribute
    public static String ignored = "static";
  }

  public static class StaticProperty {
    public static String ignored = "static";
    public String value = "kept";
  }

  @Test
  void wrapsInaccessibleSetters() {
    assertThatThrownBy(() -> Jax.toObject("{\"value\":\"x\"}", HiddenSetter.class))
        .isInstanceOf(JsonMappingException.class).hasCauseInstanceOf(IllegalAccessException.class);
    assertThatThrownBy(() -> Jax.toObject("<HiddenSetter><value>x</value></HiddenSetter>", HiddenSetter.class))
        .isInstanceOf(XmlMappingException.class).hasCauseInstanceOf(IllegalAccessException.class);
  }

  @Test
  void preservesConstructorBoundFinalFields() {
    ConstructorField value = Jax.toObject("<ConstructorField><value>x</value></ConstructorField>", ConstructorField.class);
    assertThat(value.value).isEqualTo("constructed:x");
  }

  @Test
  void fallsBackWhenConstructorAttributesOrElementsAreAbsent() {
    assertThat(Jax.toObject("<ConstructorFallback/>", ConstructorFallback.class).value).isEqualTo("default");
    assertThat(Jax.toObject("<ElementConstructorFallback/>", ElementConstructorFallback.class).value).isEqualTo("default");
  }

  @Test
  void reportsIncompatibleConcreteCollectionFields() {
    assertThatThrownBy(() -> Jax.toObject("<IncompatibleField><values>1</values></IncompatibleField>", IncompatibleField.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Failed to set public field")
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void wrapsAttributeConversionFailuresAndOmitsStaticProperties() {
    assertThatThrownBy(() -> Jax.toXml(new AttributeValue()))
        .isInstanceOf(XmlMappingException.class).hasCauseInstanceOf(IllegalArgumentException.class);
    assertThat(Jax.toJson(new StaticProperty())).isEqualTo("{\"value\":\"kept\"}");
    assertThat(Jax.toXml(new StaticProperty())).isEqualTo("<StaticProperty><value>kept</value></StaticProperty>");
  }
}
