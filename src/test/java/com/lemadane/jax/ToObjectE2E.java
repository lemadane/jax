package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToObjectE2E {

  public record JsonOrder(
      String order$hyphen$id,
      String amount$dot$due,
      String display$space$name,
      String price$dollar$value,
      String $u0039$lives,
      String emoji$u1F600$label,
      String fxg$tracking,
      JsonCustomer customer,
      List<JsonLine> lines,
      Optional<String> note
  ) {}

  public record JsonCustomer(String name, String address$dot$line1, String emoji$u1F600$tag) {}

  public record JsonLine(String sku$hyphen$code, int quantity, BigDecimal unit$dot$price) {}

  public record fxg$Record$hyphen$Order(
      fxg$Package$dot$Info fxg$package$dot$info,
      List<fxg$Line$hyphen$RecordItem> fxg$line$hyphen$item,
      String fxg$price$u00B7$net
  ) {}

  public record fxg$Package$dot$Info(
      String fxg$display$dot$name,
      String fxg$description$hyphen$text
  ) {}

  public record fxg$Line$hyphen$RecordItem(String fxg$sku$hyphen$code, int fxg$qty) {}

  public static class fxg$Shipment {

    @XmlAttribute("xmlns:fxg")
    String fxg$xmlns;

    @XmlAttribute("fxg:shipment-id")
    String shipment$hyphen$id;

    @XmlAttribute("revision.number")
    int revision$dot$number;

    fxg$Package$hyphen$info fxg$package$hyphen$info;
    List<fxg$Line$hyphen$Item> fxg$line$hyphen$item;
    String fxg$price$u00B7$net;

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

    public String fxg$price$u00B7$net() {
      return fxg$price$u00B7$net;
    }

    @JsonProperty
    public void fxg$price$u00B7$net(String value) {
      fxg$price$u00B7$net = value;
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

  @DisplayName("End-to-end strict JSON mapping handles deeply nested data and every JSON-safe special property name")
  @Test
  void testEndToEndComplexJson() {
    String json = """
        {
          "order-id": "ORD-42",
          "amount.due": "125.50",
          "display name": "Priority order",
          "price$value": "$125.50",
          "9lives": "nine",
          "emoji😀label": "smile",
          "fxg:tracking": "colon-name",
          "customer": {
          "name": "Ada",
          "address.line1": "12 Analytical Engine Way",
          "emoji😀tag": "🧮"
          },
          "lines": [
          {"sku-code": "A-1", "quantity": 2, "unit.price": 12.50},
          {"sku-code": "B-2", "quantity": 3, "unit.price": 33.00}
          ],
          "note": "Deliver carefully"
        }
        """;

    JsonOrder order = Jax.toObject(json, JsonOrder.class);

    assertThat(order.order$hyphen$id()).isEqualTo("ORD-42");
    assertThat(order.amount$dot$due()).isEqualTo("125.50");
    assertThat(order.display$space$name()).isEqualTo("Priority order");
    assertThat(order.price$dollar$value()).isEqualTo("$125.50");
    assertThat(order.$u0039$lives()).isEqualTo("nine");
    assertThat(order.emoji$u1F600$label()).isEqualTo("smile");
    assertThat(order.fxg$tracking()).isEqualTo("colon-name");
    assertThat(order.customer()).isEqualTo(
        new JsonCustomer("Ada", "12 Analytical Engine Way", "🧮")
    );
    assertThat(order.lines()).containsExactly(
        new JsonLine("A-1", 2, new BigDecimal("12.50")),
        new JsonLine("B-2", 3, new BigDecimal("33.00"))
    );
    assertThat(order.note()).contains("Deliver carefully");

    JsonOrder roundTripped = Jax.toObject(Jax.toJson(order), JsonOrder.class);
    assertThat(roundTripped).isEqualTo(order);
  }

  @DisplayName("End-to-end strict XML record mapping handles nested prefixed, hyphenated, dotted, and Unicode element names")
  @Test
  void testEndToEndComplexXmlRecord() {
    String xml = """
        <fxg:Record-Order xmlns:fxg="urn:example:shipment">
          <fxg:package.info>
          <fxg:display.name>Fragile package</fxg:display.name>
          <fxg:description-text>Use &amp; handle with care</fxg:description-text>
          </fxg:package.info>
          <fxg:line-item><fxg:sku-code>A-1</fxg:sku-code><fxg:qty>2</fxg:qty></fxg:line-item>
          <fxg:line-item><fxg:sku-code>B-2</fxg:sku-code><fxg:qty>3</fxg:qty></fxg:line-item>
          <fxg:price·net>🧮</fxg:price·net>
        </fxg:Record-Order>
        """;

    fxg$Record$hyphen$Order order = Jax.toObject(xml, fxg$Record$hyphen$Order.class);

    assertThat(order.fxg$package$dot$info()).isEqualTo(
        new fxg$Package$dot$Info("Fragile package", "Use & handle with care")
    );
    assertThat(order.fxg$line$hyphen$item()).containsExactly(
        new fxg$Line$hyphen$RecordItem("A-1", 2),
        new fxg$Line$hyphen$RecordItem("B-2", 3)
    );
    assertThat(order.fxg$price$u00B7$net()).isEqualTo("🧮");
  }

  @DisplayName("End-to-end XML POJO mapping retains special-name attributes and round-trips nested repeated elements")
  @Test
  void testEndToEndXmlPojoAttributesAndSpecialNames() {
    String xml = """
        <fxg:Shipment xmlns:fxg="urn:example:shipment"
                fxg:shipment-id="S-42" revision.number="7">
          <fxg:package-info>
          <fxg:display.name>Fragile package</fxg:display.name>
          <fxg:description-text>Use &amp; handle with care</fxg:description-text>
          </fxg:package-info>
          <fxg:line-item><fxg:sku-code>A-1</fxg:sku-code><fxg:qty>2</fxg:qty></fxg:line-item>
          <fxg:line-item><fxg:sku-code>B-2</fxg:sku-code><fxg:qty>3</fxg:qty></fxg:line-item>
          <fxg:price·net>🧮</fxg:price·net>
        </fxg:Shipment>
        """;

    fxg$Shipment shipment = Jax.toObject(xml, fxg$Shipment.class);

    assertThat(shipment.fxg$xmlns()).isEqualTo("urn:example:shipment");
    assertThat(shipment.shipment$hyphen$id()).isEqualTo("S-42");
    assertThat(shipment.revision$dot$number()).isEqualTo(7);
    assertThat(shipment.fxg$package$hyphen$info().fxg$display$dot$name()).isEqualTo("Fragile package");
    assertThat(shipment.fxg$package$hyphen$info().fxg$description$hyphen$text())
        .isEqualTo("Use & handle with care");
    assertThat(shipment.fxg$line$hyphen$item()).extracting(fxg$Line$hyphen$Item::fxg$sku$hyphen$code)
        .containsExactly("A-1", "B-2");
    assertThat(shipment.fxg$price$u00B7$net()).isEqualTo("🧮");

    String serialized = Jax.toXml(shipment);
    assertThat(serialized).contains("xmlns:fxg=\"urn:example:shipment\"")
        .contains("fxg:shipment-id=\"S-42\"")
        .contains("revision.number=\"7\"")
        .contains("<fxg:display.name>Fragile package</fxg:display.name>")
        .contains("<fxg:price·net>🧮</fxg:price·net>");
    assertThat(Jax.toObject(serialized, fxg$Shipment.class).shipment$hyphen$id()).isEqualTo("S-42");
  }
}
