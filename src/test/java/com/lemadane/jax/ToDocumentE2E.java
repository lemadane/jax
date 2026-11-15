package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToDocumentE2E {

  @DisplayName("End-to-end inferred document mapping preserves nested values, order, and scalar number types")
  @Test
  void testEndToEndNestedJsonDocument() {
    String json = """
        {
          "event": "shipment-created",
          "sequence": 2147483648,
          "largeSequence": 9223372036854775808,
          "measurement": 12.750,
          "enabled": true,
          "nothing": null,
          "customer": {
          "display name": "Ada Lovelace",
          "address": {
            "city": "London",
            "postal-code": "N1 9GU"
          }
          },
          "items": [
          {"sku": "A-1", "quantity": 2},
          {"sku": "B-2", "quantity": 3}
          ],
          "tags": ["priority", "insured"]
        }
        """;

    Document document = Jax.toDocument(json);

    assertThat(document.keySet()).containsExactly(
        "event", "sequence", "largeSequence", "measurement", "enabled", "nothing", "customer", "items", "tags"
    );
    assertThat(document.get("event")).isEqualTo("shipment-created");
    assertThat(document.get("sequence")).isInstanceOf(Long.class).isEqualTo(2147483648L);
    assertThat(document.get("largeSequence")).isEqualTo(new BigDecimal("9223372036854775808"));
    assertThat(document.get("measurement")).isEqualTo(new BigDecimal("12.750"));
    assertThat(document.get("enabled")).isEqualTo(true);
    assertThat(document).containsEntry("nothing", null);

    Document customer = (Document) document.get("customer");
    assertThat(customer.get("display name")).isEqualTo("Ada Lovelace");
    Document address = (Document) customer.get("address");
    assertThat(address).containsEntry("city", "London").containsEntry("postal-code", "N1 9GU");

    List<?> items = (List<?>) document.get("items");
    assertThat(items).hasSize(2).allMatch(Document.class::isInstance);
    assertThat(((Document) items.get(0))).containsEntry("sku", "A-1").containsEntry("quantity", 2);
    assertThat(((Document) items.get(1))).containsEntry("sku", "B-2").containsEntry("quantity", 3);
    assertThat(document.get("tags")).isEqualTo(List.of("priority", "insured"));
  }

  @DisplayName("End-to-end inferred document mapping handles empty objects, empty arrays, and integer inference boundaries")
  @Test
  void testEndToEndEmptyAndBoundaryValues() {
    Document document = Jax.toDocument(
        "{\"emptyObject\":{},\"emptyArray\":[],\"minInt\":-2147483648,\"maxInt\":2147483647,"
            + "\"minLong\":-9223372036854775808,\"maxLong\":9223372036854775807}"
    );

    assertThat(document.get("emptyObject")).isEqualTo(new Document());
    assertThat(document.get("emptyArray")).isEqualTo(List.of());
    assertThat(document.get("minInt")).isInstanceOf(Integer.class).isEqualTo(Integer.MIN_VALUE);
    assertThat(document.get("maxInt")).isInstanceOf(Integer.class).isEqualTo(Integer.MAX_VALUE);
    assertThat(document.get("minLong")).isInstanceOf(Long.class).isEqualTo(Long.MIN_VALUE);
    assertThat(document.get("maxLong")).isInstanceOf(Long.class).isEqualTo(Long.MAX_VALUE);
  }

  @DisplayName("End-to-end inferred document mapping rejects non-object roots and null input")
  @Test
  void testEndToEndInvalidDocumentInputs() {
    assertThatThrownBy(() -> Jax.toDocument("[1, 2, 3]"))
        .isInstanceOf(com.lemadane.jax.exceptions.JsonMappingException.class)
        .hasMessageContaining("A JSON object is expected");
    assertThatThrownBy(() -> Jax.toDocument("null"))
        .isInstanceOf(com.lemadane.jax.exceptions.JsonMappingException.class)
        .hasMessageContaining("A JSON object is expected");
    assertThatThrownBy(() -> Jax.toDocument(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("json must not be null");
  }

  @DisplayName("Document map constructor copies existing entries")
  @Test
  void documentCopiesMapEntries() {
    Document document = new Document(Map.of("name", "Ada", "age", 37));

    assertThat(document).containsEntry("name", "Ada").containsEntry("age", 37);
  }
}
