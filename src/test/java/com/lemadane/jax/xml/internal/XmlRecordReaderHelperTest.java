package com.lemadane.jax.xml.internal;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.reflect.RecordIntrospector;

import static org.assertj.core.api.Assertions.*;

class XmlRecordReaderHelperTest {
  record Empty() {}
  record OptionalTypes(Optional<Integer> number, Optional<List<String>> list,
      Optional<int[]> array, Optional<Empty> record) {}

  @Test
  void mapsEmptyOptionalsForAllNonStringShapes() {
    for (var component : RecordIntrospector.inspect(OptionalTypes.class).components()) {
      Object result = XmlRecordReader.readValue(Optional.class, component.genericType(),
          new XmlElement("value"), "/value", true);
      assertThat(result).as(component.name()).isEqualTo(Optional.empty());
    }
  }

  @Test
  void distinguishesStrictAndPartialEmptyScalarMapping() {
    XmlElement empty = new XmlElement("value");
    assertThat(XmlRecordReader.readValue(Integer.class, Integer.class, empty, "/value", false)).isNull();
    assertThatThrownBy(() -> XmlRecordReader.readValue(Integer.class, Integer.class, empty, "/value", true))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("/value");
    for (boolean strict : new boolean[] {false, true}) {
      assertThatThrownBy(() -> XmlRecordReader.readValue(int.class, int.class, empty, "/value", strict))
          .isInstanceOf(TypeConversionException.class).hasMessageContaining("primitive");
    }
  }

  @Test
  void rejectsNullPrimitivesAndEmptyListMarkersOnScalars() {
    XmlElement nullValue = XmlRecordReader.parseXmlDocument("<value jax-null=\"true\"/>");
    assertThatThrownBy(() -> XmlRecordReader.readValue(int.class, int.class, nullValue, "/value", true))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("null");
    XmlElement list = XmlRecordReader.parseXmlDocument("<value jax-empty-list=\"true\"/>");
    assertThatThrownBy(() -> XmlRecordReader.readValue(String.class, String.class, list, "/value", true))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("Empty-list marker");
    XmlElement array = XmlRecordReader.parseXmlDocument("<value><n>1</n><n>2</n></value>");
    assertThat((int[]) XmlRecordReader.readValue(int[].class, int[].class, array, "/value", true))
        .containsExactly(1, 2);
  }

  @Test
  void rejectsUnrepresentableTextInEmptyRecords() {
    XmlElement text = XmlRecordReader.parseXmlDocument("<Empty>unmapped</Empty>");
    assertThatThrownBy(() -> XmlRecordReader.readRecord(RecordIntrospector.inspect(Empty.class), text, "/Empty", true))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Mixed content");
    assertThat((Empty) XmlRecordReader.readRecord(RecordIntrospector.inspect(Empty.class), text, "/Empty", false))
        .isEqualTo(new Empty());
    assertThat(XmlRecordReader.qualifiedName(null, "name")).isEqualTo("name");
    assertThat(XmlRecordReader.qualifiedName("", "name")).isEqualTo("name");
    assertThat(XmlRecordReader.qualifiedName("p", "name")).isEqualTo("p:name");
  }
}
