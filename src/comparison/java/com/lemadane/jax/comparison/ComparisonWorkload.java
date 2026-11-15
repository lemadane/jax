package com.lemadane.jax.comparison;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lemadane.jax.Jax;

/** Inputs, models and reusable mapper instances shared by correctness tests and JMH. */
public final class ComparisonWorkload {
  public final String json;
  public final String xml;
  public final Class<?> jsonType;
  public final Class<?> xmlType;
  public final ObjectMapper jacksonJson;
  public final XmlMapper jacksonXml;
  public final Gson gson;

  public ComparisonWorkload() throws Exception {
    json = resource("complex-organization.json");
    xml = resource("supply-chain-manifest.xml");
    jsonType = Class.forName("ComplexOrganization");
    xmlType = Class.forName("log$colon$GlobalSupplyChainManifest");
    jacksonJson = new ObjectMapper();
    fieldsOnly(jacksonJson);
    XMLInputFactory input = XMLInputFactory.newFactory();
    input.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    input.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    jacksonXml = new XmlMapper(XmlFactory.builder().xmlInputFactory(input).build());
    fieldsOnly(jacksonXml);
    jacksonXml.setAnnotationIntrospector(new JaxXmlIntrospector(namespaces(xml)));
    jacksonXml.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    gson = new GsonBuilder().serializeNulls().create();
  }

   static void fieldsOnly(ObjectMapper mapper) {
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  public Object jaxJsonRoundTrip() {
    return Jax.toObject(Jax.toJson(Jax.toObject(json, jsonType)), jsonType);
  }

  public Object jaxJsonTreeWriterRoundTrip() {
    return Jax.toObject(com.lemadane.jax.json.internal.JsonTreeWriterBaseline.stringify(Jax.toObject(json, jsonType)), jsonType);
  }

  public Object jaxXmlTreeWriterRoundTrip() {
    return Jax.toObject(com.lemadane.jax.xml.internal.XmlTreeWriterBaseline.stringify(Jax.toObject(xml, xmlType)), xmlType);
  }

  public Object jacksonJsonRoundTrip() throws Exception {
    return jacksonJson.readValue(jacksonJson.writeValueAsString(jacksonJson.readValue(json, jsonType)), jsonType);
  }

  public Object gsonJsonRoundTrip() {
    return gson.fromJson(gson.toJson(gson.fromJson(json, jsonType)), jsonType);
  }

  public Object jaxXmlRoundTrip() {
    return Jax.toObject(Jax.toXml(Jax.toObject(xml, xmlType)), xmlType);
  }

  public Object jacksonXmlRoundTrip() throws Exception {
    return jacksonXml.readValue(jacksonXml.writeValueAsString(jacksonXml.readValue(xml, xmlType)), xmlType);
  }

   static String resource(String name) throws Exception {
    try (var stream = ComparisonWorkload.class.getResourceAsStream("/com/lemadane/jax/cli/" + name)) {
      if (stream == null) {
        throw new IllegalStateException("Missing comparison fixture " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

   static Map<String, String> namespaces(String xml) throws Exception {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    var reader = factory.createXMLStreamReader(new java.io.StringReader(xml));
    try {
      reader.nextTag();
      Map<String, String> result = new LinkedHashMap<>();
      for (int i = 0; i < reader.getNamespaceCount(); i++) {
        result.put(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
      }
      return result;
    } finally {
      reader.close();
    }
  }
}
