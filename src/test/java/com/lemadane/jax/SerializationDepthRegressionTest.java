package com.lemadane.jax;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class SerializationDepthRegressionTest {
  record Envelope(Object value) {}
  record Node(Object value) {}

  public static class Pojo {
    public Object value;

    public Pojo(Object value) {
      this.value = value;
    }
  }

  enum Shape {
    LIST, ARRAY, OPTIONAL, RECORD, POJO
  }

  static Object nested(Shape shape, int wrappers) {
    Object value = "leaf";
    for (int i = 0; i < wrappers; i++) {
      value = switch (shape) {
        case LIST -> List.of(value);
        case ARRAY -> new Object[] {value};
        case OPTIONAL -> Optional.of(value);
        case RECORD -> new Node(value);
        case POJO -> new Pojo(value);
      };
    }
    return value;
  }

  @ParameterizedTest
  @EnumSource(Shape.class)
  void acceptsValuesAtTheDepthLimit(Shape shape) {
    // Envelope + 254 wrappers + scalar leaf = 256 values along the path.
    Envelope value = new Envelope(nested(shape, 254));
    String json = Jax.toJson(value);
    String xml = Jax.toXml(value);
    assertThat(json).contains("leaf");
    assertThat(xml).contains("leaf");
    assertThat(Jax.toDocument(json)).containsKey("value");
  }

  @ParameterizedTest
  @EnumSource(Shape.class)
  void rejectsValuesImmediatelyBeyondTheDepthLimit(Shape shape) {
    Envelope value = new Envelope(nested(shape, 255));
    assertThatThrownBy(() -> Jax.toJson(value))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("serialization depth of 256");
    assertThatThrownBy(() -> Jax.toXml(value))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("serialization depth of 256");
  }

  @ParameterizedTest
  @EnumSource(Shape.class)
  void rejectsTenThousandNestedValuesWithoutStackOverflow(Shape shape) {
    Envelope value = new Envelope(nested(shape, 10_000));
    assertThatThrownBy(() -> Jax.toJson(value))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("serialization depth");
    assertThatThrownBy(() -> Jax.toXml(value))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("serialization depth");
    // A failed traversal must not affect later serialization calls.
    assertThat(Jax.toJson(new Envelope("ok"))).isEqualTo("{\"value\":\"ok\"}");
    assertThat(Jax.toXml(new Envelope("ok"))).isEqualTo("<Envelope><value>ok</value></Envelope>");
  }

  @Test
  void appliesTheSameLimitToJsonMaps() {
    Object value = "leaf";
    for (int i = 0; i < 255; i++) {
      value = Map.of("value", value);
    }
    assertThat(Jax.toJson(value)).contains("leaf");
    Object tooDeep = Map.of("value", value);
    assertThatThrownBy(() -> Jax.toJson(tooDeep))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("serialization depth of 256");
  }
}
