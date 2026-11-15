package com.lemadane.jax.xml.internal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.exceptions.TypeConversionException;

import static org.assertj.core.api.Assertions.*;

class XmlReaderHelpersTest {
  public static class Types<T> {
    public Optional<String> text;
    public Optional<Integer> number;
    public Optional<List<String>> list;
    public Optional<String[]> array;
    public Optional<Types> pojo;
    public List<List<Integer>> nested;
    public List<?> wildcard;
    public T[] genericArray;
    @XmlAttribute("id")
    Optional<Integer> id;
    @XmlAttribute
    public String label;
  }

  public static class Setters {
    @JsonProperty
    public void setValue(String value) { /* Setter selection fixture. */ }
    @JsonProperty
    public void ambiguous(String value) { /* Overload fixture. */ }
    @JsonProperty
    public void ambiguous(int value) { /* Overload fixture. */ }
  }

  public static class ThrowsConstructor {
    public ThrowsConstructor(String value) {
    throw new IllegalStateException(value);
  }
  }

  public static class ArrayConstructor {
    final int[] values;
    public ArrayConstructor(int[] values) {
    this.values = values;
  }
  }

  static Type type(String name) throws Exception {
    return Types.class.getField(name).getGenericType();
  }
  static XmlElement element(String xml) {
    return XmlRecordReader.parseXmlDocument(xml);
  }
  static Object read(Class<?> target, Type generic, String xml) {
    return XmlObjectReader.readValue(target, generic, element(xml), "/value", true);
  }

  @Test
  void resolvesGenericTypesAndFallbacks() throws Exception {
    assertThat(XmlObjectReader.typeArgument(type("list"))).isEqualTo(List.class);
    assertThat(XmlObjectReader.typeArgument(type("text"))).isEqualTo(String.class);
    assertThat(XmlObjectReader.typeArgument(List.class)).isEqualTo(Object.class);
    assertThat(XmlObjectReader.typeArgument(type("wildcard"))).isEqualTo(Object.class);
    assertThat(XmlObjectReader.genericTypeArgument(List.class)).isEqualTo(Object.class);
    assertThat(XmlObjectReader.rawType(type("genericArray"))).isEqualTo(Object[].class);
    assertThat(XmlObjectReader.rawType(XmlObjectReader.genericTypeArgument(type("wildcard"))))
        .isEqualTo(Object.class);
  }

  @Test
  void distinguishesOptionalEmptyTextNullAndCollections() throws Exception {
    assertThat(read(Optional.class, type("text"), "<v/>" )).isEqualTo(Optional.of(""));
    assertThat(read(Optional.class, type("text"), "<v jax-null=\"true\"/>" )).isEqualTo(Optional.empty());
    assertThat(read(Optional.class, type("text"), "<v>hello</v>" )).isEqualTo(Optional.of("hello"));
    for (String name : List.of("number", "list", "array", "pojo")) {
      assertThat(read(Optional.class, type(name), "<v/>" )).isEqualTo(Optional.empty());
    }
    assertThat(read(Optional.class, type("list"), "<v jax-empty-list=\"true\"/>"))
        .isEqualTo(Optional.of(List.of()));
    assertThat(read(List.class, type("nested"), "<v><item><i>1</i><i>2</i></item></v>"))
        .isEqualTo(List.of(List.of(1, 2)));
    assertThat((int[]) read(int[].class, int[].class, "<v><i>1</i><i>2</i></v>"))
        .containsExactly(1, 2);
  }

