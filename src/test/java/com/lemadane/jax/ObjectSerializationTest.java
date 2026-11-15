package com.lemadane.jax;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectSerializationTest {

  public static class Address {
    public String city;
  }

  public static class Person {
    public String name;
    public int age;
    public Address address;
    public static String shared = "ignored";
    String privateValue = "ignored";
  }

  public record RecordEnvelope(Person person) {}

  @DisplayName("Serializes POJOs and nested public fields to JSON")
  @Test
  void serializesPojosAndNestedPublicFieldsToJson() {
    Person person = new Person();
    person.name = "Ada";
    person.age = 37;
    person.address = new Address();
    person.address.city = "London & Paris";

    assertThat(Jax.toJson(person))
        .isEqualTo("{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London & Paris\"}}");
  }

  @DisplayName("Serializes records containing POJOs to JSON")
  @Test
  void serializesRecordsContainingPojosToJson() {
    Person person = new Person();
    person.name = "Ada";
    person.address = new Address();
    person.address.city = "London";

    assertThat(Jax.toJson(new RecordEnvelope(person)))
        .isEqualTo("{\"person\":{\"name\":\"Ada\",\"age\":0,\"address\":{\"city\":\"London\"}}}");
  }

  @DisplayName("Serializes POJOs and nested public fields to XML")
  @Test
  void serializesPojosAndNestedPublicFieldsToXml() {
    Person person = new Person();
    person.name = "Ada & Bob";
    person.age = 37;
    person.address = new Address();
    person.address.city = "London";

    assertThat(Jax.toXml(person))
        .isEqualTo("<Person><name>Ada &amp; Bob</name><age>37</age><address><city>London</city></address></Person>");
  }

  @DisplayName("Serializes records containing POJOs to XML")
  @Test
  void serializesRecordsContainingPojosToXml() {
    Person person = new Person();
    person.name = "Ada";
    person.address = new Address();
    person.address.city = "London";

    assertThat(Jax.toXml(new RecordEnvelope(person)))
        .isEqualTo("<RecordEnvelope><person><name>Ada</name><age>0</age><address><city>London</city></address></person></RecordEnvelope>");
  }

  @DisplayName("Rejects cycles during JSON and XML serialization")
  @Test
  void rejectsObjectCyclesDuringSerialization() {
    CyclicPojo cycle = new CyclicPojo();
    cycle.child = cycle;

    assertThatThrownBy(() -> Jax.toJson(cycle))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Cycle detected");
    assertThatThrownBy(() -> Jax.toXml(cycle))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("Cycle detected");
  }

  public static class CyclicPojo {
    public CyclicPojo child;
  }
}
