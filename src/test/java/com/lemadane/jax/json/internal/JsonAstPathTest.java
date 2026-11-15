package com.lemadane.jax.json.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.lemadane.jax.exceptions.JsonMappingException;

class JsonAstPathTest {
  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "{\"first\":[{\"ok\":1}],\"second\":[0,{\"target\":bad}]}|$.second[1].target",
      "{\"branch\":[{},{\"x\":1,\"x\":2}]}|$.branch[1].x",
      "{\"branch\":[1,{\"x\" 2}]}|$.branch[1].x",
      "{\"branch\":[{\"ok\":1},1e10001]}|$.branch[1]",
      "{\"branch\":[{\"ok\":1}] trailing}|$"
  })
  void errorsRetainTheCorrectPathAfterTraversingSiblings(String json, String path) {
    assertThatThrownBy(() -> JsonParser.parse(json)).isInstanceOf(JsonMappingException.class)
        .hasMessageEndingWith("at " + path);
  }

  @Test
  void helpersAcceptIndependentStringPaths() {
    var object = new JsonParser("{\"items\":[true,null,false]}").parseObject("$.object", 1);
    var array = (JsonParser.JsonNode.JsonArrayNode) object.members().get("items");
    assertThat(array.elements()).containsExactly(new JsonParser.JsonNode.JsonBooleanNode(true),
        new JsonParser.JsonNode.JsonNullNode(), new JsonParser.JsonNode.JsonBooleanNode(false));
    assertThat(new JsonParser("[{}]").parseArray("$.array", 1).elements()).hasSize(1);
  }
}
