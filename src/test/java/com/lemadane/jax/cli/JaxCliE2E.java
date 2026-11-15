package com.lemadane.jax.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lemadane.jax.Jax;

class JaxCliE2E {

  @TempDir
  Path tempDir;

  List<URLClassLoader> classLoaders = new ArrayList<>();

  @AfterEach
  void closeClassLoaders() throws Exception {
    for (URLClassLoader classLoader : classLoaders) {
      classLoader.close();
    }
  }

  @DisplayName("java -jar creates and compiles a record from nested JSON")
  @Test
  void createsRecordFromJsonUsingJarCommand() throws Exception {
    String json = """
        {"name":"Ada","age":37,"address":{"city":"London"},"tags":["math","computing"]}
        """;
    CliResult result = runCli("record", "json", json);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).contains("Created ").contains("Person.java");
    assertThat(Files.readString(source)).contains("public record Person").contains("record Address")
        .contains("List<String> tags");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(json, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    assertThat(type.getMethod("tags").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  @DisplayName("java -jar creates and compiles a POJO from XML")
  @Test
  void createsPojoFromXmlUsingJarCommand() throws Exception {
    String xml = "<Person><name>Ada</name><age>37</age><address><city>London</city></address></Person>";
    CliResult result = runCli("pojo", "xml", xml);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    String generated = Files.readString(source);
    assertThat(generated).contains("public class Person").contains("public String name()")
        .contains("public void name(String value)").contains("public static class Address");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(xml, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
  }

  @DisplayName("java -jar creates and compiles a constructor-bound POJO from XML attributes and repeated elements")
  @Test
  void createsConstructorPojoFromXmlUsingJarCommand() throws Exception {
    String xml = """
        <Person id="P1"><name>Ada</name><age>37</age><tag>math</tag><tag>computing</tag></Person>
        """;
    CliResult result = runCli("with-constructor", "xml", xml);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    String generated = Files.readString(source);
    assertThat(generated).contains("@XmlAttribute(\"id\")").contains("public Person(")
        .contains("public Person()").contains("List<String> tag");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(xml, type);
    assertThat(type.getMethod("id").invoke(person)).isEqualTo("P1");
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    assertThat(type.getMethod("tag").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  @DisplayName("java -jar creates and compiles a POJO from nested JSON")
  @Test
  void createsPojoFromJsonUsingJarCommand() throws Exception {
    String json = """
        {"name":"Ada","age":37,"address":{"city":"London"},"tags":["math","computing"]}
        """;
    CliResult result = runCli("pojo", "json", json);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    String generated = Files.readString(source);
    assertThat(generated).contains("public class Person").contains("public void age(int value)")
        .contains("public static class Address");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(json, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    assertThat(type.getMethod("tags").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  @DisplayName("java -jar creates and compiles a constructor-bound POJO from nested JSON")
  @Test
  void createsConstructorPojoFromJsonUsingJarCommand() throws Exception {
    String json = """
        {"name":"Ada","age":37,"address":{"city":"London"},"tags":["math","computing"]}
        """;
    CliResult result = runCli("with-constructor", "json", json);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    assertThat(Files.readString(source)).contains("public Person(").contains("public Person()");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(json, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    assertThat(type.getMethod("tags").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  @DisplayName("java -jar creates and compiles a record from nested XML")
  @Test
  void createsRecordFromXmlUsingJarCommand() throws Exception {
    String xml = """
        <Person><name>Ada</name><age>37</age><address><city>London</city></address><tag>math</tag><tag>computing</tag></Person>
        """;
    CliResult result = runCli("record", "xml", xml);
    Path source = generatedSource(result);
    assertThat(result.exitCode()).isZero();
    assertThat(Files.readString(source)).contains("public record Person").contains("record Address")
        .contains("List<String> tag");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(xml, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    assertThat(type.getMethod("tag").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  CliResult runCli(String style, String extension, String content) throws Exception {
    Path input = tempDir.resolve("person." + extension);
    Files.writeString(input, content, StandardCharsets.UTF_8);
    Path outputLog = tempDir.resolve(style + "-" + extension + ".log");
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String jar = System.getProperty("jax.jar.path");
    assertThat(jar).as("Gradle provides the executable Jax jar path").isNotBlank();

    Process process = new ProcessBuilder(java, "-jar", jar, "--create", style, input.toString())
        .redirectErrorStream(true)
        .redirectOutput(outputLog.toFile())
        .start();
    boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) {
      process.destroyForcibly();
      throw new AssertionError("CLI process did not finish within 30 seconds");
    }
    return new CliResult(process.exitValue(), Files.readString(outputLog, StandardCharsets.UTF_8));
  }

  Path generatedSource(CliResult result) {
    assertThat(result.exitCode()).as("CLI output: %s", result.output()).isZero();
    return tempDir.resolve("Person.java");
  }

  Class<?> compileGenerated(Path source) throws Exception {
    Path classes = Files.createDirectory(tempDir.resolve("generated-classes-" + System.nanoTime()));
    int compilerResult = ToolProvider.getSystemJavaCompiler().run(
        null,
        null,
        null,
        "-parameters",
        "-classpath",
        System.getProperty("java.class.path"),
        "-d",
        classes.toString(),
        source.toString()
    );
    assertThat(compilerResult).isZero();
    URLClassLoader classLoader = new URLClassLoader(new URL[]{classes.toUri().toURL()});
    classLoaders.add(classLoader);
    return classLoader.loadClass("Person");
  }

  record CliResult(int exitCode, String output) {}
}
