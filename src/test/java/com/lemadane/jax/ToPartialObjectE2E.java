package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToPartialObjectE2E {

  public record JsonOrder(
      String order$hyphen$id,
      String amount$dot$due,
      String price$dollar$value,
      JsonCustomer customer,
      List<JsonLine> lines,
      Optional<String> note
  ) {}

  public record JsonCustomer(String name, String address$dot$line1) {}

  public record JsonLine(String sku$hyphen$code, int quantity, BigDecimal unit$dot$price) {}

  public record fxg$Partial$hyphen$Order(
      fxg$Package$dot$Info fxg$package$dot$info,
      List<fxg$Line$hyphen$Item> fxg$line$hyphen$item,
      String fxg$price$u00B7$net
  ) {}

  public record fxg$Package$dot$Info(
      String fxg$display$dot$name,
      String fxg$description$hyphen$text
  ) {}

  public record fxg$Line$hyphen$Item(String fxg$sku$hyphen$code, int fxg$qty) {}

  public static class fxg$Partial$hyphen$Shipment {
    @XmlAttribute("xmlns:fxg")
    String fxg$xmlns;

    @XmlAttribute("fxg:shipment-id")
    String shipment$hyphen$id;

    @XmlAttribute("revision.number")
    int revision$dot$number;

    fxg$Partial$hyphen$Package fxg$package$hyphen$info;
    List<fxg$Partial$hyphen$Line> fxg$line$hyphen$item;

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

    public fxg$Partial$hyphen$Package fxg$package$hyphen$info() {
      return fxg$package$hyphen$info;
    }

    @JsonProperty
    public void fxg$package$hyphen$info(fxg$Partial$hyphen$Package value) {
      fxg$package$hyphen$info = value;
    }

    public List<fxg$Partial$hyphen$Line> fxg$line$hyphen$item() {
      return fxg$line$hyphen$item;
    }

    @JsonProperty
    public void fxg$line$hyphen$item(List<fxg$Partial$hyphen$Line> value) {
      fxg$line$hyphen$item = value;
    }
  }

  public static class fxg$Partial$hyphen$Package {
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

  public static class fxg$Partial$hyphen$Line {
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

  @DisplayName("End-to-end partial JSON mapping ignores unknown properties recursively and applies defaults")
  @Test
  void testEndToEndPartialJson() {
    String json = """
        {
          "order-id": "ORD-PARTIAL",
          "price$value": "$10.00",
          "customer": {
          "name": "Ada",
          "unknown.nested": "ignored"
          },
          "lines": [
          {"sku-code": "A-1", "unit.price": 10.00, "unknown-field": true}
          ],
          "unknown top-level": {"still": "ignored"}
        }
        """;

    JsonOrder order = Jax.toPartialObject(json, JsonOrder.class);

    assertThat(order.order$hyphen$id()).isEqualTo("ORD-PARTIAL");
    assertThat(order.amount$dot$due()).isNull();
    assertThat(order.price$dollar$value()).isEqualTo("$10.00");
    assertThat(order.customer()).isEqualTo(new JsonCustomer("Ada", null));
    assertThat(order.lines()).containsExactly(new JsonLine("A-1", 0, new BigDecimal("10.00")));
    assertThat(order.note()).isEmpty();
  }

  @DisplayName("End-to-end partial XML record mapping ignores unknown elements recursively and applies defaults")
  @Test
  void testEndToEndPartialXmlRecord() {
    String xml = """
        <fxg:Partial-Order xmlns:fxg="urn:example:shipment" ignored-attribute="ignored">
          <fxg:package.info>
          <fxg:display.name>Partial package</fxg:display.name>
          <fxg:unknown-child>ignored</fxg:unknown-child>
          </fxg:package.info>
          <fxg:line-item>
          <fxg:sku-code>A-1</fxg:sku-code>
          <fxg:unknown-quantity>99</fxg:unknown-quantity>
          </fxg:line-item>
          <fxg:unknown-root>ignored</fxg:unknown-root>
        </fxg:Partial-Order>
        """;

    fxg$Partial$hyphen$Order order = Jax.toPartialObject(xml, fxg$Partial$hyphen$Order.class);

    assertThat(order.fxg$package$dot$info())
        .isEqualTo(new fxg$Package$dot$Info("Partial package", null));
    assertThat(order.fxg$line$hyphen$item()).containsExactly(new fxg$Line$hyphen$Item("A-1", 0));
    assertThat(order.fxg$price$u00B7$net()).isNull();
  }

  @DisplayName("End-to-end partial XML POJO mapping retains present attributes and ignores unknown nested elements")
  @Test
  void testEndToEndPartialXmlPojo() {
    String xml = """
        <fxg:Partial-Shipment xmlns:fxg="urn:example:shipment" fxg:shipment-id="S-PARTIAL">
          <fxg:package-info>
          <fxg:description-text>Partial description</fxg:description-text>
          <fxg:unknown-child>ignored</fxg:unknown-child>
          </fxg:package-info>
          <fxg:line-item><fxg:qty>4</fxg:qty><fxg:unknown-child>ignored</fxg:unknown-child></fxg:line-item>
          <fxg:unknown-root>ignored</fxg:unknown-root>
        </fxg:Partial-Shipment>
        """;

    fxg$Partial$hyphen$Shipment shipment = Jax.toPartialObject(xml, fxg$Partial$hyphen$Shipment.class);

    assertThat(shipment.fxg$xmlns()).isEqualTo("urn:example:shipment");
    assertThat(shipment.shipment$hyphen$id()).isEqualTo("S-PARTIAL");
    assertThat(shipment.revision$dot$number()).isZero();
    assertThat(shipment.fxg$package$hyphen$info().fxg$display$dot$name()).isNull();
    assertThat(shipment.fxg$package$hyphen$info().fxg$description$hyphen$text())
        .isEqualTo("Partial description");
    assertThat(shipment.fxg$line$hyphen$item()).hasSize(1);
    assertThat(shipment.fxg$line$hyphen$item().get(0).fxg$qty()).isEqualTo(4);
    assertThat(shipment.fxg$line$hyphen$item().get(0).fxg$sku$hyphen$code()).isNull();
  }
}
