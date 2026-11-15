package com.lemadane.jax.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JaxCliValidationTest {
  @Test
  void reportsUsageAndUnsupportedInputExtensions() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
      assertThat(JaxCli.run(new String[0], stream, stream)).isEqualTo(2);
      assertThat(output.toString(StandardCharsets.UTF_8)).contains("Usage:");
      output.reset();
      assertThat(JaxCli.run(new String[] {"--create", "record", "sample.txt"}, stream, stream)).isEqualTo(1);
      assertThat(output.toString(StandardCharsets.UTF_8)).contains(".json or .xml");
    }
  }

  @Test
  void handlesMissingExtensionsAndNumericOrPunctuationOnlyNames() {
    assertThat(JaxCli.extension("sample")).isEmpty();
    assertThat(JaxCli.extension("sample.")).isEmpty();
    assertThat(JaxCli.extension("sample.JSON")).isEqualTo("json");
    assertThat(JaxCli.className("123")).isEqualTo("_123");
    assertThat(JaxCli.className("---")).isEqualTo("Generated");
  }
}
