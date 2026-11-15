package com.lemadane.jax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HardeningRegressionTest {

  record NestedLists(List<List<Integer>> values) {}

  record IntArray(int[] values) {}

  record DoubleValue(double value) {}

  record StringValues(List<String> values) {}
  record BigIntegerValue(java.math.BigInteger value) {}
  record OptionalValue(java.util.Optional<String> value) {}
  record ScalarValue(String value) {}

  public static class DangerousPojo {
    static boolean deleteCalled;

    public void delete(String value) {
      deleteCalled = true;
    }
  }

  public static class AnnotatedSetterPojo {
    String name;

    @JsonProperty
    public void name(String value) {
      name = value;
    }
  }

  public static class DangerousXmlPojo {
    static boolean deleteCalled;

    public void delete(String value) {
      deleteCalled = true;
    }
  }

  public static class XmlAttributePojo {
    @XmlAttribute
    public String data;
    public String text;
  }

  public static class NumericXmlAttributePojo {
    @XmlAttribute
    public java.math.BigInteger value;
  }

  @DisplayName("Serializes inferred Document as a JSON object")
  @Test
  void serializesDocumentAndDetectsMapCycles() {
    Document document = Jax.toDocument("{\"name\":\"Ada\",\"items\":[1,2]}");

    assertThat(Jax.toJson(document)).isEqualTo("{\"name\":\"Ada\",\"items\":[1,2]}");

    Map<String, Object> cyclic = new HashMap<>();
    cyclic.put("self", cyclic);
    assertThatThrownBy(() -> Jax.toJson(cyclic))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Cycle");

    Map<Object, Object> invalidKeys = new HashMap<>();
    invalidKeys.put(1, "value");
    assertThatThrownBy(() -> Jax.toJson(invalidKeys))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("keys must be strings");
  }

  @DisplayName("Preserves nested JSON generic element types")
  @Test
  void preservesNestedGenericTypesAndMapsArrays() {
    NestedLists nested = Jax.toObject("{\"values\":[[1]]}", NestedLists.class);
    IntArray array = Jax.toObject("{\"values\":[1,2]}", IntArray.class);

    assertThat(nested.values().get(0).get(0)).isInstanceOf(Integer.class).isEqualTo(1);
    assertThat(array.values()).containsExactly(1, 2);
    String nestedXml = Jax.toXml(nested);
    assertThat(nestedXml).isEqualTo("<NestedLists><values><values>1</values></values></NestedLists>");
    assertThat(Jax.toObject(nestedXml, NestedLists.class).values()).containsExactly(List.of(1));
    assertThat(Jax.toObject(Jax.toXml(array), IntArray.class).values()).containsExactly(1, 2);
  }

  @DisplayName("Rejects JSON setters without explicit opt-in")
  @Test
  void invokesOnlyAnnotatedJsonSetters() {
    DangerousPojo.deleteCalled = false;
    assertThatThrownBy(() -> Jax.toObject("{\"delete\":\"all\"}", DangerousPojo.class))
        .isInstanceOf(ExcessDataException.class);
    assertThat(DangerousPojo.deleteCalled).isFalse();

    AnnotatedSetterPojo annotated = Jax.toObject("{\"name\":\"Ada\"}", AnnotatedSetterPojo.class);
    assertThat(annotated.name).isEqualTo("Ada");

    DangerousXmlPojo.deleteCalled = false;
    assertThatThrownBy(
        () -> Jax.toObject("<DangerousXmlPojo><delete>all</delete></DangerousXmlPojo>", DangerousXmlPojo.class)
    ).isInstanceOf(ExcessDataException.class);
    assertThat(DangerousXmlPojo.deleteCalled).isFalse();
  }

  @DisplayName("Rejects excessive XML nesting")
  @Test
  void rejectsExcessiveXmlNesting() {
    StringBuilder xml = new StringBuilder();
    for (int i = 0; i < 257; i++) {
      xml.append("<node>");
    }
    for (int i = 0; i < 257; i++) {
      xml.append("</node>");
    }

    assertThatThrownBy(() -> Jax.toObject(xml.toString(), StringValues.class))
        .isInstanceOf(XmlMappingException.class);
  }

  @DisplayName("Preserves XML attribute whitespace and rejects invalid characters")
  @Test
  void preservesXmlAttributeWhitespaceAndRejectsInvalidCharacters() {
    XmlAttributePojo source = new XmlAttributePojo();
    source.data = "tab\tline\nreturn\r";
    source.text = "valid";

    String xml = Jax.toXml(source);
    XmlAttributePojo parsed = Jax.toObject(xml, XmlAttributePojo.class);

    assertThat(parsed.data).isEqualTo(source.data);
    assertThatThrownBy(() -> Jax.toXml(new StringValues(List.of("bad\u0000value"))))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("XML 1.0");
  }

  @DisplayName("Round trips empty and null XML list values")
  @Test
  void roundTripsEmptyAndNullXmlListValues() {
    StringValues empty = new StringValues(List.of());
    StringValues withNull = new StringValues(new ArrayList<>(java.util.Arrays.asList("first", null)));

    assertThat(Jax.toObject(Jax.toXml(empty), StringValues.class).values()).isEmpty();
    assertThat(Jax.toObject(Jax.toXml(withNull), StringValues.class).values())
        .containsExactly("first", null);

    OptionalValue optional = new OptionalValue(java.util.Optional.empty());
    String optionalXml = Jax.toXml(optional);
    assertThat(optionalXml).isEqualTo("<OptionalValue><value jax-null=\"true\"/></OptionalValue>");
    assertThat(Jax.toObject(optionalXml, OptionalValue.class)).isEqualTo(optional);
  }

  @DisplayName("Rejects XML collection markers with contradictory content")
  @Test
  void rejectsContradictoryXmlMarkers() {
    assertThatThrownBy(() -> Jax.toObject(
        "<StringValues><values jax-empty-list=\"true\">unexpected</values></StringValues>",
        StringValues.class
    )).isInstanceOf(XmlMappingException.class).hasMessageContaining("cannot have content");
    assertThatThrownBy(() -> Jax.toObject(
        "<StringValues><values jax-null=\"true\">unexpected</values></StringValues>",
        StringValues.class
    )).isInstanceOf(XmlMappingException.class).hasMessageContaining("cannot have content");
    assertThatThrownBy(() -> Jax.toObject(
        "<ScalarValue><value jax-null=\"true\">unexpected</value></ScalarValue>",
        ScalarValue.class
    )).isInstanceOf(XmlMappingException.class).hasMessageContaining("cannot have content");
  }

  @DisplayName("Rejects property names that are invalid XML names")
  @Test
  void rejectsInvalidXmlElementNames() {
    record InvalidXmlName(String $u0020$bad) {}

    assertThatThrownBy(() -> Jax.toXml(new InvalidXmlName("value")))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("Invalid XML name");
  }

  @DisplayName("Rejects non-finite floating conversion and extreme numeric expansion")
  @Test
  void rejectsUnboundedNumericValues() {
    assertThatThrownBy(() -> Jax.toObject("{\"value\":1e999}", DoubleValue.class))
        .isInstanceOf(TypeConversionException.class);
    assertThatThrownBy(() -> Jax.toObject("{\"value\":1e100000000}", BigIntegerValue.class))
        .isInstanceOf(JsonMappingException.class);
    assertThatThrownBy(() -> Jax.toObject("{\"value\":\"1e100000000\"}", BigIntegerValue.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("exponent");
    assertThatThrownBy(() -> Jax.toObject(
        "<BigIntegerValue><value>1e100000000</value></BigIntegerValue>",
        BigIntegerValue.class
    )).isInstanceOf(TypeConversionException.class).hasMessageContaining("exponent");
    assertThatThrownBy(() -> Jax.toObject(
        "<NumericXmlAttributePojo value=\"1e100000000\"/>",
        NumericXmlAttributePojo.class
    )).isInstanceOf(TypeConversionException.class).hasMessageContaining("exponent");
    assertThatThrownBy(
        () -> Jax.toObject("{\"value\":\"" + " ".repeat(1025) + "1\"}", BigIntegerValue.class)
    ).isInstanceOf(TypeConversionException.class).hasMessageContaining("maximum length");
  }

  @DisplayName("Rejects Unicode digits in exponents that exceed the limit")
  @Test
  void rejectsUnicodeDigitExponents() {
    // Arabic-Indic digits ١٠٠٠١ (equivalent to 10001)
    assertThatThrownBy(() -> Jax.toObject("{\"value\":\"1e١٠٠٠١\"}", BigIntegerValue.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("exponent");
    // Extended Arabic-Indic digits
    assertThatThrownBy(() -> Jax.toObject("{\"value\":\"1e۱۰۰۰۱\"}", BigIntegerValue.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("exponent");
  }

  @DisplayName("Preserves deeply nested generic types through round-trip")
  @Test
  void preservesDeeplyNestedGenericTypes() {
    record TripleNested(List<List<List<Integer>>> values) {}
    
    TripleNested original = new TripleNested(List.of(
        List.of(List.of(1, 2), List.of(3)),
        List.of(List.of(4))
    ));
    
    String json = Jax.toJson(original);
    TripleNested parsed = Jax.toObject(json, TripleNested.class);
    
    assertThat(parsed.values()).hasSize(2);
    assertThat(parsed.values().get(0)).hasSize(2);
    assertThat(parsed.values().get(0).get(0)).containsExactly(1, 2);
    assertThat(parsed.values().get(0).get(1)).containsExactly(3);
    assertThat(parsed.values().get(1).get(0)).containsExactly(4);
    assertThat(parsed).isEqualTo(original);
  }

  @DisplayName("Distinguishes Optional.empty() from Optional.of(\"\") in XML round-trip")
  @Test
  void distinguishesOptionalEmptyFromEmptyString() {
    record OptionalString(Optional<String> value) {}
    
    // Optional.empty() should use jax-null marker in XML
    OptionalString empty = new OptionalString(Optional.empty());
    String emptyXml = Jax.toXml(empty);
    System.out.println("Empty XML: " + emptyXml);
    OptionalString parsedEmpty = Jax.toObject(emptyXml, OptionalString.class);
    System.out.println("Parsed empty: " + parsedEmpty);
    assertThat(parsedEmpty).isEqualTo(empty);
    
    // Optional.of("") should produce empty element without marker in XML
    OptionalString emptyString = new OptionalString(Optional.of(""));
    String emptyStringXml = Jax.toXml(emptyString);
    System.out.println("EmptyString XML: " + emptyStringXml);
    OptionalString parsedEmptyString = Jax.toObject(emptyStringXml, OptionalString.class);
    System.out.println("Parsed emptyString: " + parsedEmptyString);
    assertThat(parsedEmptyString).isEqualTo(emptyString);
  }

  @DisplayName("Preserves Optional collections and records in XML round-trip")
  @Test
  void preservesOptionalCollectionsAndRecords() {
    record OptionalList(Optional<List<String>> value) {}
    record OptionalRecord(Optional<StringValues> value) {}

    OptionalList emptyOptional = new OptionalList(Optional.empty());
    OptionalList emptyList = new OptionalList(Optional.of(List.of()));
    OptionalList listWithItems = new OptionalList(Optional.of(List.of("a", "b")));

    assertThat(Jax.toObject(Jax.toXml(emptyOptional), OptionalList.class)).isEqualTo(emptyOptional);
    assertThat(Jax.toObject(Jax.toXml(emptyList), OptionalList.class)).isEqualTo(emptyList);
    assertThat(Jax.toObject(Jax.toXml(listWithItems), OptionalList.class)).isEqualTo(listWithItems);

    OptionalRecord emptyRecordOptional = new OptionalRecord(Optional.empty());
    OptionalRecord emptyRecord = new OptionalRecord(Optional.of(new StringValues(List.of())));
    OptionalRecord recordWithItems = new OptionalRecord(Optional.of(new StringValues(List.of("x"))));

    assertThat(Jax.toObject(Jax.toXml(emptyRecordOptional), OptionalRecord.class)).isEqualTo(emptyRecordOptional);
    assertThat(Jax.toObject(Jax.toXml(emptyRecord), OptionalRecord.class)).isEqualTo(emptyRecord);
    assertThat(Jax.toObject(Jax.toXml(recordWithItems), OptionalRecord.class)).isEqualTo(recordWithItems);
  }
}

