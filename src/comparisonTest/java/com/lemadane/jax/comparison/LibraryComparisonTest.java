package com.lemadane.jax.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lemadane.jax.Jax;

class LibraryComparisonTest {
   static ComparisonWorkload workload;

  @BeforeAll
  static void setup() throws Exception {
    workload = new ComparisonWorkload();
  }

  @ParameterizedTest(name = "Full organization JSON: {0}")
  @ValueSource(strings = {"JAX", "JAX tree writer", "Jackson", "Gson"})
  void preservesCompleteJsonAndExactlyTwoDefaults(String library) throws Exception {
    Object restored;
    String output;
    switch (library) {
      case "JAX" -> {
        restored = workload.jaxJsonRoundTrip();
        output = Jax.toJson(restored);
      }
      case "JAX tree writer" -> {
        restored = workload.jaxJsonTreeWriterRoundTrip();
        output = com.lemadane.jax.json.internal.JsonTreeWriterBaseline.stringify(restored);
        assertThat(output).isEqualTo(Jax.toJson(restored));
      }
      case "Jackson" -> {
        restored = workload.jacksonJsonRoundTrip();
        output = workload.jacksonJson.writeValueAsString(restored);
      }
      case "Gson" -> {
        restored = workload.gsonJsonRoundTrip();
        output = workload.gson.toJson(restored);
      }
      default -> throw new IllegalArgumentException(library);
    }
    var expected = jsonValues(JsonParser.parseString(workload.json));
    expected.put("$/organization/departments/0/projects/0/milestones/0/blockers", "null");
    expected.put("$/organization/departments/0/projects/0/milestones/1/deliverables", "null");
    assertThat(jsonValues(JsonParser.parseString(output))).isEqualTo(expected);
    assertThat(restored.getClass()).isEqualTo(workload.jsonType);
  }

  @ParameterizedTest(name = "Full supply-chain XML: {0}")
  @ValueSource(strings = {"JAX", "JAX tree writer", "Jackson XML"})
  void preservesCompleteXmlByNamespaceUriAndLocalName(String library) throws Exception {
    Object restored;
    String output;
    if (library.equals("JAX tree writer")) {
      restored = workload.jaxXmlTreeWriterRoundTrip();
      output = com.lemadane.jax.xml.internal.XmlTreeWriterBaseline.stringify(restored);
      assertThat(output).isEqualTo(Jax.toXml(restored));
    } else {
      restored = library.equals("JAX") ? workload.jaxXmlRoundTrip() : workload.jacksonXmlRoundTrip();
      output = library.equals("JAX") ? Jax.toXml(restored) : workload.jacksonXml.writeValueAsString(restored);
    }
    assertThat(xmlValues(output)).isEqualTo(xmlValues(workload.xml));
    assertThat(restored.getClass()).isEqualTo(workload.xmlType);
  }

   static Map<String, String> jsonValues(JsonElement root) {
    Map<String, String> result = new TreeMap<>();
    jsonValues(root, "$", result);
    return result;
  }

   static void jsonValues(JsonElement node, String path, Map<String, String> result) {
    if (node.isJsonObject()) {
      result.put(path, "object");
      node.getAsJsonObject().entrySet().forEach(entry -> jsonValues(entry.getValue(), path + "/" + entry.getKey(), result));
    } else if (node.isJsonArray()) {
      result.put(path, "array:" + node.getAsJsonArray().size());
      for (int i = 0; i < node.getAsJsonArray().size(); i++) {
        jsonValues(node.getAsJsonArray().get(i), path + "/" + i, result);
      }
    } else if (node.isJsonNull()) {
      result.put(path, "null");
    } else if (node.getAsJsonPrimitive().isNumber()) {
      result.put(path, "number:" + new BigDecimal(node.getAsString()).stripTrailingZeros().toPlainString());
    } else {
      result.put(path, node.toString());
    }
  }

   static Map<String, String> xmlValues(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var root = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
    Map<String, String> result = new TreeMap<>();
    xmlValues(root.getDocumentElement(), "", result);
    return result;
  }

   static void xmlValues(org.w3c.dom.Element element, String path, Map<String, String> result) {
    path += "/" + expandedName(element);
    result.put(path, "element");
    for (int i = 0; i < element.getAttributes().getLength(); i++) {
      var attribute = element.getAttributes().item(i);
      if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
        result.put(path + "/@" + expandedName(attribute), attribute.getNodeValue());
      }
    }
    Map<String, Integer> counts = new HashMap<>();
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < element.getChildNodes().getLength(); i++) {
      var node = element.getChildNodes().item(i);
      if (node instanceof org.w3c.dom.Element child) {
        String name = expandedName(child);
        int index = counts.merge(name, 1, Integer::sum);
        xmlValues(child, path + "[" + name + ":" + index + "]", result);
      } else if (node instanceof org.w3c.dom.Text) {
        text.append(node.getNodeValue());
      }
    }
    if (counts.isEmpty() || !text.toString().isBlank()) {
      result.put(path + "/text()", text.toString());
    }
  }

   static String expandedName(org.w3c.dom.Node node) {
    return "{" + (node.getNamespaceURI() == null ? "" : node.getNamespaceURI()) + "}" + node.getLocalName();
  }
}
