package com.lemadane.jax.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import java.util.List;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import com.lemadane.jax.Jax;

class JaxCliTest {

  @TempDir
  Path tempDir;

  @DisplayName("Creates POJO source from JSON beside the input")
  @Test
  void createsPojoSourceFromJsonBesideInput() throws Exception {
    Path input = tempDir.resolve("person.json");
    Files.writeString(input, "{\"name\":\"Ada\",\"age\":37}", StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    int exitCode;
    try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "pojo", input.toString()}, out, err);
    }

    String source = Files.readString(tempDir.resolve("Person.java"), StandardCharsets.UTF_8);
    assertThat(exitCode).isZero();
    assertThat(output.toString(StandardCharsets.UTF_8)).contains("Person.java");
    assertThat(source).contains("public String name()").contains("public void name(String value)")
        .contains("public int age()").contains("public void age(int value)");
  }

  @DisplayName("Compiles and maps generated records from nested JSON")
  @Test
  void generatedRecordCompilesAndMapsJsonWithNestedValues() throws Exception {
    Path input = tempDir.resolve("person.json");
    String json = "{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\"},"
        + "\"tags\":[\"math\",\"computing\"]}";
    Files.writeString(input, json, StandardCharsets.UTF_8);
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "record", input.toString()}, out, err);
    }

    Path source = tempDir.resolve("Person.java");
    Path classes = Files.createDirectory(tempDir.resolve("record-classes"));
    assertThat(exitCode).isZero();
    int compilerResult = ToolProvider.getSystemJavaCompiler().run(
        null,
        null,
        null,
        "-sourcepath",
        tempDir.toString(),
        "-classpath",
        System.getProperty("java.class.path"),
        "-d",
        classes.toString(),
        source.toString()
    );
    assertThat(compilerResult).isZero();

    try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()})) {
      Class<?> type = classLoader.loadClass("Person");
      Object person = Jax.toObject(json, type);
      assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
      assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
      assertThat(type.getMethod("tags").invoke(person)).isEqualTo(List.of("math", "computing"));
      Object address = type.getMethod("address").invoke(person);
      assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    }
  }

  @DisplayName("Compiles and maps constructor POJOs from XML attributes and repeated elements")
  @Test
  void generatedConstructorPojoCompilesAndMapsXmlAttributesAndRepeatedElements() throws Exception {
    Path input = tempDir.resolve("person.xml");
    Files.writeString(input,
        "<Person id=\"P1\"><name>Ada</name><age>37</age><tag>math</tag><tag>computing</tag></Person>",
        StandardCharsets.UTF_8);
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "with-constructor", input.toString()}, out, err);
    }

    Path source = tempDir.resolve("Person.java");
    Path classes = Files.createDirectory(tempDir.resolve("classes"));
    assertThat(exitCode).isZero();
    int compilerResult = ToolProvider.getSystemJavaCompiler().run(
        null,
        null,
        null,
        "-sourcepath",
        tempDir.toString(),
        "-classpath",
        System.getProperty("java.class.path"),
        "-d",
        classes.toString(),
        source.toString()
    );
    assertThat(compilerResult).isZero();

    try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()})) {
      Class<?> type = classLoader.loadClass("Person");
      Object person = Jax.toObject(
          "<Person id=\"P1\"><name>Ada</name><age>37</age><tag>math</tag><tag>computing</tag></Person>",
          type
      );
      assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
      assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
      assertThat(type.getMethod("id").invoke(person)).isEqualTo("P1");
      assertThat(type.getMethod("tag").invoke(person)).isEqualTo(List.of("math", "computing"));
      assertThat(Jax.toJson(person)).contains("\"name\":\"Ada\"").contains("\"age\":37");
      assertThat(Jax.toXml(person)).contains("id=\"P1\"").contains("<name>Ada</name>")
          .contains("<tag>math</tag>").contains("<tag>computing</tag>");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Person.java", "Address.java"})
  void rejectsExistingOutputBeforeWritingAnySource(String existingFile) throws Exception {
    Path input = tempDir.resolve("person.json");
    Files.writeString(input, "{\"address\":{\"city\":\"London\"}}");
    Path existing = tempDir.resolve(existingFile);
    Files.writeString(existing, "// Existing user source");
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ByteArrayOutputStream error = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
      assertThat(JaxCli.run(new String[]{"--create", "record", input.toString()}, out, err)).isEqualTo(1);
    }
    assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
    assertThat(error.toString(StandardCharsets.UTF_8)).contains(existingFile);
    assertThat(Files.readString(existing)).isEqualTo("// Existing user source");
    try (var files = Files.list(tempDir)) {
      assertThat(files.filter(path -> path.toString().endsWith(".java")).toList()).containsExactly(existing);
    }
  }

  @DisplayName("Rejects unknown source-generation styles")
  @Test
  void rejectsUnknownCreateStyle() {
    ByteArrayOutputStream error = new ByteArrayOutputStream();
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream());
        PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "bean", "person.json"}, out, err);
    }
    assertThat(exitCode).isEqualTo(2);
    assertThat(error.toString(StandardCharsets.UTF_8)).contains("Unknown create style");
  }

  @DisplayName("Merges distinct object fields from JSON arrays into generated schemas")
  @Test
  void mergesObjectFieldsAcrossJsonArrayItems() throws Exception {
    Path input = tempDir.resolve("collection.json");
    Files.writeString(input, "{\"items\":[{\"name\":\"Ada\"},{\"age\":37}]}", StandardCharsets.UTF_8);
    ByteArrayOutputStream error = new ByteArrayOutputStream();
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "record", input.toString()}, out, err);
    }

    assertThat(exitCode).isZero();
    assertThat(Files.readString(tempDir.resolve("Collection.java"), StandardCharsets.UTF_8))
        .contains("public record Collection(List<Items> items)")
        .doesNotContain("record Items");
    assertThat(Files.readString(tempDir.resolve("Items.java"), StandardCharsets.UTF_8))
        .contains("public record Items(String name, Integer age)");
    assertThat(error.toString(StandardCharsets.UTF_8)).isEmpty();
  }

  @DisplayName("Rejects colliding generated Java field names")
  @Test
  void rejectsCollidingGeneratedJavaFieldNames() throws Exception {
    Path input = tempDir.resolve("collision.json");
    Files.writeString(input, "{\"\":\"empty\",\"value\":\"named\"}", StandardCharsets.UTF_8);
    ByteArrayOutputStream error = new ByteArrayOutputStream();
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "record", input.toString()}, out, err);
    }

    assertThat(exitCode).isEqualTo(1);
    assertThat(error.toString(StandardCharsets.UTF_8)).contains("both map to Java identifier");
  }

  @DisplayName("Keeps distinct special-name properties in generated schemas")
  @Test
  void keepsDistinctSpecialNameProperties() throws Exception {
    Path input = tempDir.resolve("special-names.json");
    Files.writeString(input, "{\"a-b\":\"hyphen\",\"a:hyphen:b\":\"colons\"}", StandardCharsets.UTF_8);
    int exitCode;
    try (PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      exitCode = JaxCli.run(new String[]{"--create", "record", input.toString()}, out, err);
    }

    String source = Files.readString(tempDir.resolve("SpecialNames.java"), StandardCharsets.UTF_8);
    assertThat(exitCode).isZero();
    assertThat(source).contains("String a$hyphen$b").contains("String a$colon$hyphen$colon$b");
  }

  @DisplayName("Main command writes generated source and returns successfully")
  @Test
  void mainCreatesSourceForValidArguments() throws Exception {
    Path input = tempDir.resolve("main.json");
    Files.writeString(input, "{\"name\":\"Ada\"}", StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    try (PrintStream testOut = new PrintStream(output, true, StandardCharsets.UTF_8);
        PrintStream testErr = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
      System.setOut(testOut);
      System.setErr(testErr);
      JaxCli.main(new String[]{"--create", "record", input.toString()});
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    assertThat(Files.readString(tempDir.resolve("Main.java"), StandardCharsets.UTF_8))
        .contains("public record Main(String name)");
    assertThat(output.toString(StandardCharsets.UTF_8)).contains("Created ");
  }
}
