package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToJsonE2E {

  enum DeliveryStatus {
    READY,
    IN_TRANSIT
  }

  record JsonOrder(
      String order$hyphen$id,
      String display$space$name,
      String emoji$u1F600$label,
      String notes,
      BigDecimal total,
      boolean expedited,
      DeliveryStatus status,
      Optional<String> instruction,
      JsonCustomer customer,
      List<JsonLine> lines,
      String missing
  ) {}

  record JsonCustomer(String name, JsonAddress address) {}

  record JsonAddress(String city, String postal$hyphen$code) {}

  record JsonLine(String sku$hyphen$code, int quantity, BigDecimal unit$dot$price) {}

  public static class AccessorOrder {
    String order$hyphen$id;
    AccessorCustomer customer;
    List<String> tags;
    static String shared = "must not be serialized";
    String ignored = "no public accessor";

    AccessorOrder(String orderId, AccessorCustomer customer, List<String> tags) {
      this.order$hyphen$id = orderId;
      this.customer = customer;
      this.tags = tags;
    }

    public String order$hyphen$id() {
      return order$hyphen$id;
    }

    public AccessorCustomer customer() {
      return customer;
    }

    public List<String> tags() {
      return tags;
    }
  }

  public static class AccessorCustomer {
    String display$dot$name;

    AccessorCustomer(String displayName) {
      display$dot$name = displayName;
    }

    public String display$dot$name() {
      return display$dot$name;
    }
  }

  @DisplayName("End-to-end JSON serialization emits nested records, arrays, scalars, and encoded special names")
  @Test
  void testEndToEndNestedRecordSerializationAndRoundTrip() {
    JsonOrder order = new JsonOrder(
        "ORD-42",
        "Priority order",
        "😀 label",
        "Quote: \"; slash: \\; lines:\n\tend",
        new BigDecimal("125.50"),
        true,
        DeliveryStatus.IN_TRANSIT,
        Optional.empty(),
        new JsonCustomer("Ada Lovelace", new JsonAddress("London", "N1 9GU")),
        List.of(
            new JsonLine("A-1", 2, new BigDecimal("12.50")),
            new JsonLine("B-2", 3, new BigDecimal("33.00"))
        ),
        null
    );

    String json = Jax.toJson(order);

    assertThat(json).isEqualTo(
        "{\"order-id\":\"ORD-42\",\"display name\":\"Priority order\",\"emoji😀label\":\"😀 label\","
            + "\"notes\":\"Quote: \\\"; slash: \\\\; lines:\\n\\tend\",\"total\":125.50,"
            + "\"expedited\":true,\"status\":\"IN_TRANSIT\",\"instruction\":null,"
            + "\"customer\":{\"name\":\"Ada Lovelace\",\"address\":{\"city\":\"London\","
            + "\"postal-code\":\"N1 9GU\"}},\"lines\":[{\"sku-code\":\"A-1\",\"quantity\":2,"
            + "\"unit.price\":12.50},{\"sku-code\":\"B-2\",\"quantity\":3,\"unit.price\":33.00}],"
            + "\"missing\":null}"
    );
    assertThat(Jax.toObject(json, JsonOrder.class)).isEqualTo(order);
  }

  @DisplayName("End-to-end JSON serialization reads public POJO accessors and omits inaccessible or static fields")
  @Test
  void testEndToEndPojoAccessors() {
    AccessorOrder order = new AccessorOrder(
        "ORD-99",
        new AccessorCustomer("Ada's & Co."),
        List.of("fragile", "insured")
    );

    assertThat(Jax.toJson(order)).isEqualTo(
        "{\"order-id\":\"ORD-99\",\"customer\":{\"display.name\":\"Ada's & Co.\"},"
            + "\"tags\":[\"fragile\",\"insured\"]}"
    );
  }

  @DisplayName("End-to-end JSON serialization rejects null roots and non-finite numeric values")
  @Test
  void testEndToEndInvalidValues() {
    record NonFiniteValue(double value) {}

    assertThatThrownBy(() -> Jax.toJson(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("object must not be null");
    assertThatThrownBy(() -> Jax.toJson(new NonFiniteValue(Double.POSITIVE_INFINITY)))
        .isInstanceOf(com.lemadane.jax.exceptions.JsonMappingException.class)
        .hasMessageContaining("Non-finite number value");
  }
}
