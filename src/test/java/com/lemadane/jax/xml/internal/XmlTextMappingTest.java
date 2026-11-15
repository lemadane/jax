package com.lemadane.jax.xml.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.exceptions.XmlMappingException;

class XmlTextMappingTest {

  @Test
  void readsPrivateTextAlongsideAttributesAndEscapesItOnWrite() {
    Amount amount = Jax.toObject("<Amount currency=\"EUR\">  001 &amp; &lt;two&gt;\r&#xD;三  </Amount>", Amount.class);
    assertThat(amount.currency).isEqualTo("EUR");
    assertThat(amount.value).isEqualTo("  001 & <two>\n\r三  ");
    String xml = Jax.toXml(amount);
    assertThat(xml).contains("currency=\"EUR\"", "&amp;", "&lt;two&gt;", "&#xD;").doesNotContain("<value>");
    assertThat(Jax.toObject(xml, Amount.class).value).isEqualTo(amount.value);
  }

  @Test
  void handlesEmptyAbsentAndCdataText() {
    assertThat(Jax.toObject("<Amount/>", Amount.class).value).isEmpty();
    assertThat(Jax.toObject("<Amount><![CDATA[<raw>&]]></Amount>", Amount.class).value).isEqualTo("<raw>&");
    Amount amount = new Amount();
    assertThat(Jax.toXml(amount)).isEqualTo("<Amount/>");
    amount.value = "";
    assertThat(Jax.toObject(Jax.toXml(amount), Amount.class).value).isEmpty();
    amount.value = "\u0001";
    assertThatThrownBy(() -> Jax.toXml(amount)).isInstanceOf(XmlMappingException.class).hasMessageContaining("XML 1.0");
  }

  @Test
  void bindsConstructorTextAndAttributeWithoutChildProperties() {
    Price price = Jax.toObject("<Price unit=\"KG\">620.50</Price>", Price.class);
    assertThat(price.value).isEqualByComparingTo("620.50");
    assertThat(price.unit).isEqualTo("KG");
    assertThat(Jax.toXml(price)).isEqualTo("<Price unit=\"KG\">620.50</Price>");
  }

  @Test
  void handlesInheritedTextOptionalAndEnumValues() {
    Derived derived = Jax.toObject("<Derived>inherited</Derived>", Derived.class);
    assertThat(Jax.toXml(derived)).isEqualTo("<Derived>inherited</Derived>");
    OptionalText optional = Jax.toObject("<OptionalText>4.20</OptionalText>", OptionalText.class);
    assertThat(optional.value).contains(new BigDecimal("4.20"));
    assertThat(Jax.toXml(optional)).isEqualTo("<OptionalText>4.20</OptionalText>");
    optional.value = Optional.empty();
    assertThat(Jax.toXml(optional)).isEqualTo("<OptionalText/>");
    StatusText status = Jax.toObject("<StatusText>READY</StatusText>", StatusText.class);
    assertThat(status.value).isEqualTo(Status.READY);
    assertThat(Jax.toXml(status)).isEqualTo("<StatusText>READY</StatusText>");
  }

  @Test
  void convertsNumericTextAndReportsInvalidOrEmptyPrimitiveValues() {
    assertThat(Jax.toObject("<Count>42</Count>", Count.class).value).isEqualTo(42);
    assertThat(Jax.toXml(Jax.toObject("<Count>42</Count>", Count.class))).isEqualTo("<Count>42</Count>");
    assertThatThrownBy(() -> Jax.toObject("<Count>invalid</Count>", Count.class))
        .isInstanceOf(com.lemadane.jax.exceptions.TypeConversionException.class).hasMessageContaining("text()");
    assertThatThrownBy(() -> Jax.toObject("<Count/>", Count.class))
        .isInstanceOf(com.lemadane.jax.exceptions.TypeConversionException.class);
    assertThat(Jax.toObject("<OptionalText/>", OptionalText.class).value).isEmpty();
    assertThatThrownBy(() -> Jax.toObject("<OptionalObject>data</OptionalObject>", OptionalObject.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("scalar");
  }

  @Test
  void rejectsMixedContentInBothDirections() {
    assertThatThrownBy(() -> Jax.toObject("<Mixed>before<child>after</child></Mixed>", Mixed.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("child elements");
    Mixed mixed = new Mixed();
    mixed.child = "after";
    assertThatThrownBy(() -> Jax.toXml(mixed)).isInstanceOf(XmlMappingException.class).hasMessageContaining("child elements");
  }

  @Test
  void rejectsAmbiguousOrStructuredTextAnnotations() {
    for (Class<?> type : List.of(Duplicate.class, Both.class, ArrayText.class, CollectionText.class,
        ObjectText.class, RecordText.class, MapText.class)) {
      assertThatThrownBy(() -> Jax.toObject("<" + type.getSimpleName() + "/>", type))
          .isInstanceOf(XmlMappingException.class);
      assertThatThrownBy(() -> Jax.toXml(type.getDeclaredConstructor().newInstance()))
          .isInstanceOf(XmlMappingException.class);
    }
    assertThatThrownBy(() -> Jax.toObject("<DuplicateParameters>x</DuplicateParameters>", DuplicateParameters.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("one @XmlText constructor parameter");
    assertThatThrownBy(() -> Jax.toObject("<BothParameter>x</BothParameter>", BothParameter.class))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("both @XmlText and @XmlAttribute");
  }

  @Test
  void ignoresStaticAnnotationsAndRejectsStructuredOptionalContent() {
    assertThat(XmlTextSupport.field(StaticText.class)).isNull();
    assertThat(Jax.toXml(new StaticText())).isEqualTo("<StaticText/>");
    OptionalText optional = new OptionalText();
    // Type erasure must not allow a structured value to become arbitrary toString() text.
    @SuppressWarnings({"rawtypes", "unchecked"})
    Optional<BigDecimal> invalid = (Optional) Optional.of(List.of("x"));
    optional.value = invalid;
    assertThatThrownBy(() -> Jax.toXml(optional)).isInstanceOf(XmlMappingException.class).hasMessageContaining("scalar");
  }

  public static class Amount {
    @XmlAttribute public String currency;
    @XmlText String value;
    public String value() {
      return value;
    }
  }

  public static class Price {
    @XmlAttribute final String unit;
    @XmlText final BigDecimal value;
    public Price(@XmlAttribute("unit") String unit, @XmlText BigDecimal value) {
      this.unit = unit;
      this.value = value;
    }
  }

  public static class Derived extends Amount { }
  public static class Count { @XmlText public int value; }
  public static class OptionalObject { @XmlText Optional<Amount> value; }
  public static class OptionalText { @XmlText Optional<BigDecimal> value; }
  public enum Status { READY }
  public static class StatusText { @XmlText Status value; }
  public static class Mixed {
    @XmlText public String value;
    public String child;
  }
  public static class Duplicate extends Amount { @XmlText String another; }
  public static class Both { @XmlText @XmlAttribute String value; }
  public static class ArrayText { @XmlText String[] value; }
  public static class CollectionText { @XmlText List<String> value; }
  public static class ObjectText { @XmlText Amount value; }
  public record Value(String value) { }
  public static class MapText { @XmlText java.util.Map<String, String> value; }
  public static class RecordText { @XmlText Value value; }
  public static class StaticText { @XmlText static String value; }
  public static class DuplicateParameters {
    public DuplicateParameters(@XmlText String first, @XmlText String second) { }
  }
  public static class BothParameter {
    public BothParameter(@XmlText @XmlAttribute("id") String value) { }
  }
}
