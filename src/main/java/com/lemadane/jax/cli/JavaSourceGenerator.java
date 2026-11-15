package com.lemadane.jax.cli;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.lemadane.jax.exceptions.DataMappingException;
import com.lemadane.jax.json.internal.JsonParser;
import com.lemadane.jax.naming.DataNameCodec;

final class JavaSourceGenerator {

  enum Style {
    RECORD,
    POJO,
    WITH_CONSTRUCTOR;

    static Style fromArgument(String value) {
      return switch (value) {
        case "record" -> RECORD;
        case "pojo" -> POJO;
        case "with-constructor" -> WITH_CONSTRUCTOR;
        default -> throw new IllegalArgumentException(
            "Unknown create style '" + value + "'. Use record, pojo, or with-constructor."
        );
      };
    }
  }

  static final Set<String> JAVA_KEYWORDS = Set.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
      "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
      "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
      "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
      "volatile", "while", "true", "false", "null", "record", "sealed", "permits", "var", "yield"
  );

  JavaSourceGenerator() {
  }

  static String generate(String className, String input, boolean xml, Style style) {
    Schema root = xml ? parseXml(input, className) : parseJson(input, className);
    if (xml && style == Style.RECORD && containsAttributes(root)) {
      throw new DataMappingException(
          "Record generation from XML with attributes is unsupported; use pojo or with-constructor."
      );
    }
    StringBuilder source = new StringBuilder();
    if (style != Style.RECORD) {
      source.append("import com.lemadane.jax.JsonProperty;\n\n");
    }
    if (xml && containsAttributes(root)) {
      source.append("import com.lemadane.jax.XmlAttribute;\n\n");
    }
    if (containsLists(root)) {
      source.append("import java.util.List;\n\n");
    }
    renderType(source, root, true, style, 0);
    return source.toString();
  }

  static Schema parseJson(String input, String className) {
    JsonParser.JsonNode node = JsonParser.parse(input);
    if (!(node instanceof JsonParser.JsonNode.JsonObjectNode objectNode)) {
      throw new DataMappingException("JSON input must have an object at the root.");
    }
    return jsonObject(className, objectNode);
  }

  static Schema jsonObject(String name, JsonParser.JsonNode.JsonObjectNode objectNode) {
    Schema schema = new Schema(name);
    for (Map.Entry<String, JsonParser.JsonNode> entry : objectNode.members().entrySet()) {
      addField(schema, javaName(entry.getKey()), jsonField(entry.getKey(), entry.getValue()));
    }
    return schema;
  }

  static FieldSchema jsonField(String externalName, JsonParser.JsonNode value) {
    if (value instanceof JsonParser.JsonNode.JsonArrayNode array) {
      FieldSchema field = new FieldSchema(externalName, javaName(externalName));
      field.list = true;
      if (!array.elements().isEmpty()) {
        field.type = mergeValues(capitalize(field.name), array.elements());
      }
      return field;
    }
    FieldSchema field = new FieldSchema(externalName, javaName(externalName));
    field.type = jsonType(capitalize(field.name), value);
    return field;
  }

  static TypeSchema jsonType(String name, JsonParser.JsonNode value) {
    if (value instanceof JsonParser.JsonNode.JsonObjectNode objectNode) {
      return TypeSchema.object(jsonObject(name, objectNode));
    }
    if (value instanceof JsonParser.JsonNode.JsonStringNode) {
      return TypeSchema.scalar("String");
    }
    if (value instanceof JsonParser.JsonNode.JsonBooleanNode) {
      return TypeSchema.scalar("boolean");
    }
    if (value instanceof JsonParser.JsonNode.JsonNumberNode number) {
      BigDecimal decimal = number.bigDecimalValue();
      if (decimal.stripTrailingZeros().scale() <= 0) {
        if (decimal.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
            && decimal.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) {
          return TypeSchema.scalar("int");
        }
        if (decimal.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
            && decimal.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0) {
          return TypeSchema.scalar("long");
        }
        return TypeSchema.scalar("java.math.BigInteger");
      }
      return TypeSchema.scalar("java.math.BigDecimal");
    }
    return TypeSchema.scalar("String");
  }

  static TypeSchema mergeValues(String name, List<JsonParser.JsonNode> values) {
    JsonParser.JsonNode first = values.get(0);
    if (first instanceof JsonParser.JsonNode.JsonObjectNode firstObject) {
      Schema merged = jsonObject(name, firstObject);
      for (int i = 1; i < values.size(); i++) {
        if (values.get(i) instanceof JsonParser.JsonNode.JsonObjectNode nextObject) {
          mergeObject(merged, jsonObject(name, nextObject));
        }
      }
      return TypeSchema.object(merged);
    }
    return jsonType(name, first);
  }

  static void mergeObject(Schema target, Schema source) {
    for (Map.Entry<String, FieldSchema> entry : source.fields.entrySet()) {
      FieldSchema existing = target.fields.get(entry.getKey());
      if (existing == null) {
        addField(target, entry.getKey(), entry.getValue());
      } else if (!existing.externalName.equals(entry.getValue().externalName)
          || existing.attribute != entry.getValue().attribute) {
        throw nameCollision(existing.externalName, entry.getValue().externalName, entry.getKey());
      }
    }
  }

  static void addField(Schema schema, String javaName, FieldSchema field) {
    FieldSchema existing = schema.fields.get(javaName);
    if (existing != null && (!existing.externalName.equals(field.externalName)
        || existing.attribute != field.attribute)) {
      throw nameCollision(existing.externalName, field.externalName, javaName);
    }
    schema.fields.put(javaName, field);
  }

  static DataMappingException nameCollision(String first, String second, String javaName) {
    return new DataMappingException(
        "External property names \"" + first + "\" and \"" + second
            + "\" both map to Java identifier \"" + javaName + "\"."
    );
  }

  static Schema parseXml(String input, String className) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
      throw new XMLStreamException("External XML entities are disabled");
    });

    try {
      XMLStreamReader reader = factory.createXMLStreamReader(new java.io.StringReader(input));
      try {
        ArrayDeque<XmlNode> stack = new ArrayDeque<>();
        XmlNode root = null;
        while (reader.hasNext()) {
          int event = reader.next();
          if (event == XMLStreamConstants.DTD) {
            throw new XMLStreamException("DOCTYPE declarations are disallowed");
          }
          if (event == XMLStreamConstants.START_ELEMENT) {
            XmlNode node = new XmlNode(qualifiedName(reader.getPrefix(), reader.getLocalName()));
            for (int i = 0; i < reader.getAttributeCount(); i++) {
              node.attributes.put(qualifiedName(reader.getAttributePrefix(i), reader.getAttributeLocalName(i)),
                  reader.getAttributeValue(i));
            }
            if (stack.isEmpty()) {
              if (root != null) {
                throw new XMLStreamException("Multiple document elements");
              }
              root = node;
            } else {
              stack.peek().children.add(node);
            }
            stack.push(node);
          } else if (event == XMLStreamConstants.END_ELEMENT) {
            if (stack.isEmpty()) {
              throw new XMLStreamException("Unexpected end element");
            }
            stack.pop();
          } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA
              || event == XMLStreamConstants.SPACE) {
            if (!stack.isEmpty()) {
              stack.peek().text.append(reader.getText());
            }
          }
        }
        if (root == null || !stack.isEmpty()) {
          throw new XMLStreamException("XML document has no complete root element");
        }
        if (root.children.isEmpty() && !root.text.toString().isBlank()) {
          throw new DataMappingException("A text-only XML root cannot be mapped to a generated class.");
        }
        return xmlObject(className, root);
      } finally {
        reader.close();
      }
    } catch (XMLStreamException | IllegalArgumentException e) {
      throw new DataMappingException("Failed to parse XML sample: " + e.getMessage(), e);
    }
  }

  static Schema xmlObject(String name, XmlNode node) {
    if (!node.text.toString().isBlank() && (!node.children.isEmpty() || !node.attributes.isEmpty())) {
      throw new DataMappingException("Mixed XML content cannot be represented by generated properties.");
    }
    Schema schema = new Schema(name);
    for (Map.Entry<String, String> entry : node.attributes.entrySet()) {
      if (entry.getKey().equals("xmlns") || entry.getKey().startsWith("xmlns:")) {
        continue;
      }
      FieldSchema field = new FieldSchema(entry.getKey(), javaName(entry.getKey()));
      field.attribute = true;
      field.type = scalarFromText(entry.getValue());
      addField(schema, field.name, field);
    }
    Map<String, List<XmlNode>> grouped = new LinkedHashMap<>();
    Map<String, String> externalNamesByJavaName = new LinkedHashMap<>();
    for (XmlNode child : node.children) {
      String javaName = javaName(child.name);
      String previousExternalName = externalNamesByJavaName.putIfAbsent(javaName, child.name);
      if (previousExternalName != null && !previousExternalName.equals(child.name)) {
        throw nameCollision(previousExternalName, child.name, javaName);
      }
      grouped.computeIfAbsent(javaName, ignored -> new ArrayList<>()).add(child);
    }
    for (Map.Entry<String, List<XmlNode>> entry : grouped.entrySet()) {
      List<XmlNode> occurrences = entry.getValue();
      String externalName = occurrences.get(0).name;
      FieldSchema field = new FieldSchema(externalName, entry.getKey());
      field.list = occurrences.size() > 1;
      XmlNode first = occurrences.get(0);
      if (!first.attributes.isEmpty() || !first.children.isEmpty()) {
        Schema nested = xmlObject(capitalize(field.name), first);
        for (int i = 1; i < occurrences.size(); i++) {
          mergeObject(nested, xmlObject(capitalize(field.name), occurrences.get(i)));
        }
        field.type = TypeSchema.object(nested);
      } else {
        field.type = scalarFromText(first.text.toString());
      }
      addField(schema, field.name, field);
    }
    return schema;
  }

  static TypeSchema scalarFromText(String value) {
    String trimmed = value.trim();
    if (trimmed.equals("true") || trimmed.equals("false")) {
      return TypeSchema.scalar("boolean");
    }
    if (trimmed.matches("-?(0|[1-9][0-9]*)")) {
      try {
        return jsonType("Value", new JsonParser.JsonNode.JsonNumberNode(trimmed, new BigDecimal(trimmed)));
      } catch (NumberFormatException ignored) {
        return TypeSchema.scalar("String");
      }
    }
    if (trimmed.matches("-?(0|[1-9][0-9]*)\\.[0-9]+")) {
      return TypeSchema.scalar("java.math.BigDecimal");
    }
    return TypeSchema.scalar("String");
  }

  static void renderType(StringBuilder source, Schema schema, boolean topLevel, Style style, int indent) {
    String pad = "    ".repeat(indent);
    if (style == Style.RECORD) {
      source.append(pad);
      source.append("public ");
      source.append("record ").append(schema.name).append('(');
      int index = 0;
      for (FieldSchema field : schema.fields.values()) {
        if (index++ > 0) {
          source.append(", ");
        }
        if (field.attribute) {
          source.append("@XmlAttribute(\"").append(javaString(field.externalName)).append("\") ");
        }
        source.append(field.typeName()).append(' ').append(field.name);
      }
      source.append(") {\n");
      renderNestedTypes(source, schema, style, indent + 1);
      source.append(pad).append("}\n");
      return;
    }

    source.append(pad);
    if (topLevel) {
      source.append("public ");
    } else {
      source.append("public static ");
    }
    source.append("class ").append(schema.name).append(" {\n");
    for (FieldSchema field : schema.fields.values()) {
      source.append(pad).append("    ");
      if (field.attribute) {
        source.append("@XmlAttribute(\"").append(javaString(field.externalName)).append("\") ");
      }
      source.append(field.typeName()).append(' ').append(field.name).append(";\n");
    }
    if (style == Style.WITH_CONSTRUCTOR) {
      source.append('\n').append(pad).append("    public ").append(schema.name).append("() {\n")
          .append(pad).append("    }\n");
      source.append('\n').append(pad).append("    public ").append(schema.name).append('(');
      appendParameters(source, schema, false);
      source.append(") {\n");
      for (FieldSchema field : schema.fields.values()) {
        source.append(pad).append("        this.").append(field.name).append(" = ").append(field.name)
            .append(";\n");
      }
      source.append(pad).append("    }\n");
    }
    for (FieldSchema field : schema.fields.values()) {
      source.append('\n').append(pad).append("    public ").append(field.typeName()).append(' ')
          .append(field.name).append("() {\n")
          .append(pad).append("        return ").append(field.name).append(";\n")
          .append(pad).append("    }\n")
          .append(pad).append("    @JsonProperty\n")
          .append(pad).append("    public void ").append(field.name).append('(')
          .append(field.typeName()).append(" value) {\n")
          .append(pad).append("        this.").append(field.name).append(" = value;\n")
          .append(pad).append("    }\n");
    }
    renderNestedTypes(source, schema, style, indent + 1);
    source.append(pad).append("}\n");
  }

  static void appendParameters(StringBuilder source, Schema schema, boolean attributesOnly) {
    int index = 0;
    for (FieldSchema field : schema.fields.values()) {
      if (attributesOnly && !field.attribute) {
        continue;
      }
      if (index++ > 0) {
        source.append(", ");
      }
      if (field.attribute) {
        source.append("@XmlAttribute(\"").append(javaString(field.externalName)).append("\") ");
      }
      source.append(field.typeName()).append(' ').append(field.name);
    }
  }

  static void renderNestedTypes(StringBuilder source, Schema schema, Style style, int indent) {
    for (FieldSchema field : schema.fields.values()) {
      if (field.type.object != null) {
        source.append('\n');
        renderType(source, field.type.object, false, style, indent);
      }
    }
  }

  static boolean containsAttributes(Schema schema) {
    for (FieldSchema field : schema.fields.values()) {
      if (field.attribute || (field.type.object != null && containsAttributes(field.type.object))) {
        return true;
      }
    }
    return false;
  }

  static boolean containsLists(Schema schema) {
    for (FieldSchema field : schema.fields.values()) {
      if (field.list || (field.type.object != null && containsLists(field.type.object))) {
        return true;
      }
    }
    return false;
  }

  static String javaName(String externalName) {
    String encoded = DataNameCodec.encode(externalName);
    if (encoded.isEmpty()) {
      encoded = "value";
    }
    if (JAVA_KEYWORDS.contains(encoded)) {
      encoded = "$u" + String.format(Locale.ROOT, "%04X", (int) encoded.charAt(0)) + "$"
          + encoded.substring(1);
    }
    return encoded;
  }

  static String capitalize(String value) {
    if (value.isEmpty()) {
      return "Value";
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  static String qualifiedName(String prefix, String localName) {
    return prefix == null || prefix.isEmpty() ? localName : prefix + ":" + localName;
  }

  static String javaString(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  static final class Schema {
    final String name;
    final Map<String, FieldSchema> fields = new LinkedHashMap<>();

    Schema(String name) {
      this.name = name;
    }
  }

  static final class FieldSchema {
    final String externalName;
    final String name;
    TypeSchema type = TypeSchema.scalar("Object");
    boolean list;
    boolean attribute;

    FieldSchema(String externalName, String name) {
      this.externalName = externalName;
      this.name = name;
    }

    String typeName() {
      String value = type.object == null ? type.scalar : type.object.name;
      return list ? "List<" + boxed(value) + ">" : value;
    }
  }

  static final class TypeSchema {
    final String scalar;
    final Schema object;

    TypeSchema(String scalar, Schema object) {
      this.scalar = scalar;
      this.object = object;
    }

    static TypeSchema scalar(String type) {
      return new TypeSchema(type, null);
    }

    static TypeSchema object(Schema schema) {
      return new TypeSchema(null, schema);
    }
  }

  static String boxed(String type) {
    return switch (type) {
      case "boolean" -> "Boolean";
      case "byte" -> "Byte";
      case "short" -> "Short";
      case "int" -> "Integer";
      case "long" -> "Long";
      case "float" -> "Float";
      case "double" -> "Double";
      case "char" -> "Character";
      default -> type;
    };
  }

  static final class XmlNode {
    final String name;
    final Map<String, String> attributes = new LinkedHashMap<>();
    final List<XmlNode> children = new ArrayList<>();
    final StringBuilder text = new StringBuilder();

    XmlNode(String name) {
      this.name = name;
    }
  }
}
