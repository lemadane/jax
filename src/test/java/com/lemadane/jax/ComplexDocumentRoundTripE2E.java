package com.lemadane.jax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.lemadane.jax.XmlRoundTripSnapshot.xmlValues;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.exceptions.MissingDataException;

class ComplexDocumentRoundTripE2E {

  @TempDir
  Path tempDir;
  final List<URLClassLoader> classLoaders = new ArrayList<>();

  @AfterEach
  void closeModels() throws Exception {
    for (URLClassLoader loader : classLoaders) {
      loader.close();
    }
  }

  @Test
  @DisplayName("Public Document API preserves the complete complex JSON, including absent and null properties")
  void organizationDocumentRetainsOriginalDataExactly() throws Exception {
    Document original = Jax.toDocument(fixture("complex-organization.json"));
    String output = Jax.toJson(original);
    Document restored = Jax.toDocument(output);
    assertThat(restored).isEqualTo(original);
    assertThat(Jax.toDocument(Jax.toJson(restored))).isEqualTo(original);
  }

  @ParameterizedTest(name = "Complex JSON API round trip: {0}")
  @ValueSource(strings = {"record", "pojo", "with-constructor"})
  void organizationModelsPreserveAllValuesWithOnlyDocumentedDefaults(String style) throws Exception {
    String input = fixture("complex-organization.json");
    Class<?> type = compileSavedModel("complex-organization", style, "ComplexOrganization", 23);
    Object model;
    if (type.isRecord()) {
      assertThatThrownBy(() -> Jax.toObject(input, type)).isInstanceOf(MissingDataException.class);
      model = Jax.toPartialObject(input, type);
    } else {
      model = Jax.toObject(input, type);
    }

    Document original = Jax.toDocument(input);
    Document restored = Jax.toDocument(Jax.toJson(model));
    List<?> milestones = milestones(restored);
    Object expectedDefault = type.isRecord() ? List.of() : null;
    Document first = (Document) milestones.get(0);
    Document second = (Document) milestones.get(1);
    assertThat(first).containsKey("blockers");
    assertThat(second).containsKey("deliverables");
    assertThat(first.get("blockers")).isEqualTo(expectedDefault);
    assertThat(second.get("deliverables")).isEqualTo(expectedDefault);
    assertThat((Document) milestones(original).get(0)).doesNotContainKey("blockers");
    assertThat((Document) milestones(original).get(1)).doesNotContainKey("deliverables");

    // The second pass is strict and stable, including the two explicitly tested defaults.
    Object reread = Jax.toObject(Jax.toJson(model), type);
    assertThat(Jax.toDocument(Jax.toJson(reread))).isEqualTo(restored);

    // Remove ONLY these two known additions; every original property, list item,
    // null, string, boolean and numeric value must otherwise match the input.
    first.remove("blockers");
    second.remove("deliverables");
    assertThat(restored).usingRecursiveComparison()
        .withComparatorForType((left, right) -> new BigDecimal(left.toString())
            .compareTo(new BigDecimal(right.toString())), Number.class)
        .isEqualTo(original);
  }

  @ParameterizedTest(name = "Supply-chain XML API round trip: {0}")
  @ValueSource(strings = {"pojo", "with-constructor"})
  void supplyChainModelsPreserveEveryXmlElementAttributeAndValue(String style) throws Exception {
    String input = fixture("supply-chain-manifest.xml");
    Class<?> type = compileSavedModel("supply-chain", style, "log$colon$GlobalSupplyChainManifest", 64);
    Object model = Jax.toObject(input, type);
    String output = Jax.toXml(model);
    assertThat(xmlValues(output)).containsExactlyInAnyOrderEntriesOf(xmlValues(input));

    Object restored = Jax.toObject(output, type);
    assertThat(xmlValues(Jax.toXml(restored))).isEqualTo(xmlValues(input));
  }

  static List<?> milestones(Document root) {
    Document organization = (Document) root.get("organization");
    Document department = (Document) ((List<?>) organization.get("departments")).get(0);
    Document project = (Document) ((List<?>) department.get("projects")).get(0);
    return (List<?>) project.get("milestones");
  }

  static String fixture(String name) throws Exception {
    try (var stream = ComplexDocumentRoundTripE2E.class.getResourceAsStream("/com/lemadane/jax/cli/" + name)) {
      assertThat(stream).as("Complete supplied fixture: %s", name).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  Class<?> compileSavedModel(String example, String style, String rootName, int expectedFiles) throws Exception {
    Path directory = Path.of(System.getProperty("jax.examples.path", "examples"), example, style);
    List<Path> sources;
    try (var files = Files.list(directory)) {
      sources = files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }
    assertThat(sources).hasSize(expectedFiles);
    Path classes = Files.createDirectory(tempDir.resolve(example + "-" + style));
    List<String> arguments = new ArrayList<>(List.of(
        "-parameters", "-classpath", System.getProperty("java.class.path"), "-d", classes.toString()));
    sources.forEach(path -> arguments.add(path.toString()));
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler).as("A JDK is required to compile the saved examples").isNotNull();
    assertThat(compiler.run(null, null, null, arguments.toArray(String[]::new))).isZero();
    URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()});
    classLoaders.add(loader);
    for (Path source : sources) {
      Class<?> generated = loader.loadClass(source.getFileName().toString().replace(".java", ""));
      assertThat(generated.getEnclosingClass()).isNull();
      assertThat(generated.getDeclaredClasses()).isEmpty();
    }
    return loader.loadClass(rootName);
  }
}
