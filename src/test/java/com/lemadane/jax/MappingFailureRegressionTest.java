package com.lemadane.jax;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lemadane.jax.exceptions.DataMappingException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class MappingFailureRegressionTest {
  @TempDir
  Path directory;

  public static class Setters {
    private int[] array;
    private List<Integer> list;

    @JsonProperty
    public void array(int[] value) {
      array = value;
    }

    @JsonProperty
    public void list(List<Integer> value) {
      list = value;
    }

    @JsonProperty
    public void scalar(String value) {
      throw new IllegalArgumentException("rejected " + value);
    }
  }

  public static class FieldValue {
    public String value;
  }

  public static class Ambiguous {
    public Ambiguous(String value) { /* Matching constructor fixture. */ }
    public Ambiguous(int value) { /* Matching constructor fixture. */ }
  }

  public abstract static class AbstractConstructor {
    public AbstractConstructor(String value) { /* Cannot instantiate abstract type. */ }
  }

  public static class BrokenGetter {
    private String value;

    public String value() {
      throw new IllegalStateException("getter failed");
    }
  }

  record BrokenRecord(String value) {
    @Override
    public String value() {
      throw new IllegalStateException("accessor failed");
    }
  }

  @Test
  void readsXmlCollectionsThroughSetters() {
    Setters result = Jax.toObject("<Setters><array>1</array><array>2</array>"
        + "<list>3</list><list>4</list></Setters>", Setters.class);
    assertThat(result.array).containsExactly(1, 2);
    assertThat(result.list).containsExactly(3, 4);
  }

  @Test
  void rejectsRepeatedScalarXmlFieldsAndSetters() {
    assertThatThrownBy(() -> Jax.toObject("<FieldValue><value>a</value><value>b</value></FieldValue>", FieldValue.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("non-collection field");
    assertThatThrownBy(() -> Jax.toObject("<Setters><scalar>a</scalar><scalar>b</scalar></Setters>", Setters.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("non-collection setter");
    assertThatThrownBy(() -> Jax.toObject("<Setters><scalar>a</scalar></Setters>", Setters.class))
        .isInstanceOf(XmlMappingException.class).hasRootCauseMessage("rejected a");
  }

  @Test
  void reportsConstructorAmbiguityAndInstantiationFailures() {
    assertThatThrownBy(() -> Jax.toObject("{\"value\":1}", Ambiguous.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Multiple constructors");
    assertThatThrownBy(() -> Jax.toObject("<Ambiguous><value>1</value></Ambiguous>", Ambiguous.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Multiple constructors");
    assertThatThrownBy(() -> Jax.toObject("{\"value\":\"x\"}", AbstractConstructor.class))
        .isInstanceOf(JsonMappingException.class).hasCauseInstanceOf(InstantiationException.class);
  }

  @Test
  void explainsMissingParameterMetadataInBothFormats() throws Exception {
    Path source = directory.resolve("WithoutNames.java");
    Files.writeString(source, "public class WithoutNames { public WithoutNames(String value) {} }");
    assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
        "-d", directory.toString(), source.toString())).isZero();
    try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {directory.toUri().toURL()})) {
      Class<?> type = loader.loadClass("WithoutNames");
      assertThat(type.getConstructors()[0].getParameters()[0].isNamePresent()).isFalse();
      for (String input : List.of("{\"value\":\"x\"}", "<WithoutNames><value>x</value></WithoutNames>")) {
        assertThatThrownBy(() -> Jax.toObject(input, type))
            .isInstanceOf(DataMappingException.class).hasMessageContaining("-parameters");
      }
    }
  }

  @Test
  void wrapsFailingGettersAndRecordAccessors() {
    for (Object value : List.of(new BrokenGetter(), new BrokenRecord("x"))) {
      assertThatThrownBy(() -> Jax.toJson(value))
          .isInstanceOf(JsonMappingException.class).hasRootCauseInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> Jax.toXml(value))
          .isInstanceOf(XmlMappingException.class).hasRootCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void rejectsEmptyInputsAndUnsupportedPartialFormats() {
    assertThatThrownBy(() -> Jax.toObject(" \n ", FieldValue.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Empty");
    assertThatThrownBy(() -> Jax.toPartialObject(" \n ", FieldValue.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Empty");
    assertThatThrownBy(() -> Jax.toPartialObject("[]", FieldValue.class))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Cannot detect");
  }
}
