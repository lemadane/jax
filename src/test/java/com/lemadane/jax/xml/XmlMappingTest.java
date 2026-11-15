package com.lemadane.jax.xml;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.MissingDataException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;

import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlMappingTest {

  // --- Record Definitions for Testing ---

  public record SimpleFacility(String FacilityID) {}

  public record ScanDateTime(String Date, String Time24hr, String Microseconds) {}

  public record PlaceHolder() {}

  public record VanLoadScan(
      String FacilityID,
      String ScanType,
      String ScannedBarcode,
      ScanDateTime ScanDateTime,
      PlaceHolder PlaceHolder
  ) {
    static String Version;

    public static String version() {
      return Version;
    }
  }

  public record FacilityInboundEvent(
      VanLoadScan VanLoadScan,
      PlaceHolder PlaceHolder
  ) {
    static String Version;

    public static String version() {
      return Version;
    }
  }

  public record EventWithList(
      int eventMessageCount,
      List<VanLoadScan> VanLoadScan
  ) {}

  public record StaticFieldsRecord(String id) {
    public static String publicAttr;
    protected static String protectedAttr;
    static String pkgAttr;
    static String privateAttr;

    public static String getPrivateAttr() {
      return privateAttr;
    }
  }

  public record FinalStaticRecord(String id) {
    static final String Version = "1.0";
  }

  public record fxg$FacilityInboundEvent(
      fxg$VanLoadScan fxg$VanLoadScan
  ) {
    static String xmlns$fxg;

    public static String namespace() {
      return xmlns$fxg;
    }
  }

  public record fxg$VanLoadScan(
      String fxg$FacilityID
  ) {}

  public record PartialRecord(
      String id,
      int count,
      boolean active,
      Optional<String> description,
      List<String> tags
  ) {}

  // --- Tests ---

  @DisplayName("Strict Parse Flat Record")
  @Test
  void testStrictParseFlatRecord() {
    String xml = "<SimpleFacility><FacilityID>0603</FacilityID></SimpleFacility>";
    SimpleFacility facility = Jax.toObject(xml, SimpleFacility.class);
    assertThat(facility.FacilityID()).isEqualTo("0603");
  }

  @DisplayName("Strict Parse Nested Records And Ignore Static Attributes")
  @Test
  void testStrictParseNestedRecordsAndStaticAttributes() {
    String xml = """
        <FacilityInboundEvent Version="1.0">
          <VanLoadScan Version="2.5">
            <FacilityID>0603</FacilityID>
            <ScanType>V3</ScanType>
            <ScannedBarcode>9611019061319607196193</ScannedBarcode>
            <ScanDateTime>
              <Date>20210804</Date>
              <Time24hr>072512</Time24hr>
              <Microseconds>527412</Microseconds>
            </ScanDateTime>
            <PlaceHolder/>
          </VanLoadScan>
          <PlaceHolder/>
        </FacilityInboundEvent>
        """;

    FacilityInboundEvent event = Jax.toObject(xml, FacilityInboundEvent.class);

    assertThat(FacilityInboundEvent.version()).isNull();
    assertThat(VanLoadScan.version()).isNull();
    assertThat(event.VanLoadScan().FacilityID()).isEqualTo("0603");
    assertThat(event.VanLoadScan().ScanType()).isEqualTo("V3");
    assertThat(event.VanLoadScan().ScanDateTime().Date()).isEqualTo("20210804");
    assertThat(event.PlaceHolder()).isNotNull();
  }

  @DisplayName("Repeated Elements Into List")
  @Test
  void testRepeatedElementsIntoList() {
    String xml = """
        <EventWithList>
          <eventMessageCount>2</eventMessageCount>
          <VanLoadScan>
            <FacilityID>0603</FacilityID>
            <ScanType>V3</ScanType>
            <ScannedBarcode>111</ScannedBarcode>
            <ScanDateTime>
              <Date>20210804</Date>
              <Time24hr>072512</Time24hr>
              <Microseconds>527412</Microseconds>
            </ScanDateTime>
            <PlaceHolder/>
          </VanLoadScan>
          <VanLoadScan>
            <FacilityID>0417</FacilityID>
            <ScanType>I3</ScanType>
            <ScannedBarcode>222</ScannedBarcode>
            <ScanDateTime>
              <Date>20210805</Date>
              <Time24hr>080000</Time24hr>
              <Microseconds>000000</Microseconds>
            </ScanDateTime>
            <PlaceHolder/>
          </VanLoadScan>
        </EventWithList>
        """;

    EventWithList event = Jax.toObject(xml, EventWithList.class);
    assertThat(event.eventMessageCount()).isEqualTo(2);
    assertThat(event.VanLoadScan()).hasSize(2);
    assertThat(event.VanLoadScan().get(0).FacilityID()).isEqualTo("0603");
    assertThat(event.VanLoadScan().get(1).FacilityID()).isEqualTo("0417");
  }

  @DisplayName("Xml Attributes Do Not Mutate Static Fields")
  @Test
  void testXmlAttributesDoNotMutateStaticFields() {
    StaticFieldsRecord.publicAttr = "before-public";
    StaticFieldsRecord.protectedAttr = "before-protected";
    StaticFieldsRecord.pkgAttr = "before-package";
    StaticFieldsRecord.privateAttr = "before-private";
    String xml = """
        <StaticFieldsRecord publicAttr="pub" protectedAttr="prot" pkgAttr="pkg" privateAttr="priv">
          <id>S123</id><Extra>invalid</Extra>
        </StaticFieldsRecord>
        """;

    assertThatThrownBy(() -> Jax.toObject(xml, StaticFieldsRecord.class))
        .isInstanceOf(ExcessDataException.class);
    assertThat(StaticFieldsRecord.publicAttr).isEqualTo("before-public");
    assertThat(StaticFieldsRecord.protectedAttr).isEqualTo("before-protected");
    assertThat(StaticFieldsRecord.pkgAttr).isEqualTo("before-package");
    assertThat(StaticFieldsRecord.getPrivateAttr()).isEqualTo("before-private");
  }

  @DisplayName("Final Static Xml Attribute Is Ignored")
  @Test
  void testFinalStaticFieldIsNotSerializedAsXmlAttribute() {
    String xml = "<FinalStaticRecord Version=\"2.0\"><id>1</id></FinalStaticRecord>";
    assertThat(Jax.toObject(xml, FinalStaticRecord.class).id()).isEqualTo("1");
    assertThat(Jax.toXml(new FinalStaticRecord("1"))).isEqualTo("<FinalStaticRecord><id>1</id></FinalStaticRecord>");
  }

  @DisplayName("Strict Parse Missing Element Throws")
  @Test
  void testStrictParseMissingElementThrows() {
    String xml = "<SimpleFacility></SimpleFacility>";
    assertThatThrownBy(() -> Jax.toObject(xml, SimpleFacility.class))
        .isInstanceOf(MissingDataException.class)
        .hasMessageContaining("Missing XML element <FacilityID>");
  }

  @DisplayName("Strict Parse Excess Element Throws")
  @Test
  void testStrictParseExcessElementThrows() {
    String xml = "<SimpleFacility><FacilityID>0603</FacilityID><Extra>value</Extra></SimpleFacility>";
    assertThatThrownBy(() -> Jax.toObject(xml, SimpleFacility.class))
        .isInstanceOf(ExcessDataException.class)
        .hasMessageContaining("Excess XML element <Extra>");
  }

  @DisplayName("Duplicate Element For Non List Throws")
  @Test
  void testDuplicateElementForNonListThrows() {
    String xml = "<SimpleFacility><FacilityID>0603</FacilityID><FacilityID>0417</FacilityID></SimpleFacility>";
    assertThatThrownBy(() -> Jax.toObject(xml, SimpleFacility.class))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("Multiple XML elements");
  }

  @DisplayName("Root Name Mismatch Throws")
  @Test
  void testRootNameMismatchThrows() {
    String xml = "<WrongRoot><FacilityID>0603</FacilityID></WrongRoot>";
    assertThatThrownBy(() -> Jax.toObject(xml, SimpleFacility.class))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("Root XML element <WrongRoot> does not match requested record type");
  }

  @DisplayName("Namespaced Xml Mapping")
  @Test
  void testNamespacedXmlMapping() {
    fxg$FacilityInboundEvent.xmlns$fxg = "urn:original";
    String xml = """
        <fxg:FacilityInboundEvent xmlns:fxg="urn:fedex:facility">
          <fxg:VanLoadScan>
            <fxg:FacilityID>0603</fxg:FacilityID>
          </fxg:VanLoadScan>
        </fxg:FacilityInboundEvent>
        """;

    fxg$FacilityInboundEvent event = Jax.toObject(xml, fxg$FacilityInboundEvent.class);
    assertThat(fxg$FacilityInboundEvent.namespace()).isEqualTo("urn:original");
    assertThat(event.fxg$VanLoadScan().fxg$FacilityID()).isEqualTo("0603");

    assertThatThrownBy(() -> Jax.toXml(event))
        .isInstanceOf(XmlMappingException.class)
        .hasMessageContaining("without a namespace declaration for prefix fxg");
  }

  @DisplayName("Partial Parsing Missing Values Default")
  @Test
  void testPartialParsingMissingValuesDefault() {
    String xml = "<PartialRecord><id>P1</id></PartialRecord>";
    PartialRecord partial = Jax.toPartialObject(xml, PartialRecord.class);

    assertThat(partial.id()).isEqualTo("P1");
    assertThat(partial.count()).isEqualTo(0);
    assertThat(partial.active()).isFalse();
    assertThat(partial.description()).isEmpty();
    assertThat(partial.tags()).isEmpty();
  }

  @DisplayName("Partial Parsing Present Malformed Value Throws")
  @Test
  void testPartialParsingPresentMalformedValueThrows() {
    String xml = "<PartialRecord><id>P1</id><count>abc</count></PartialRecord>";
    assertThatThrownBy(() -> Jax.toPartialObject(xml, PartialRecord.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("Cannot convert value \"abc\" to int");
  }

  @DisplayName("Xml Security Xxe Rejection")
  @Test
  void testXmlSecurityXxeRejection() {
    String xmlWithDtd = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
        <SimpleFacility>
          <FacilityID>&xxe;</FacilityID>
        </SimpleFacility>
        """;

    assertThatThrownBy(() -> Jax.toObject(xmlWithDtd, SimpleFacility.class))
        .isInstanceOf(XmlMappingException.class);
  }   

  @DisplayName("Stringification And Round Trip")
  @Test
  void testStringificationAndRoundTrip() {
    SimpleFacility original = new SimpleFacility("0603");
    String xml = Jax.toXml(original);
    assertThat(xml).isEqualTo("<SimpleFacility><FacilityID>0603</FacilityID></SimpleFacility>");

    SimpleFacility roundTripped = Jax.toObject(xml, SimpleFacility.class);
    assertThat(roundTripped).isEqualTo(original);
  }

  @DisplayName("Text And Attribute Escaping")
  @Test
  void testTextAndAttributeEscaping() {
    record EscapedRecord(String text) {
    }

    EscapedRecord rec = new EscapedRecord("Hello & <World>");

    String xml = Jax.toXml(rec);
    assertThat(xml).doesNotContain("attr=");
    assertThat(xml).contains("<text>Hello &amp; &lt;World&gt;</text>");
  }
}
