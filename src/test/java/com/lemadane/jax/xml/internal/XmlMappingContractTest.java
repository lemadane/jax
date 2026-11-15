package com.lemadane.jax.xml.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class XmlMappingContractTest {
  record Value(String value) {}

  public static class Pojo {
    public String value = "text";
  }

  public static class ParameterAttribute {
    public ParameterAttribute(@XmlAttribute String id) { /* Annotation fixture. */ }
  }

  @Test
  void serializesPropertiesThroughDirectHelperEntryPoints() {
    Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    XmlElement record = new XmlElement("root");
    XmlObjectWriter.appendRecordProperties(new Value("text"), record, seen);
    XmlElement pojo = new XmlElement("root");
    XmlObjectWriter.appendPojoProperties(new Pojo(), pojo, seen);
    assertThat(record.textContent()).isEqualTo("text");
    assertThat(pojo.textContent()).isEqualTo("text");
    assertThat(seen).isEmpty();
  }

  @Test
  void rejectsWrongMappingEntryPointAndNullRecordArguments() {
    assertThatThrownBy(() -> XmlObjectReader.toObject("<Value/>", Value.class))
        .isInstanceOf(RecordConstructionException.class);
    assertThatThrownBy(() -> XmlRecordReader.parse(null, "<Value/>", true))
        .isInstanceOf(NullPointerException.class).hasMessageContaining("recordType");
    assertThatThrownBy(() -> XmlRecordReader.parse(Value.class, null, true))
        .isInstanceOf(NullPointerException.class).hasMessageContaining("xml");
  }

  @Test
  void rejectsParameterOnlyAnnotationDuringAttributeSerialization() throws Exception {
    var parameter = ParameterAttribute.class.getConstructor(String.class).getParameters()[0];
    assertThatThrownBy(() -> XmlObjectWriter.xmlAttributeName(parameter))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("only supported on fields");
    assertThat(XmlObjectWriter.isValidXmlNamePart("")).isFalse();
  }
  public abstract static class AbstractValue {
    public AbstractValue(String value) { /* Cannot instantiate an abstract target. */ }
  }

  @Test
  void wrapsInstantiationFailureInConstructorHelper() {
    XmlElement root = XmlRecordReader.parseXmlDocument("<AbstractValue><value>x</value></AbstractValue>");
    assertThatThrownBy(() -> XmlObjectReader.createFromConstructor(AbstractValue.class, root, root.children(), "/root", true))
        .isInstanceOf(XmlMappingException.class).hasCauseInstanceOf(InstantiationException.class)
        .hasMessageContaining("Failed to invoke matching constructor");
  }

}
