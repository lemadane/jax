package com.lemadane.jax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lemadane.jax.convert.DefaultValueProvider;
import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.DataMappingException;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.MissingDataException;
import com.lemadane.jax.exceptions.NameEncodingException;
import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.json.internal.JsonParser;
import com.lemadane.jax.reflect.RecordIntrospector;
import com.lemadane.jax.reflect.RecordMetadata;

class CoverageTest {

  public enum DummyEnum { A, B }

  public record TestOpt(Optional<String> optStr, Optional<Integer> optInt) {}
  public record TestPrimitives(byte b, short s, int i, long l, float f, double d, char c, boolean bool) {}
  public record TestBoxed(Byte b, Short s, Integer i, Long l, Float f, Double d, Character c, Boolean bool, BigInteger bi, BigDecimal bd, DummyEnum de) {}
  public record Nested(String name) {}
  public record RootWithNestedList(List<Nested> items) {}
  public record RootWithPrimList(List<Integer> numbers) {}
  public record XmlStaticTest(String val) {
  public static String attr1;
  public static String attr2 = null;
  }

  @DisplayName("Exception Cause Constructors")
  @Test
  void testExceptionCauseConstructors() {
  Throwable cause = new RuntimeException("cause");
  assertThat(
    new MissingDataException("msg", cause)).hasCause(cause);
  assertThat(
    new ExcessDataException("msg", cause)).hasCause(cause);
  assertThat(
    new TypeConversionException("msg", cause)).hasCause(cause);
  assertThat(
    new JsonMappingException("msg", cause)).hasCause(cause);
  assertThat(
    new XmlMappingException("msg", cause)).hasCause(cause);
  assertThat(
    new DataMappingException("msg", cause)).hasCause(cause);
  assertThat(
    new NameEncodingException("msg", cause)).hasCause(cause);
  assertThat(
    new RecordConstructionException("msg", cause)).hasCause(cause);

  // Non-record inspect test
  assertThatThrownBy(
    () -> RecordIntrospector.inspect(OrdinaryClass.class))
    .isInstanceOf(RecordConstructionException.class);
  }

  public static class OrdinaryClass {}

