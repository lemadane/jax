package com.lemadane.jax.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static com.lemadane.jax.XmlRoundTripSnapshot.xmlValues;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.math.BigDecimal;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import com.lemadane.jax.Jax;
import com.lemadane.jax.Document;
import com.lemadane.jax.exceptions.MissingDataException;

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
    assertThat(Files.readString(source)).contains("public record Person")
        .contains("List<String> tags");

    assertThat(Files.readString(tempDir.resolve("Address.java"))).contains("public ");
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
        .contains("public void name(String value)");

    assertThat(Files.readString(tempDir.resolve("Address.java"))).contains("public ");
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
    assertThat(generated).contains("public class Person").contains("public void age(int value)");

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
    assertThat(Files.readString(source)).contains("public record Person")
        .contains("List<String> tag");

    Class<?> type = compileGenerated(source);
    Object person = Jax.toObject(xml, type);
    assertThat(type.getMethod("name").invoke(person)).isEqualTo("Ada");
    assertThat(type.getMethod("age").invoke(person)).isEqualTo(37);
    Object address = type.getMethod("address").invoke(person);
    assertThat(address.getClass().getMethod("city").invoke(address)).isEqualTo("London");
    assertThat(type.getMethod("tag").invoke(person)).isEqualTo(List.of("math", "computing"));
  }

  @DisplayName("java -jar generates models that round-trip a deeply nested organization payload")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"record", "pojo", "with-constructor"})
  void roundTripsComplexOrganizationFromJsonUsingJarCommand(String style) throws Exception {
    String json;
    try (InputStream fixture = getClass().getResourceAsStream("complex-organization.json")) {
      assertThat(fixture).as("complex organization JSON fixture").isNotNull();
      json = new String(fixture.readAllBytes(), StandardCharsets.UTF_8);
    }

    CliResult result = runCli(style, "json", json);
    Class<?> type = compileGenerated(generatedSource(result));
    Object model;
    if (type.isRecord()) {
      // Milestones have different shapes: M1 has deliverables, M2 has blockers.
      // The generated record contains both fields, so missing fields require partial mapping.
      assertThatThrownBy(() -> Jax.toObject(json, type)).isInstanceOf(MissingDataException.class);
      model = Jax.toPartialObject(json, type);
    } else {
      model = Jax.toObject(json, type);
    }

    Document expected = Jax.toDocument(json);
    Document organization = (Document) expected.get("organization");
    Document department = (Document) ((List<?>) organization.get("departments")).get(0);
    Document project = (Document) ((List<?>) department.get("projects")).get(0);
    List<?> milestones = (List<?>) project.get("milestones");
    // Serialization includes the missing properties with the documented mapping defaults.
    Object missingList = type.isRecord() ? List.of() : null;
    ((Document) milestones.get(0)).put("blockers", missingList);
    ((Document) milestones.get(1)).put("deliverables", missingList);

    String serialized = Jax.toJson(model);
    assertThat(Jax.toDocument(serialized)).usingRecursiveComparison()
        .withComparatorForType((left, right) -> new BigDecimal(left.toString())
            .compareTo(new BigDecimal(right.toString())), Number.class)
        .isEqualTo(expected);

    // After defaults have been serialized, the complete model supports strict re-reading.
    Object reread = Jax.toObject(serialized, type);
    assertThat(Jax.toDocument(Jax.toJson(reread))).isEqualTo(Jax.toDocument(serialized));
  }

  @DisplayName("Separate files resolve repeated type names without changing mapped data")
  @ParameterizedTest(name = "{0} from {1}")
  @CsvSource({"record,json", "pojo,json", "with-constructor,json",
      "record,xml", "pojo,xml", "with-constructor,xml"})
  void createsSeparateFilesForCollidingTypeNames(String style, String extension) throws Exception {
    String json = """
        {
          "person": {"name": "Ada"},
          "billing": {"address": {"city": "London"}},
          "shipping": {"address": {"postalCode": "ABC"}},
          "list": {"value": "reserved"},
          "string": {"value": true},
          "address": {"line": "Main Street"}
        }
        """;
    String xml = """
        <Person>
          <person><name>Ada</name></person>
          <billing><address><city>London</city></address></billing>
          <shipping><address><postalCode>ABC</postalCode></address></shipping>
          <list><value>reserved</value></list>
          <string><value>true</value></string>
          <address><line>Main Street</line></address>
        </Person>
        """;
    String input = extension.equals("json") ? json : xml;
    CliResult result = runCli(style, extension, input);
    Class<?> type = compileGenerated(generatedSource(result));
    try (var files = Files.list(tempDir)) {
      assertThat(files.filter(path -> path.toString().endsWith(".java"))
          .map(path -> path.getFileName().toString()).toList()).containsExactlyInAnyOrder(
              "Person.java", "Person2.java", "Billing.java", "Address.java", "Shipping.java",
              "Address2.java", "List2.java", "String2.java", "Address3.java");
    }
    Object model = Jax.toObject(input, type);
    assertThat(Jax.toDocument(Jax.toJson(model))).isEqualTo(Jax.toDocument(json));
    String output = extension.equals("json") ? Jax.toJson(model) : Jax.toXml(model);
    Object reread = Jax.toObject(output, type);
    assertThat(Jax.toDocument(Jax.toJson(reread))).isEqualTo(Jax.toDocument(json));
  }

  @DisplayName("Separate XML classes preserve attributes at multiple levels")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"pojo", "with-constructor"})
  void roundTripsSeparateXmlClassesWithAttributes(String style) throws Exception {
    String xml = """
        <Person id="P1">
          <address code="HOME"><city>London</city></address>
          <item sku="A"><name>Pen</name></item>
          <item sku="B"><name>Book</name></item>
        </Person>
        """;
    CliResult result = runCli(style, "xml", xml);
    Class<?> type = compileGenerated(generatedSource(result));
    assertThat(Files.readString(tempDir.resolve("Address.java"))).contains("@XmlAttribute(\"code\")");
    assertThat(Files.readString(tempDir.resolve("Item.java"))).contains("@XmlAttribute(\"sku\")");
    Object model = Jax.toObject(xml, type);
    String output = Jax.toXml(model);
    assertThat(output).contains("id=\"P1\"", "code=\"HOME\"", "sku=\"A\"", "sku=\"B\"");
    assertThat(Jax.toDocument(Jax.toJson(Jax.toObject(output, type))))
        .isEqualTo(Jax.toDocument(Jax.toJson(model)));
  }

  @DisplayName("The supplied incomplete supply-chain XML produces no Java files")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"record", "pojo", "with-constructor"})
  void rejectsIncompleteSupplyChainManifestWithoutCreatingFiles(String style) throws Exception {
    String xml;
    try (InputStream fixture = getClass().getResourceAsStream("incomplete-supply-chain-manifest.xml")) {
      assertThat(fixture).isNotNull();
      xml = new String(fixture.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(xml.stripTrailing()).endsWith("<wms:LineItems>");
    CliResult result = runCli(style, "xml", xml);
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.output()).contains("Failed to parse XML sample").doesNotContain("Created ");
    try (var files = Files.list(tempDir)) {
      assertThat(files.filter(path -> path.toString().endsWith(".java")).toList()).isEmpty();
    }
  }

  @DisplayName("The complete supply-chain XML generates separate classes and preserves all XML values")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"pojo", "with-constructor"})
  void roundTripsCompleteSupplyChainManifest(String style) throws Exception {
    String xml;
    // Repairs pasted markup only; all supplied values and namespace URIs are retained.
    try (InputStream fixture = getClass().getResourceAsStream("supply-chain-manifest.xml")) {
      assertThat(fixture).isNotNull();
      xml = new String(fixture.readAllBytes(), StandardCharsets.UTF_8);
    }
    CliResult result = runCli(style, "xml", xml);
    assertThat(result.exitCode()).as(result.output()).isZero();
    Class<?> type = compileGenerated(tempDir.resolve("log$colon$GlobalSupplyChainManifest.java"));
    Object manifest = Jax.toObject(xml, type);
    String output = Jax.toXml(manifest);
    assertThat(xmlValues(output)).containsExactlyInAnyOrderEntriesOf(xmlValues(xml));
    Object reread = Jax.toObject(output, type);
    assertThat(xmlValues(Jax.toXml(reread))).isEqualTo(xmlValues(output));
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
    List<Path> sources;
    try (var files = Files.list(source.getParent())) {
      sources = files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }
    List<String> arguments = new ArrayList<>(List.of(
        "-parameters", "-classpath", System.getProperty("java.class.path"), "-d", classes.toString()));
    sources.forEach(path -> arguments.add(path.toString()));
    int compilerResult = ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new));
    assertThat(compilerResult).isZero();
    URLClassLoader classLoader = new URLClassLoader(new URL[]{classes.toUri().toURL()});
    classLoaders.add(classLoader);
    for (Path file : sources) {
      String name = file.getFileName().toString().replace(".java", "");
      Class<?> generated = classLoader.loadClass(name);
      assertThat(generated.getEnclosingClass()).as("%s is a top-level type", name).isNull();
      assertThat(generated.getDeclaredClasses()).as("%s contains no nested types", name).isEmpty();
    }
    return classLoader.loadClass(source.getFileName().toString().replace(".java", ""));
  }

  record CliResult(int exitCode, String output) {}
}