  @Test
  void mapsEmptyAndNullScalarsAndRejectsPrimitiveNulls() {
    assertThat(read(String.class, String.class, "<v/>" )).isEqualTo("");
    assertThat(read(Integer.class, Integer.class, "<v/>" )).isNull();
    assertThat(read(String.class, String.class, "<v jax-null=\"true\"/>" )).isNull();
    for (String xml : List.of("<v/>", "<v jax-null=\"true\"/>")) {
      assertThatThrownBy(() -> read(int.class, int.class, xml))
          .isInstanceOf(TypeConversionException.class).hasMessageContaining("/value");
    }
    assertThatThrownBy(() -> read(String.class, String.class, "<v jax-empty-list=\"true\"/>"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("Empty-list marker");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "<v jax-null=\"true\" jax-empty-list=\"true\"/>",
      "<v jax-null=\"false\"/>", "<v jax-empty-list=\"false\"/>",
      "<v jax-null=\"true\">text</v>", "<v jax-empty-list=\"true\"><child/></v>"
  })
  void rejectsInvalidMarkers(String xml) {
    assertThatThrownBy(() -> XmlObjectReader.validateMarkerAttributes(element(xml), "/value"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("/value");
  }

  @Test
  void readsEmptyCollectionsAndReturnsImmutableLists() throws Exception {
    XmlElement empty = element("<v jax-empty-list=\"true\"/>");
    assertThat((int[]) XmlObjectReader.readArray(int.class, List.of(empty), "/v", true)).isEmpty();
    List<?> list = (List<?>) XmlObjectReader.readList(List.class, List.of(empty), "/v", true);
    assertThat(list).isEmpty();
    List<?> nested = (List<?>) read(List.class, type("nested"), "<v><i><n>1</n></i></v>");
    assertThatThrownBy(nested::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void resolvesAndSetsPrivateOptionalAttributes() throws Exception {
    var field = Types.class.getDeclaredField("id");
    assertThat(XmlObjectReader.xmlAttributeName(field)).isEqualTo("id");
    assertThat(XmlObjectReader.xmlAttributeName(Types.class.getField("label"))).isEqualTo("label");
    Object value = XmlObjectReader.readAttributeValue(Optional.class, field.getGenericType(), "42", "/@id");
    Types<?> instance = new Types<>();
    XmlObjectReader.setField(field, instance, value, Types.class, "/@id");
    assertThat(instance.id).contains(42);
    assertThatThrownBy(() -> XmlObjectReader.setField(field, instance, 42, Types.class, "/@id"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("/@id");
  }

  @Test
  void handlesConstructorArraysAndFailures() {
    ArrayConstructor instance = XmlObjectReader.toObject(
        "<ArrayConstructor><values>2</values><values>4</values></ArrayConstructor>", ArrayConstructor.class);
    assertThat(instance.values).containsExactly(2, 4);
    assertThatThrownBy(() -> XmlObjectReader.toObject(
        "<ThrowsConstructor><value>bad</value></ThrowsConstructor>", ThrowsConstructor.class))
        .isInstanceOf(XmlMappingException.class).hasCauseInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> XmlObjectReader.createInstance(ArrayConstructor.class, "/root"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("/root");
    assertThatThrownBy(() -> XmlObjectReader.toObject("<wrong/>", Types.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("does not match");
    assertThatThrownBy(() -> XmlObjectReader.toObject("<Runnable/>", Runnable.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("concrete");
  }

  @Test
  void selectsBeanSettersAndRejectsAmbiguity() {
    assertThat(XmlObjectReader.findSetter(Setters.class, "value", "/").getName()).isEqualTo("setValue");
    assertThat(XmlObjectReader.findSetter(Setters.class, "missing", "/")).isNull();
    assertThatThrownBy(() -> XmlObjectReader.findSetter(Setters.class, "ambiguous", "/"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Multiple");
  }

  @Test
  void elementViewsAreImmutableAndTextPreservesOrder() {
    XmlElement root = element("<root id=\"1\">before<child>inside</child>after</root>");
    assertThat(root.textContent()).isEqualTo("beforeinsideafter");
    assertThat(root.hasNonWhitespaceText()).isTrue();
    assertThat(root.isEmpty()).isFalse();
    assertThatThrownBy(() -> root.attributes().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> root.children().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> root.content().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThat(element("<root> \n </root>").hasNonWhitespaceText()).isFalse();
    assertThat(element("<root><child/></root>").hasNonWhitespaceText()).isFalse();
  }
}
