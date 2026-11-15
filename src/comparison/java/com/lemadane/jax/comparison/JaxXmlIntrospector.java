package com.lemadane.jax.comparison;

import java.util.Map;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.naming.DataNameCodec;

/** Maps the same saved JAX models to Jackson XML, without a second set of DTOs. */
public final class JaxXmlIntrospector extends JacksonXmlAnnotationIntrospector {
   static final long serialVersionUID = 1L;
   final Map<String, String> namespaces;

  public JaxXmlIntrospector(Map<String, String> namespaces) {
    super(false);
    this.namespaces = Map.copyOf(namespaces);
  }

  @Override
  public boolean hasIgnoreMarker(AnnotatedMember member) {
    String name = externalName(member);
    // XmlMapper manages namespace declarations itself; namespace URIs are retained below.
    return name.equals("xmlns") || name.startsWith("xmlns:") || super.hasIgnoreMarker(member);
  }

  @Override
  public PropertyName findRootName(AnnotatedClass type) {
    return qualified(DataNameCodec.decode(type.getRawType().getSimpleName()));
  }

  @Override
  public PropertyName findNameForSerialization(Annotated member) {
    return member instanceof AnnotatedField ? qualified(externalName(member)) : super.findNameForSerialization(member);
  }

  @Override
  public PropertyName findNameForDeserialization(Annotated member) {
    return member instanceof AnnotatedField ? qualified(externalName(member)) : super.findNameForDeserialization(member);
  }

  @Override
  public String findNamespace(MapperConfig<?> config, Annotated member) {
    return qualified(externalName(member)).getNamespace();
  }

  @Override
  public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated member) {
    return member.hasAnnotation(XmlAttribute.class);
  }

  @Override
  public Boolean isOutputAsText(MapperConfig<?> config, Annotated member) {
    return member.hasAnnotation(XmlText.class);
  }

   String externalName(Annotated member) {
    if (member.hasAnnotation(XmlText.class)) {
      return "text";
    }
    XmlAttribute attribute = member.getAnnotation(XmlAttribute.class);
    return attribute != null && !attribute.value().isEmpty()
        ? attribute.value() : DataNameCodec.decode(member.getName());
  }

   PropertyName qualified(String name) {
    int colon = name.indexOf(':');
    if (colon < 0) {
      return new PropertyName(name, "");
    }
    String prefix = name.substring(0, colon);
    String namespace = namespaces.get(prefix);
    if (namespace == null && !prefix.equals("xmlns")) {
      throw new IllegalArgumentException("Unknown XML prefix in comparison model: " + prefix);
    }
    return new PropertyName(name.substring(colon + 1), namespace);
  }
}
