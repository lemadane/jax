package com.lemadane.jax.json.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.JsonMappingException;

class JsonStringFastPathTest {
  @Test
  void preservesLongUnicodeStringsBeforeAndAfterEscapes() {
    String prefix = "plain text é 中文 😀 ".repeat(100);
    String suffix = "more text 😀".repeat(100);
    String encoded = "\"" + prefix + "\\n\\u0041\\\\\\\"" + suffix + "\"";
    var parsed = (JsonParser.JsonNode.JsonStringNode) JsonParser.parse(encoded);
    String expected = prefix + "\nA\\\"" + suffix;
    assertThat(parsed.value()).isEqualTo(expected);
    assertThat(JsonRecordWriter.escapeString(expected)).isEqualTo(prefix + "\\nA\\\\\\\"" + suffix);
    assertThat(((JsonParser.JsonNode.JsonStringNode) JsonParser.parse("\"" + prefix + "\"")).value()).isEqualTo(prefix);
    assertThat(JsonRecordWriter.escapeString(prefix)).isEqualTo(prefix);
  }

  @Test
  void rejectsControlsAndIncompleteStringsOnBothPaths() {
    for (String input : new String[] {"\"plain\ntext\"", "\"plain\\nthen\ttext\"", "\"plain", "\"plain\\n"}) {
      assertThatThrownBy(() -> JsonParser.parse(input)).isInstanceOf(JsonMappingException.class);
    }
  }
}
