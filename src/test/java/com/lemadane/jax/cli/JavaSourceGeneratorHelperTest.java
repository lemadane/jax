package com.lemadane.jax.cli;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.exceptions.DataMappingException;
import com.lemadane.jax.json.internal.JsonParser;
import com.lemadane.jax.naming.DataNameCodec;

import static org.assertj.core.api.Assertions.*;

class JavaSourceGeneratorHelperTest {
  @ParameterizedTest
  @CsvSource({"true,boolean", "42,int", "2147483648,long", "-2147483649,long",
      "9223372036854775808,java.math.BigInteger", "1.25,java.math.BigDecimal", "null,String"})
  void infersJsonScalarTypesAtNumericBoundaries(String input, String expected) {
    assertThat(JavaSourceGenerator.jsonType("Value", JsonParser.parse(input)).scalar).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"true,boolean", "false,boolean", "42,int", "2147483648,long", "1.25,java.math.BigDecimal",
      "hello,String", "01,String"})
  void infersXmlScalarTypes(String input, String expected) {
    assertThat(JavaSourceGenerator.scalarFromText(input).scalar).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "[1,2.5]|java.math.BigDecimal", "[2.5,1]|java.math.BigDecimal",
      "[1,2147483648]|long", "[2147483648,1]|long",
      "[1,9223372036854775808]|java.math.BigInteger",
      "[9223372036854775808,1]|java.math.BigInteger",
      "[9223372036854775808,0.125]|java.math.BigDecimal",
      "[null,1,2.5,null]|java.math.BigDecimal", "[null,null]|String",
      "[true,false]|boolean", "[1,2,3]|int"
  })
  void infersArrayTypesFromEveryNonNullElement(String input, String expected) {
    var array = (JsonParser.JsonNode.JsonArrayNode) JsonParser.parse(input);
    assertThat(JavaSourceGenerator.mergeValues("Value", array.elements()).scalar).isEqualTo(expected);
  }

  @Test
  void encodesKeywordsAndEscapesJavaStrings() {
    assertThat(JavaSourceGenerator.JAVA_KEYWORDS).contains("private").doesNotContain("");
    for (String keyword : JavaSourceGenerator.JAVA_KEYWORDS) {
      String name = JavaSourceGenerator.javaName(keyword);
      assertThat(name).isNotEqualTo(keyword);
      assertThat(DataNameCodec.decode(name)).isEqualTo(keyword);
    }
    assertThat(JavaSourceGenerator.javaName("")).isEqualTo("value");
    assertThat(JavaSourceGenerator.capitalize("")).isEqualTo("Value");
    assertThat(JavaSourceGenerator.capitalize("person")).isEqualTo("Person");
    assertThat(JavaSourceGenerator.javaString("a\\b\"c")).isEqualTo("a\\\\b\\\"c");
    assertThat(JavaSourceGenerator.qualifiedName(null, "name")).isEqualTo("name");
    assertThat(JavaSourceGenerator.qualifiedName("", "name")).isEqualTo("name");
    assertThat(JavaSourceGenerator.qualifiedName("p", "name")).isEqualTo("p:name");
  }

  @Test
  void boxesEveryPrimitiveListElementType() {
    Map<String, String> types = Map.of("boolean", "Boolean", "byte", "Byte", "short", "Short",
        "int", "Integer", "long", "Long", "float", "Float", "double", "Double", "char", "Character");
    types.forEach((primitive, boxed) -> {
      var field = new JavaSourceGenerator.FieldSchema("items", "items");
      field.type = JavaSourceGenerator.TypeSchema.scalar(primitive);
      field.list = true;
      assertThat(field.typeName()).isEqualTo("List<" + boxed + ">");
    });
    assertThat(JavaSourceGenerator.boxed("String")).isEqualTo("String");
  }

  @Test
  void mergesObjectFieldsAndRejectsConflictingExternalNames() {
    var merged = JavaSourceGenerator.mergeValues("Item", List.of(
        JsonParser.parse("{\"id\":1}"), JsonParser.parse("{\"name\":\"Ada\"}")));
    assertThat(merged.object.fields).containsOnlyKeys("id", "name");
    var target = new JavaSourceGenerator.Schema("Target");
    JavaSourceGenerator.addField(target, "value", new JavaSourceGenerator.FieldSchema("first", "value"));
    var source = new JavaSourceGenerator.Schema("Source");
    JavaSourceGenerator.addField(source, "value", new JavaSourceGenerator.FieldSchema("second", "value"));
    assertThatThrownBy(() -> JavaSourceGenerator.mergeObject(target, source))
        .isInstanceOf(DataMappingException.class).hasMessageContaining("first").hasMessageContaining("second");
  }

  @ParameterizedTest
  @ValueSource(strings = {"<root>text</root>", "<root>text<child/></root>",
      "<!DOCTYPE root [<!ENTITY x 'text'>]><root>&x;</root>", "<root>", ""})
  void rejectsUnsupportedOrMalformedXml(String xml) {
    assertThatThrownBy(() -> JavaSourceGenerator.parseXml(xml, "Root"))
        .isInstanceOf(DataMappingException.class);
  }

  @Test
  void detectsNestedAttributesAndMergesRepeatedXmlObjects() {
    var schema = JavaSourceGenerator.parseXml(
        "<root><item id=\"1\"><name>Ada</name></item><item id=\"2\"><age>42</age></item></root>", "Root");
    assertThat(JavaSourceGenerator.containsAttributes(schema)).isTrue();
    assertThat(JavaSourceGenerator.containsLists(schema)).isTrue();
    assertThat(schema.fields.get("item").type.object.fields).containsOnlyKeys("id", "name", "age");
    assertThatThrownBy(() -> JavaSourceGenerator.generate("Root", "<root><child id=\"1\"/></root>",
        true, JavaSourceGenerator.Style.RECORD)).isInstanceOf(DataMappingException.class)
        .hasMessageContaining("attributes");
    assertThatThrownBy(() -> JavaSourceGenerator.parseJson("[]", "Root"))
        .isInstanceOf(DataMappingException.class);
  }
  @Test
  void generatesAttributeFreeXmlRecordsAndNestedLists() {
    String xml = "<root><child><value>0</value><value>1</value></child></root>";
    var schema = JavaSourceGenerator.parseXml(xml, "Root");
    assertThat(JavaSourceGenerator.containsAttributes(schema)).isFalse();
    assertThat(JavaSourceGenerator.containsLists(schema)).isTrue();
    var sources = JavaSourceGenerator.generate("Root", xml, true, JavaSourceGenerator.Style.RECORD);
    assertThat(sources).containsOnlyKeys("Root.java", "Child.java");
    assertThat(sources.get("Root.java")).contains("public record Root(Child child)")
        .doesNotContain("public record Child", "XmlAttribute");
    assertThat(sources.get("Child.java")).contains("public record Child(List<Integer> value)")
        .doesNotContain("XmlAttribute");
  }

  @Test
  void preservesNamespaceDeclarationsInSchemaProperties() {
    var root = new JavaSourceGenerator.XmlNode("root");
    root.attributes.put("xmlns", "urn:default");
    root.attributes.put("xmlns:p", "urn:prefix");
    root.attributes.put("id", "1");
    assertThat(JavaSourceGenerator.xmlObject("Root", root).fields).containsOnlyKeys("id", "xmlns", "xmlns$colon$p");
  }

  @Test
  void rendersSeparatePojoClassesAndAttributeOnlyParameters() {
    var generated = JavaSourceGenerator.generate("Root", "<root><child id=\"1\"><value>x</value></child></root>",
        true, JavaSourceGenerator.Style.POJO);
    assertThat(generated).containsOnlyKeys("Root.java", "Child.java");
    assertThat(generated.get("Child.java")).contains("public class Child", "String value");
    assertThat(generated.get("Root.java")).doesNotContain("class Child");
    var schema = JavaSourceGenerator.parseXml("<root id=\"1\"><value>x</value></root>", "Root");
    StringBuilder parameters = new StringBuilder();
    JavaSourceGenerator.appendParameters(parameters, schema, true);
    assertThat(parameters).hasToString("@XmlAttribute(\"id\") int id");
  }

  @Test
  void avoidsCaseInsensitiveTypeNameCollisionsAndReservedRootNames() {
    var sources = JavaSourceGenerator.generate("Root", "{\"name\":{},\"NAME\":{}}",
        false, JavaSourceGenerator.Style.RECORD);
    assertThat(sources).containsOnlyKeys("Root.java", "Name.java", "NAME2.java");
    assertThatThrownBy(() -> JavaSourceGenerator.generate("String", "{}", false, JavaSourceGenerator.Style.RECORD))
        .isInstanceOf(DataMappingException.class).hasMessageContaining("Rename the input file");
  }

  @Test
  void mergesNestedXmlSchemasAndWidensScalarValues() {
    var schema = JavaSourceGenerator.parseXml("""
        <Root><item enabled="true"><child><value>1</value></child></item>
        <item><child><value>2.5</value><extra>x</extra></child></item></Root>
        """, "Root");
    var item = schema.fields.get("item").type.object;
    assertThat(item.fields.get("enabled").typeName()).isEqualTo("Boolean");
    var child = item.fields.get("child").type.object;
    assertThat(child.fields).containsOnlyKeys("value", "extra");
    assertThat(child.fields.get("value").typeName()).isEqualTo("java.math.BigDecimal");
    assertThat(JavaSourceGenerator.parseXml("<Root><v>1</v><v>text</v></Root>", "Root")
        .fields.get("v").typeName()).isEqualTo("List<String>");
    assertThat(JavaSourceGenerator.parseXml("<Root><v>1</v><v>2147483648</v></Root>", "Root")
        .fields.get("v").typeName()).isEqualTo("List<Long>");
    assertThat(JavaSourceGenerator.parseXml("<Root><v>1</v><v>9223372036854775808</v></Root>", "Root")
        .fields.get("v").typeName()).isEqualTo("List<java.math.BigInteger>");
  }

  @Test
  void rejectsIncompatibleScalarAndStructuredXmlSamples() {
    for (String xml : List.of(
        "<Root><v>1</v><v id=\"2\">two</v></Root>",
        "<Root><item><v>1</v></item><item><v><child>2</child></v></item></Root>",
        "<Root><item><v><child>2</child></v></item><item><v>1</v></item></Root>")) {
      assertThatThrownBy(() -> JavaSourceGenerator.parseXml(xml, "Root"))
          .isInstanceOf(DataMappingException.class).hasMessageContaining("scalar and object");
    }
  }

  @Test
  void preservesNamespaceDeclarationsAndGeneratesTextParameters() {
    var sources = JavaSourceGenerator.generate(null, """
        <p:Amount xmlns:p="urn:prices" xmlns="urn:default" unit="EUR">001.20</p:Amount>
        """, true, JavaSourceGenerator.Style.WITH_CONSTRUCTOR);
    assertThatThrownBy(() -> JavaSourceGenerator.generate(null, "<String id=\"1\"/>",
        true, JavaSourceGenerator.Style.POJO)).isInstanceOf(DataMappingException.class)
        .hasMessageContaining("Rename the XML root element");
    assertThat(sources).containsOnlyKeys("p$colon$Amount.java");
    assertThat(sources.get("p$colon$Amount.java"))
        .contains("@XmlAttribute(\"xmlns:p\") String xmlns$colon$p", "@XmlAttribute(\"xmlns\") String xmlns",
            "@XmlText String $text", "import com.lemadane.jax.XmlText;");
  }

}
