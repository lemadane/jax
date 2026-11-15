package com.lemadane.jax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.ExcessDataException;

class PojoCacheConcurrencyTest {
  public static class InheritedValue {
     String display$space$name;

    public String getDisplay$space$name() {
      return display$space$name;
    }

    @JsonProperty
    public void setDisplay$space$name(String value) {
      display$space$name = value;
    }
  }

  public static class CachedJson extends InheritedValue {
     int sequence;

    public int getSequence() {
      return sequence;
    }

    @JsonProperty
    public void setSequence(int value) {
      sequence = value;
    }
  }

  public static class CachedXml {
    @XmlAttribute
    public int sequence;
    public List<TextValue> item;
  }

  public static class TextValue {
    @XmlAttribute
    public String unit;
    @XmlText
    public String text;
  }

  @Test
  void concurrentJsonMappingKeepsInheritedAccessorsAndValuesIsolated() throws Exception {
    concurrently(index -> {
      String input = "{\"sequence\":" + index + ",\"display name\":\"value-" + index + "\"}";
      CachedJson restored = Jax.toObject(Jax.toJson(Jax.toObject(input, CachedJson.class)), CachedJson.class);
      assertThat(restored.getSequence()).isEqualTo(index);
      assertThat(restored.getDisplay$space$name()).isEqualTo("value-" + index);
    });
  }

  @Test
  void concurrentXmlMappingKeepsAttributesTextAndListsIsolated() throws Exception {
    concurrently(index -> {
      String input = "<CachedXml sequence=\"" + index + "\"><item unit=\"KG\">" + index
          + "</item><item unit=\"LB\">other</item></CachedXml>";
      CachedXml restored = Jax.toObject(Jax.toXml(Jax.toObject(input, CachedXml.class)), CachedXml.class);
      assertThat(restored.sequence).isEqualTo(index);
      assertThat(restored.item).hasSize(2);
      assertThat(restored.item.get(0).unit).isEqualTo("KG");
      assertThat(restored.item.get(0).text).isEqualTo(Integer.toString(index));
      assertThat(restored.item.get(1).unit).isEqualTo("LB");
      assertThat(restored.item.get(1).text).isEqualTo("other");
    });
  }

  @Test
  void warmMetadataDoesNotSuppressUnknownPropertyValidation() {
    Jax.toObject("{\"sequence\":1}", CachedJson.class);
    Jax.toObject("<CachedXml sequence=\"1\"/>", CachedXml.class);
    for (String name : List.of("firstUnknown", "secondUnknown")) {
      assertThatThrownBy(() -> Jax.toObject("{\"" + name + "\":1}", CachedJson.class))
          .isInstanceOf(ExcessDataException.class).hasMessageContaining(name);
      assertThatThrownBy(() -> Jax.toObject("<CachedXml><" + name + "/></CachedXml>", CachedXml.class))
          .isInstanceOf(ExcessDataException.class).hasMessageContaining(name);
    }
  }

   static void concurrently(java.util.function.IntConsumer check) throws Exception {
    var executor = Executors.newFixedThreadPool(8);
    try {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        int index = i;
        tasks.add(() -> {
          check.accept(index);
          return null;
        });
      }
      for (var future : executor.invokeAll(tasks)) {
        future.get();
      }
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }
}
