package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToXmlE2E {

  enum ShipmentStatus {
    READY,
    IN_TRANSIT
  }

  record XmlOrder(
      String order$hyphen$id,
      String description,
      BigDecimal total,
      boolean expedited,
      ShipmentStatus status,
      Optional<String> instruction,
      XmlCustomer customer,
      List<XmlLine> lines,
      String omitted
  ) {}

  record XmlCustomer(String name, XmlAddress address) {}

  record XmlAddress(String city, String postal$hyphen$code) {}

  record XmlLine(String sku$hyphen$code, int quantity, BigDecimal unit$dot$price) {}

  public static class fxg$Shipment {
    @XmlAttribute("xmlns:fxg")
    String fxg$xmlns;

    @XmlAttribute("fxg:shipment-id")
    String shipment$hyphen$id;

    @XmlAttribute("revision.number")
    int revision$dot$number;

    fxg$Package$hyphen$info fxg$package$hyphen$info;
    List<fxg$Line$hyphen$Item> fxg$line$hyphen$item;

    public String fxg$xmlns() {
      return fxg$xmlns;
    }

    @JsonProperty
    public void fxg$xmlns(String value) {
      fxg$xmlns = value;
    }

    public String shipment$hyphen$id() {
      return shipment$hyphen$id;
    }

    @JsonProperty
    public void shipment$hyphen$id(String value) {
      shipment$hyphen$id = value;
    }

    public int revision$dot$number() {
      return revision$dot$number;
    }

    @JsonProperty
    public void revision$dot$number(int value) {
      revision$dot$number = value;
    }

    public fxg$Package$hyphen$info fxg$package$hyphen$info() {
      return fxg$package$hyphen$info;
    }

    @JsonProperty
    public void fxg$package$hyphen$info(fxg$Package$hyphen$info value) {
      fxg$package$hyphen$info = value;
    }

    public List<fxg$Line$hyphen$Item> fxg$line$hyphen$item() {
      return fxg$line$hyphen$item;
    }

    @JsonProperty
    public void fxg$line$hyphen$item(List<fxg$Line$hyphen$Item> value) {
      fxg$line$hyphen$item = value;
    }
  }

  public static class fxg$Package$hyphen$info {
    String fxg$display$dot$name;
    String fxg$description$hyphen$text;

    public String fxg$display$dot$name() {
      return fxg$display$dot$name;
    }

    @JsonProperty
    public void fxg$display$dot$name(String value) {
      fxg$display$dot$name = value;
    }

    public String fxg$description$hyphen$text() {
      return fxg$description$hyphen$text;
    }

    @JsonProperty
    public void fxg$description$hyphen$text(String value) {
      fxg$description$hyphen$text = value;
    }
  }

  public static class fxg$Line$hyphen$Item {
    String fxg$sku$hyphen$code;
    int fxg$qty;

    public String fxg$sku$hyphen$code() {
      return fxg$sku$hyphen$code;
    }

    @JsonProperty
    public void fxg$sku$hyphen$code(String value) {
      fxg$sku$hyphen$code = value;
    }

    public int fxg$qty() {
      return fxg$qty;
    }

    @JsonProperty
    public void fxg$qty(int value) {
      fxg$qty = value;
    }
  }

  public static class CyclicPojo {
    CyclicPojo child;

    public CyclicPojo child() {
      return child;
    }
  }

  @DisplayName("End-to-end XML serialization emits nested records, repeated elements, scalar values, and escaped text")
  @Test
  void testEndToEndNestedRecordSerializationAndRoundTrip() {
    XmlOrder order = new XmlOrder(
        "ORD-42",
        "Fragile <glass> & insured",
        new BigDecimal("125.50"),
        true,
        ShipmentStatus.IN_TRANSIT,
        Optional.of("Handle with care"),
        new XmlCustomer("Ada & Bob", new XmlAddress("London", "N1 9GU")),
        List.of(
            new XmlLine("A-1", 2, new BigDecimal("12.50")),
            new XmlLine("B-2", 3, new BigDecimal("33.00"))
        ),
        "Not omitted"
    );

    String xml = Jax.toXml(order);

    assertThat(xml).isEqualTo(
        "<XmlOrder><order-id>ORD-42</order-id><description>Fragile &lt;glass&gt; &amp; insured</description>"
            + "<total>125.50</total><expedited>true</expedited><status>IN_TRANSIT</status>"
            + "<instruction>Handle with care</instruction>"
            + "<customer><name>Ada &amp; Bob</name><address><city>London</city>"
            + "<postal-code>N1 9GU</postal-code></address></customer>"
            + "<lines><sku-code>A-1</sku-code><quantity>2</quantity><unit.price>12.50</unit.price></lines>"
            + "<lines><sku-code>B-2</sku-code><quantity>3</quantity><unit.price>33.00</unit.price></lines>"
            + "<omitted>Not omitted</omitted>"
            + "</XmlOrder>"
    );
    assertThat(Jax.toObject(xml, XmlOrder.class)).isEqualTo(order);
  }

  @DisplayName("End-to-end XML serialization emits POJO attributes and nested namespaced elements")
  @Test
  void testEndToEndPojoAttributesAndNamespaces() {
    fxg$Shipment shipment = new fxg$Shipment();
    shipment.fxg$xmlns = "urn:example:shipment";
    shipment.shipment$hyphen$id = "S-42 & \"quoted\"";
    shipment.revision$dot$number = 7;

    fxg$Package$hyphen$info packageInfo = new fxg$Package$hyphen$info();
    packageInfo.fxg$display$dot$name = "Fragile package";
    packageInfo.fxg$description$hyphen$text = "Use & handle with care";
    shipment.fxg$package$hyphen$info = packageInfo;

    fxg$Line$hyphen$Item firstLine = new fxg$Line$hyphen$Item();
    firstLine.fxg$sku$hyphen$code = "A-1";
    firstLine.fxg$qty = 2;
    fxg$Line$hyphen$Item secondLine = new fxg$Line$hyphen$Item();
    secondLine.fxg$sku$hyphen$code = "B-2";
    secondLine.fxg$qty = 3;
    shipment.fxg$line$hyphen$item = List.of(firstLine, secondLine);

    String xml = Jax.toXml(shipment);

    assertThat(xml).isEqualTo(
        "<fxg:Shipment xmlns:fxg=\"urn:example:shipment\" fxg:shipment-id=\"S-42 &amp; &quot;quoted&quot;\""
            + " revision.number=\"7\"><fxg:package-info><fxg:display.name>Fragile package</fxg:display.name>"
            + "<fxg:description-text>Use &amp; handle with care</fxg:description-text></fxg:package-info>"
            + "<fxg:line-item><fxg:sku-code>A-1</fxg:sku-code><fxg:qty>2</fxg:qty></fxg:line-item>"
            + "<fxg:line-item><fxg:sku-code>B-2</fxg:sku-code><fxg:qty>3</fxg:qty></fxg:line-item>"
            + "</fxg:Shipment>"
    );
    fxg$Shipment roundTripped = Jax.toObject(xml, fxg$Shipment.class);
    assertThat(roundTripped.shipment$hyphen$id()).isEqualTo("S-42 & \"quoted\"");
    assertThat(roundTripped.fxg$package$hyphen$info().fxg$description$hyphen$text())
        .isEqualTo("Use & handle with care");
    assertThat(roundTripped.fxg$line$hyphen$item()).extracting(fxg$Line$hyphen$Item::fxg$sku$hyphen$code)
        .containsExactly("A-1", "B-2");
  }

  @DisplayName("End-to-end XML serialization rejects null roots, scalar roots, and unbound prefixes")
  @Test
  void testEndToEndInvalidRootsAndNamespaces() {
    record PrefixedRecord(String fxg$value) {}

    assertThatThrownBy(() -> Jax.toXml(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("object must not be null");
    assertThatThrownBy(() -> Jax.toXml("plain text"))
        .isInstanceOf(com.lemadane.jax.exceptions.XmlMappingException.class)
        .hasMessageContaining("XML root value must be a Java record or POJO");
    assertThatThrownBy(() -> Jax.toXml(new PrefixedRecord("value")))
        .isInstanceOf(com.lemadane.jax.exceptions.XmlMappingException.class)
        .hasMessageContaining("without a namespace declaration");
  }

  @DisplayName("End-to-end XML serialization detects cycles in object graphs")
  @Test
  void testEndToEndCycleDetection() {
    CyclicPojo node = new CyclicPojo();
    node.child = node;

    assertThatThrownBy(() -> Jax.toXml(node))
        .isInstanceOf(com.lemadane.jax.exceptions.XmlMappingException.class)
        .hasMessageContaining("Cycle detected");
  }
}