  @DisplayName("Type Converter Exhaustive")
  @Test
  void testTypeConverterExhaustive() {
  assertThat(
    TypeConverter.convert(String.class, "hello", "path"))
    .isEqualTo("hello");
  assertThat(
    TypeConverter.convert(String.class, 123, "path"))
    .isEqualTo("123");

  // Numbers to primitives/wrappers
  assertThat(
    TypeConverter.convert(byte.class, (byte) 5, "path"))
    .isEqualTo((byte) 5);
  assertThat(
    TypeConverter.convert(Byte.class, "10", "path"))
    .isEqualTo((byte) 10);
  assertThat(
    TypeConverter.convert(short.class, (short) 100, "path"))
    .isEqualTo((short) 100);
  assertThat(
    TypeConverter.convert(Short.class, "200", "path"))
    .isEqualTo((short) 200);
  assertThat(
    TypeConverter.convert(int.class, 1000, "path"))
    .isEqualTo(1000);
  assertThat(
    TypeConverter.convert(Integer.class, "2000", "path"))
    .isEqualTo(2000);
  assertThat(
    TypeConverter.convert(long.class, 10000L, "path"))
    .isEqualTo(10000L);
  assertThat(
    TypeConverter.convert(Long.class, "20000", "path"))
    .isEqualTo(20000L);
  assertThat(
    TypeConverter.convert(float.class, 1.5f, "path"))
    .isEqualTo(1.5f);
  assertThat(
    TypeConverter.convert(Float.class, "2.5", "path"))
    .isEqualTo(2.5f);
  assertThat(
    TypeConverter.convert(double.class, 10.5d, "path"))
    .isEqualTo(10.5d);
  assertThat(
    TypeConverter.convert(Double.class, "20.5", "path"))
    .isEqualTo(20.5d);
  assertThat(
    TypeConverter.convert(boolean.class, true, "path"))
    .isEqualTo(true);
  assertThat(
    TypeConverter.convert(Boolean.class, "FALSE", "path"))
    .isEqualTo(false);
  assertThat(
    TypeConverter.convert(char.class, 'X', "path"))
    .isEqualTo('X');
  assertThat(
    TypeConverter.convert(Character.class, "Y", "path"))
    .isEqualTo('Y');
  assertThat(
    TypeConverter.convert(BigInteger.class, "12345", "path"))
    .isEqualTo(new BigInteger("12345"));
  assertThat(
    TypeConverter.convert(BigDecimal.class, "123.45", "path"))
    .isEqualTo(new BigDecimal("123.45"));
  assertThat(
    TypeConverter.convert(DummyEnum.class, "B", "path"))
    .isEqualTo(DummyEnum.B);

  // Conversion errors
  assertThatThrownBy(
    () -> TypeConverter.convert(byte.class, null, "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(boolean.class, "notbool", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(byte.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(short.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(int.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(long.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(float.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(double.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(char.class, "multi", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(BigInteger.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(BigDecimal.class, "abc", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(DummyEnum.class, "C", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(java.util.Date.class, "val", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(Integer.class, "4294967297", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(Integer.class, "1.9", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(BigInteger.class, "1.9", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(Byte.class, "128", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(Short.class, "32768", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> TypeConverter.convert(Long.class, "9223372036854775808", "path"))
    .isInstanceOf(TypeConversionException.class);
  assertThat(
    TypeConverter.convert(Integer.class, new BigDecimal("1.0"), "path"))
    .isEqualTo(1);
  }

  @DisplayName("Default Value Provider Exhaustive")
  @Test
  void testDefaultValueProviderExhaustive() {
  RecordMetadata meta =
    RecordIntrospector.inspect(TestPrimitives.class);
  for (RecordMetadata.ComponentMetadata comp : meta.components()) {
    Object def = DefaultValueProvider.getDefaultValue(comp);
    assertThat(def).isNotNull();
  }
  }

  @DisplayName("Json Parser Escapes And Numbers")
  @Test
  void testJsonParserEscapesAndNumbers() {
  String jsonStr =
    "{\"msg\":\"slash \\/ backslash \\\\ quote \\\" b \\b f \\f n \\n r \\r t \\t\", \"n1\": -10.5, \"n2\": 1e5, \"n3\": 2.5E-2}";
  record MsgRec(String msg, double n1, double n2, double n3) {}

  MsgRec rec = Jax.toObject(
    jsonStr,
    MsgRec.class);
  assertThat(rec.msg()).contains("/");
  assertThat(rec.n1()).isEqualTo(-10.5);
  assertThat(rec.n2()).isEqualTo(100000.0);

  assertThatThrownBy(() -> JsonParser.parse(""))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> JsonParser.parse("   "))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> JsonParser.parse("{ \"a\" "))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> JsonParser.parse("[ 1, "))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> JsonParser.parse("{ \"a\": 1, }"))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> JsonParser.parse("undefined"))
    .isInstanceOf(JsonMappingException.class);
  }

  @DisplayName("Json Record Writer Edge Cases")
  @Test
  void testJsonRecordWriterEdgeCases() {
  TestOpt optEmpty =
    new TestOpt(
      Optional.empty(),
      Optional.of(42));
  String jsonOpt = Jax.toJson(optEmpty);
  assertThat(jsonOpt).contains("\"optStr\":null");
  assertThat(jsonOpt).contains("\"optInt\":42");

  TestBoxed boxed =
    new TestBoxed(
      (byte) 1,
      (short) 2,
      3,
      4L,
      5.5f,
      6.6d,
      'Z',
      true,
      BigInteger.TEN,
      BigDecimal.ONE,
      DummyEnum.A);
  String jsonBoxed = Jax.toJson(boxed);
  assertThat(jsonBoxed).contains("\"de\":\"A\"");

  // Float / Double Infinity & NaN
  record FloatRec(float f) {}
  assertThatThrownBy(
    () -> Jax.toJson(
      new FloatRec(
        Float.POSITIVE_INFINITY)))
    .isInstanceOf(JsonMappingException.class);

  record DoubleRec(double d) {}
  assertThatThrownBy(
    () -> Jax.toJson(
      new DoubleRec(
        Double.NEGATIVE_INFINITY)))
    .isInstanceOf(JsonMappingException.class);

  // Control char escaping in writer
  record ControlRec(String text) {}
  ControlRec ctrl = new ControlRec("line1\nline2\t\u0001");
  String jsonCtrl = Jax.toJson(ctrl);
  assertThat(jsonCtrl).contains("\\n");
  assertThat(jsonCtrl).contains("\\t");
  assertThat(jsonCtrl).contains("\\u0001");
  }

  @DisplayName("Json Record Reader Edge Cases")
  @Test
  void testJsonRecordReaderEdgeCases() {
  record PrimRec(int count) {}
  assertThatThrownBy(
    () -> Jax.toObject("{\"count\": null}", PrimRec.class))
    .isInstanceOf(TypeConversionException.class);

  String jsonListNested =
    "{\"items\":[{\"name\":\"A\"}, {\"name\":\"B\"}]}";
  RootWithNestedList rootList =
    Jax.toObject(
      jsonListNested,
      RootWithNestedList.class);
  assertThat(rootList.items()).hasSize(2);
  assertThat(rootList.items().get(0).name()).isEqualTo("A");

  String jsonPrimList = "{\"numbers\":[1, 2, 3]}";
  RootWithPrimList rootPrimList =
    Jax.toObject(
      jsonPrimList,
      RootWithPrimList.class);
  assertThat(rootPrimList.numbers()).containsExactly(1, 2, 3);

  record IntValue(int value) {}
  assertThatThrownBy(
    () -> Jax.toObject("{\"value\":4294967297}", IntValue.class))
    .isInstanceOf(TypeConversionException.class);
  assertThatThrownBy(
    () -> Jax.toObject("{\"value\":1.9}", IntValue.class))
    .isInstanceOf(TypeConversionException.class);
  }

  @DisplayName("Json Parser Nesting Limit")
  @Test
  void testJsonParserNestingLimit() {
  String deeplyNested =
    "[".repeat(10_000) + "0" + "]"
    .repeat(10_000);
  assertThatThrownBy(() -> JsonParser.parse(deeplyNested))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("Maximum JSON nesting depth");
  }

  @DisplayName("Xml Writer And Reader Edge Cases")
  @Test
  void testXmlWriterAndReaderEdgeCases() {
  TestOpt optRec =
    new TestOpt(
      Optional.empty(),
      Optional.of(99));
  String xmlOpt = Jax.toXml(optRec);
  assertThat(xmlOpt).contains("<optInt>99</optInt>");
  assertThat(xmlOpt).contains("jax-null=\"true\"");
  assertThat(Jax.toObject(xmlOpt, TestOpt.class)).isEqualTo(optRec);

  XmlStaticTest.attr1 = "A1";
  XmlStaticTest.attr2 = null;
  XmlStaticTest staticRec = new XmlStaticTest("test");
  String xmlStatic = Jax.toXml(staticRec);
  assertThat(xmlStatic).doesNotContain("attr1=");

  // XML empty elements mapping
  record EmptyRecordTest(String str, Optional<String> opt, TestOpt record) {}
  String xmlEmpty =
    "<EmptyRecordTest><str/><opt jax-null=\"true\"/><record/></EmptyRecordTest>";
  EmptyRecordTest parsedEmpty =
    Jax.toPartialObject(
      xmlEmpty,
      EmptyRecordTest.class);
  assertThat(parsedEmpty.str()).isEqualTo("");
  assertThat(parsedEmpty.opt()).isEmpty();
  assertThat(parsedEmpty.record()).isNotNull();

  XmlStaticTest.attr1 = "shared";
  XmlStaticTest parsedStatic =
    Jax.toObject("<XmlStaticTest attr1=\"input\"><val>test</val></XmlStaticTest>",
    XmlStaticTest.class);
  assertThat(parsedStatic.val()).isEqualTo("test");
  assertThat(XmlStaticTest.attr1).isEqualTo("shared");
  assertThat(Jax.toXml(parsedStatic)).doesNotContain("attr1=");
  }

  @DisplayName("Record Metadata Methods")
  @Test
  void testRecordMetadataMethods() {
  RecordMetadata meta =
    RecordIntrospector.inspect(TestOpt.class);
  assertThat(meta.recordSimpleName()).isEqualTo("TestOpt");
  assertThat(meta.decodedRecordName()).isEqualTo("TestOpt");
  assertThat(meta.canonicalConstructor()).isNotNull();
  assertThat(meta.components()).hasSize(2);
  RecordMetadata.ComponentMetadata comp =
    meta.components()
    .get(0);
  assertThat(comp.name()).isEqualTo("optStr");
  assertThat(comp.decodedName()).isEqualTo("optStr");
  assertThat(comp.type()).isEqualTo(Optional.class);
  assertThat(comp.genericType()).isNotNull();
  assertThat(comp.accessor()).isNotNull();
  assertThat(comp.isList()).isFalse();
  assertThat(comp.listElementType()).isNull();
  assertThat(comp.isOptional()).isTrue();
  assertThat(comp.optionalValueType()).isEqualTo(String.class);
  }

  @DisplayName("Json Record Reader Non Object Roots And Types")
  @Test
  void testJsonRecordReaderNonObjectRootsAndTypes() {
  assertThatThrownBy(() -> Jax.toObject("123", Nested.class))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("JSON number");

  assertThatThrownBy(() -> Jax.toObject("true", Nested.class))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("JSON boolean");

  assertThatThrownBy(
    () -> Jax.toObject("[\"a\"]", Nested.class))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("JSON array");

  assertThatThrownBy(
    () -> Jax.toObject("\"str\"", Nested.class))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("JSON string");

  assertThatThrownBy(() -> Jax.toObject("null", Nested.class))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("JSON null");

  record ListReq(List<String> items) {}
  assertThatThrownBy(
    () -> Jax.toObject("{\"items\": \"not_an_array\"}", ListReq.class))
    .isInstanceOf(TypeConversionException.class)
    .hasMessageContaining("A JSON array does not match");

  record NestedReq(Nested item) {}
  assertThatThrownBy(
    () -> Jax.toObject("{\"item\": 123}", NestedReq.class))
    .isInstanceOf(TypeConversionException.class)
    .hasMessageContaining(
      "Expected JSON object for nested record");
  }

  @DisplayName("Json Record Writer Collection Cycle And Escaping")
  @Test
  void testJsonRecordWriterCollectionCycleAndEscaping() {
  record ListCycle(List<Object> items) {}
  List<Object> cycleList = new ArrayList<>();
  cycleList.add(cycleList);
  ListCycle cycleRec = new ListCycle(cycleList);

  assertThatThrownBy(() -> Jax.toJson(cycleRec))
    .isInstanceOf(JsonMappingException.class)
    .hasMessageContaining("Cycle detected");

  record StringEscapes(String text) {}
  StringEscapes esc =
    new StringEscapes("a\"b\\c/d\be\ff\ng\rh\ti\u0005");
  String jsonStr = Jax.toJson(esc);
  assertThat(
    jsonStr).contains("\\\"")
    .contains("\\\\")
    .contains("\\b")
    .contains("\\f")
    .contains("\\n")
    .contains("\\r")
    .contains("\\t")
    .contains("\\u0005");
  }

  @DisplayName("Xml Reader Empty Primitive In Partial Mode")
  @Test
  void testXmlReaderEmptyPrimitiveInPartialMode() {
  String xml = "<TestPrimitives><b/></TestPrimitives>";
  assertThatThrownBy(
    () -> Jax.toPartialObject(xml, TestPrimitives.class))
    .isInstanceOf(TypeConversionException.class)
    .hasMessageContaining(
      "Empty XML element <b> cannot be mapped to primitive type byte");
  }

  @DisplayName("Json Parser Syntax Errors Exhaustive")
  @Test
  void testJsonParserSyntaxErrorsExhaustive() {
  record Dummy(String a, boolean b, String c, double d) {}

  assertThatThrownBy(
    () -> Jax.toObject("{\"a\": \"unclosed}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"a\": \"\\k\"}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"a\": \"\n\"}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"a\": \"\\uGGGG\"}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"b\": truX}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"b\": falX}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"c\": nulX}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"d\": -}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"d\": 1.}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"d\": 1e}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{\"a\": 1 \"b\": 2}", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(
    () -> Jax.toObject("{ \"a\" }", Dummy.class))
    .isInstanceOf(JsonMappingException.class);

  assertThatThrownBy(() -> Jax.toObject("!", Dummy.class))
    .isInstanceOf(JsonMappingException.class);
  }
}
