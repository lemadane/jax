package com.lemadane.jax.json;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.TypeConversionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonObjectMappingTest {

  public static class Person {
    String name;
    int age;

    public String name() {
      return name;
    }

    @JsonProperty
    public void name(String value) {
      name = value;
    }

    public int age() {
      return age;
    }

    @JsonProperty
    public void age(int value) {
      age = value;
    }
  }

  public static class Team {
    List<Person> members;

    public List<Person> members() {
      return members;
    }

    @JsonProperty
    public void members(List<Person> value) {
      members = value;
    }
  }

  public static class NamedProperty {
    String displayName;

    public String displayName() {
      return displayName;
    }

    @JsonProperty
    public void setDisplayName(String value) {
      displayName = value;
    }
  }

  public static class EncodedProperty {
    String display$space$name;

    public String display$space$name() {
      return display$space$name;
    }

    @JsonProperty
    public void display$space$name(String value) {
      display$space$name = value;
    }
  }

  public static class XmlAddress {
    public String city;
  }

  public static class XmlPerson {
    public String name;
    public int age;
    public XmlAddress address;
  }

  public static class XmlTeam {
    public List<XmlPerson> members;
  }

  public static class PersonWithXmlAttributes {
    @XmlAttribute
    String id;

    @XmlAttribute("xml:version")
    int version;

    public String name;

    public String id() {
      return id;
    }

    public int version() {
      return version;
    }
  }

  public static class ConstructorWithXmlAttribute {
    final String id;
    final String name;

    public ConstructorWithXmlAttribute(@XmlAttribute String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String id() {
      return id;
    }

    public String name() {
      return name;
    }
  }

  public static class PartialAddress {
    public String city;

    @JsonProperty
    public void city(String value) {
      city = value;
    }
  }

  public static class PartialPerson {
    String name;
    PartialAddress address;

    public String name() {
      return name;
    }

    @JsonProperty
    public void name(String value) {
      name = value;
    }

    public PartialAddress address() {
      return address;
    }

    @JsonProperty
    public void address(PartialAddress value) {
      address = value;
    }
  }

  public static class PartialXmlPerson {
    public String name;
    public PartialAddress address;
  }

  public static class fxg$NamespacedPerson {
    public String fxg$first$hyphen$name;
    public fxg$NamespacedAddress fxg$address;
  }

  public static class fxg$NamespacedAddress {
    public String fxg$city;
  }

  public static class ConstructorAddress {
    final String city;

    public ConstructorAddress(String city) {
      this.city = city;
    }

    public String city() {
      return city;
    }
  }

  public static class ConstructorPerson {
    final String name;
    final int age;
    final ConstructorAddress address;

    public ConstructorPerson(String name, int age, ConstructorAddress address) {
      this.name = name;
      this.age = age;
      this.address = address;
    }

    public String name() {
      return name;
    }

    public int age() {
      return age;
    }

    public ConstructorAddress address() {
      return address;
    }
  }

  public static class ConstructorTeam {
    final List<ConstructorPerson> members;

    public ConstructorTeam(List<ConstructorPerson> members) {
      this.members = members;
    }

    public List<ConstructorPerson> members() {
      return members;
    }
  }

  public static class AmbiguousConstructor {
    public AmbiguousConstructor(String name) {}

    public AmbiguousConstructor(String name, int age) {}
  }

  public static class UntypedValue {
    Object value;

    public Object value() {
      return value;
    }

    @JsonProperty
    public void value(Object value) {
      this.value = value;
    }
  }

  @DisplayName("Maps JSON properties through property-named setters")
  @Test
  void mapsJsonToMutableClassThroughPropertyNamedSetters() {
    Person person = Jax.toObject("""
        {"name":"Ada","age":37}
        """, Person.class);

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
  }

  @DisplayName("Maps JSON properties to record components")
  @Test
  void mapsJsonToRecord() {
    PersonRecord person = Jax.toObject("{\"name\":\"Ada\",\"age\":37}", PersonRecord.class);

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
  }

  @DisplayName("Maps untyped JSON arrays and rejects untyped objects")
  @Test
  void mapsUntypedJsonArraysAndRejectsUntypedObjects() {
    UntypedValue arrayValue = Jax.toObject(
        "{\"value\":[null,\"text\",17,true]}",
        UntypedValue.class
    );
    assertThat(arrayValue.value()).isEqualTo(java.util.Arrays.asList(
        null,
        "text",
        new java.math.BigDecimal("17"),
        true
    ));

    assertThatThrownBy(() -> Jax.toObject("{\"value\":{\"nested\":true}}", UntypedValue.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("without a declared target type");
  }

  @DisplayName("Maps JSON to nested constructor-bound POJOs")
  @Test
  void mapsJsonToConstructorBoundNestedPojo() {
    ConstructorPerson person = Jax.toObject(
        "{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\"}}",
        ConstructorPerson.class
    );

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
    assertThat(person.address().city()).isEqualTo("London");
  }

  @DisplayName("Maps JSON arrays to constructor-bound list properties")
  @Test
  void mapsJsonArrayToConstructorBoundList() {
    ConstructorTeam team = Jax.toObject(
        "{\"members\":[{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\"}}]}",
        ConstructorTeam.class
    );

    assertThat(team.members()).hasSize(1);
    assertThat(team.members().get(0).name()).isEqualTo("Ada");
  }

  @DisplayName("Rejects ambiguous matching constructors")
  @Test
  void rejectsAmbiguousMatchingConstructors() {
    assertThatThrownBy(() -> Jax.toObject("{\"name\":\"Ada\",\"age\":37}", AmbiguousConstructor.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Multiple constructors match");
  }

  @DisplayName("Maps XML to POJOs with nested public fields")
  @Test
  void mapsXmlToPojoWithNestedPublicFields() {
    XmlPerson person = Jax.toObject(
        "<XmlPerson><name>Ada</name><age>37</age><address><city>London</city></address></XmlPerson>",
        XmlPerson.class
    );

    assertThat(person.name).isEqualTo("Ada");
    assertThat(person.age).isEqualTo(37);
    assertThat(person.address.city).isEqualTo("London");
  }

  @DisplayName("Maps XML attributes to per-instance POJO fields")
  @Test
  void mapsXmlAttributesToPojoInstanceFields() {
    PersonWithXmlAttributes first = Jax.toObject(
        "<PersonWithXmlAttributes id=\"A1\" xml:version=\"1\"><name>Ada</name></PersonWithXmlAttributes>",
        PersonWithXmlAttributes.class
    );
    PersonWithXmlAttributes second = Jax.toObject(
        "<PersonWithXmlAttributes id=\"B2\" xml:version=\"2\"><name>Lin</name></PersonWithXmlAttributes>",
        PersonWithXmlAttributes.class
    );

    assertThat(first.id()).isEqualTo("A1");
    assertThat(first.version()).isEqualTo(1);
    assertThat(first.name).isEqualTo("Ada");
    assertThat(second.id()).isEqualTo("B2");
    assertThat(second.version()).isEqualTo(2);
  }

  @DisplayName("Maps XML attributes to annotated constructor parameters")
  @Test
  void mapsXmlAttributesToAnnotatedConstructorParameters() {
    ConstructorWithXmlAttribute person = Jax.toObject(
        "<ConstructorWithXmlAttribute id=\"A1\"><name>Ada</name></ConstructorWithXmlAttribute>",
        ConstructorWithXmlAttribute.class
    );

    assertThat(person.id()).isEqualTo("A1");
    assertThat(person.name()).isEqualTo("Ada");
  }

  @DisplayName("Serializes annotated POJO fields as XML attributes")
  @Test
  void serializesAnnotatedPojoFieldsAsXmlAttributes() {
    PersonWithXmlAttributes person = Jax.toObject(
        "<PersonWithXmlAttributes id=\"A1\" xml:version=\"1\"><name>Ada</name></PersonWithXmlAttributes>",
        PersonWithXmlAttributes.class
    );

    assertThat(Jax.toXml(person))
        .isEqualTo("<PersonWithXmlAttributes id=\"A1\" xml:version=\"1\"><name>Ada</name></PersonWithXmlAttributes>");
  }

  @DisplayName("Maps XML to nested constructor-bound POJOs")
  @Test
  void mapsXmlToConstructorBoundNestedPojo() {
    ConstructorPerson person = Jax.toObject(
        "<ConstructorPerson><name>Ada</name><age>37</age><address><city>London</city></address></ConstructorPerson>",
        ConstructorPerson.class
    );

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
    assertThat(person.address().city()).isEqualTo("London");
  }

  @DisplayName("Maps repeated XML elements to constructor-bound lists")
  @Test
  void mapsRepeatedXmlElementsToConstructorBoundList() {
    ConstructorTeam team = Jax.toObject(
        "<ConstructorTeam><members><name>Ada</name><age>37</age><address><city>London</city></address></members></ConstructorTeam>",
        ConstructorTeam.class
    );

    assertThat(team.members()).hasSize(1);
    assertThat(team.members().get(0).name()).isEqualTo("Ada");
  }

  @DisplayName("Maps XML to records through the unified API")
  @Test
  void mapsXmlToRecordThroughUnifiedApi() {
    PersonRecord person = Jax.toObject("<PersonRecord><name>Ada</name><age>37</age></PersonRecord>", PersonRecord.class);

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
  }

  @DisplayName("Maps repeated XML elements to nested POJO lists")
  @Test
  void mapsRepeatedXmlElementsToNestedPojoList() {
    XmlTeam team = Jax.toObject(
        "<XmlTeam><members><name>Ada</name><age>37</age></members>"
            + "<members><name>Lin</name><age>42</age></members></XmlTeam>",
        XmlTeam.class
    );

    assertThat(team.members).extracting(member -> member.name).containsExactly("Ada", "Lin");
  }

  @DisplayName("Detects JSON after leading whitespace")
  @Test
  void autoDetectsJsonAfterLeadingWhitespace() {
    PersonRecord person = Jax.toObject("  \n {\"name\":\"Ada\",\"age\":37}", PersonRecord.class);

    assertThat(person.name()).isEqualTo("Ada");
  }

  @DisplayName("Maps nested JSON objects and lists")
  @Test
  void mapsNestedObjectsAndLists() {
    Team team = Jax.toObject("""
        {"members":[{"name":"Ada","age":37},{"name":"Lin","age":42}]}
        """, Team.class);

    assertThat(team.members()).extracting(Person::name).containsExactly("Ada", "Lin");
  }

  @DisplayName("Supports JavaBean setters and encoded property names")
  @Test
  void supportsJavaBeanSettersAndEncodedPropertyNames() {
    NamedProperty javaBean = Jax.toObject("{\"displayName\":\"Main\"}", NamedProperty.class);
    EncodedProperty encoded = Jax.toObject("{\"display name\":\"Main\"}", EncodedProperty.class);

    assertThat(javaBean.displayName()).isEqualTo("Main");
    assertThat(encoded.display$space$name()).isEqualTo("Main");
  }

  @DisplayName("Leaves missing POJO properties at Java defaults")
  @Test
  void leavesMissingPropertiesAtTheirJavaDefaults() {
    Person person = Jax.toObject("{\"name\":\"Ada\"}", Person.class);

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isZero();
  }

  @DisplayName("Rejects unknown JSON properties and invalid values")
  @Test
  void rejectsUnknownPropertiesAndInvalidValues() {
    assertThatThrownBy(() -> Jax.toObject("{\"unknown\":1}", Person.class))
        .isInstanceOf(ExcessDataException.class);
    assertThatThrownBy(() -> Jax.toObject("{\"age\":\"old\"}", Person.class))
        .isInstanceOf(TypeConversionException.class);
    assertThatThrownBy(() -> Jax.toObject("[]", Person.class))
        .isInstanceOf(JsonMappingException.class);
  }

  @DisplayName("Maps partial JSON objects and ignores unknown nested properties")
  @Test
  void mapsPartialJsonObjectAndIgnoresUnknownNestedProperties() {
    PartialPerson person = Jax.toPartialObject(
        "{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\",\"postcode\":\"N1\"},\"ignored\":true}",
        PartialPerson.class
    );

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.address().city).isEqualTo("London");
  }

  @DisplayName("Maps partial XML objects and ignores unknown nested elements")
  @Test
  void mapsPartialXmlObjectAndIgnoresUnknownNestedElements() {
    PartialXmlPerson person = Jax.toPartialObject(
        "<PartialXmlPerson><name>Ada</name><age>37</age><address><city>London</city><postcode>N1</postcode></address></PartialXmlPerson>",
        PartialXmlPerson.class
    );

    assertThat(person.name).isEqualTo("Ada");
    assertThat(person.address.city).isEqualTo("London");
  }

  @DisplayName("Maps colon-qualified XML elements to dollar-encoded POJO names")
  @Test
  void mapsColonQualifiedXmlElementsToDollarEncodedPojoNames() {
    fxg$NamespacedPerson person = Jax.toObject(
        "<fxg:NamespacedPerson xmlns:fxg=\"urn:example\">"
            + "<fxg:first-name>Ada</fxg:first-name>"
            + "<fxg:address><fxg:city>London</fxg:city></fxg:address>"
            + "</fxg:NamespacedPerson>",
        fxg$NamespacedPerson.class
    );

    assertThat(person.fxg$first$hyphen$name).isEqualTo("Ada");
    assertThat(person.fxg$address.fxg$city).isEqualTo("London");
  }

  @DisplayName("Ignores unknown colon-qualified elements in partial XML mapping")
  @Test
  void ignoresUnknownColonQualifiedElementsInPartialXmlMapping() {
    fxg$NamespacedPerson person = Jax.toPartialObject(
        "<fxg:NamespacedPerson xmlns:fxg=\"urn:example\">"
            + "<fxg:first-name>Ada</fxg:first-name>"
            + "<fxg:age>37</fxg:age>"
            + "<fxg:address><fxg:city>London</fxg:city><fxg:postcode>N1</fxg:postcode></fxg:address>"
            + "</fxg:NamespacedPerson>",
        fxg$NamespacedPerson.class
    );

    assertThat(person.fxg$first$hyphen$name).isEqualTo("Ada");
    assertThat(person.fxg$address.fxg$city).isEqualTo("London");
  }

  @DisplayName("Maps partial JSON objects to records")
  @Test
  void mapsPartialObjectToRecord() {
    PersonRecord person = Jax.toPartialObject(
        "{\"name\":\"Ada\",\"age\":37,\"unknown\":true}",
        PersonRecord.class
    );

    assertThat(person.name()).isEqualTo("Ada");
    assertThat(person.age()).isEqualTo(37);
  }

  public record PersonRecord(String name, int age) {}
}
