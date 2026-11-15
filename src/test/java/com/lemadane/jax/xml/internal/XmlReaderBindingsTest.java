package com.lemadane.jax.xml.internal;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.NameEncodingException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlReaderBindingsTest {
  public static class Parent {
    public String name;
  }

  public static class Fields extends Parent {
    public String name;
    public List<String> ns$colon$item;
    public String ns$item;
    @XmlAttribute
    public String id;
  }

  public static class Setters {
    List<Integer> values;

    @JsonProperty
    public void item(List<Integer> value) {
      values = value;
    }

    @JsonProperty
    public void ambiguous(String value) {
      throw new AssertionError("Ambiguous setter must not be invoked");
    }

    @JsonProperty
    public void ambiguous(int value) {
      throw new AssertionError("Ambiguous setter must not be invoked");
    }
  }

  public static class InvalidNames {
    static InvalidNames last;
    @XmlAttribute
    public int id;
    public String good;
    public String bad$uZZZZ$;

    public InvalidNames() {
      last = this;
    }
  }

  public static class GenericSetterParent {
    List<Optional<Integer>> samples;

    @JsonProperty
    public void samples(List<Optional<Integer>> value) {
      samples = value;
    }
  }

  public static class GenericSetters extends GenericSetterParent {
    int[] numbers;
    Optional<List<String>> tags;

    @JsonProperty
    public void setNumbers(int[] value) {
      numbers = value;
    }

    @JsonProperty
    public void ns$colon$tags(Optional<List<String>> value) {
      tags = value;
    }
  }

  @Test
  void inheritedFieldsAndDecodedAliasesStillReceiveTheirValues() {
    Fields value = Jax.toObject("""
        <Fields xmlns:ns="urn:test" id="manifest"><name>same</name><ns:item>cargo</ns:item></Fields>
        """, Fields.class);
    assertThat(value.name).isEqualTo("same");
    assertThat(((Parent) value).name).isEqualTo("same");
    assertThat(value.ns$colon$item).containsExactly("cargo");
    assertThat(value.ns$item).isEqualTo("cargo");
    assertThat(value.id).isEqualTo("manifest");
    assertThatThrownBy(() -> Jax.toObject("""
        <Fields xmlns:ns="urn:test"><ns:item>first</ns:item><ns:item>second</ns:item></Fields>
        """, Fields.class)).isInstanceOf(XmlMappingException.class).hasMessageContaining("non-collection field");
  }

  @Test
  void repeatedSetterValuesKeepOrderAndAmbiguityRemainsDeferred() {
    Setters value = Jax.toObject("<Setters><item>3</item><item>1</item><item>2</item></Setters>", Setters.class);
    assertThat(value.values).containsExactly(3, 1, 2);
    assertThatThrownBy(value.values::clear).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> Jax.toObject("<Setters><ambiguous>1</ambiguous></Setters>", Setters.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Multiple one-argument setters");
    assertThatThrownBy(() -> Jax.toObject("<Setters><unknown>1</unknown></Setters>", Setters.class))
        .isInstanceOf(ExcessDataException.class).hasMessageContaining("unknown");
    Setters partial = Jax.toPartialObject("<Setters><unknown>1</unknown><item>7</item></Setters>", Setters.class);
    assertThat(partial.values).containsExactly(7);
  }

  @Test
  void invalidEncodedFieldsRetainAttributeAndTraversalFailureOrder() {
    assertThatThrownBy(() -> Jax.toObject("<InvalidNames id=\"bad\"/>", InvalidNames.class))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("/@id");
    assertThatThrownBy(() -> Jax.toObject("<InvalidNames id=\"42\"><good>mapped</good></InvalidNames>", InvalidNames.class))
        .isInstanceOf(NameEncodingException.class);
    assertThat(InvalidNames.last.id).isEqualTo(42);
    assertThat(InvalidNames.last.good).isEqualTo("mapped");
    assertThatThrownBy(() -> Jax.toObject("<InvalidNames><unknown/></InvalidNames>", InvalidNames.class))
        .isInstanceOf(NameEncodingException.class);
  }

  @Test
  void cachedInheritedAndEncodedSettersRetainGenericCollectionsAndArrays() {
    for (int i = 0; i < 2; i++) {
      GenericSetters value = Jax.toObject("""
          <GenericSetters xmlns:ns="urn:test">
            <samples>4</samples><samples jax-null="true"/>
            <numbers>3</numbers><numbers>1</numbers>
            <ns:tags><tag>first</tag><tag>second</tag></ns:tags>
          </GenericSetters>
          """, GenericSetters.class);
      assertThat(value.samples).containsExactly(Optional.of(4), Optional.empty());
      assertThat(value.numbers).containsExactly(3, 1);
      assertThat(value.tags.orElseThrow()).containsExactly("first", "second");
      assertThatThrownBy(value.tags.orElseThrow()::clear).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
