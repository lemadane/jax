package com.lemadane.jax.json.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.Jax;
import com.lemadane.jax.exceptions.JsonMappingException;

import static org.assertj.core.api.Assertions.*;

class JsonDirectWriterTest {
  record Envelope(Object value) {}
  record Names(String log$colon$Name, String unit$hyphen$price, String a$space$b, String dollar$dollar$key) {}
  record Colliding(String a$b, String a$colon$b) {}
  enum State { READY }

  public static class Link {
    public Object value;
  }

  public static class Parent {
    public String value = "parent";
  }

  public static class Child extends Parent {
    public String value = "child";
  }

  public static class IgnoredName {
     String $uNOPE$ = "ignored";
    public String value = "kept";
  }

  public static class BrokenGetter {
     String value;
    public String getValue() {
      throw new IllegalStateException("getter failed");
    }
  }

  record BrokenRecord(String value) {
    @Override
    public String value() {
      throw new IllegalStateException("record accessor failed");
    }
  }

  static String treeWrite(Object value) {
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    StringBuilder output = new StringBuilder();
    JsonRecordWriter.writeNode(JsonRecordWriter.toNode(value, visited), output);
    return output.toString();
  }

  @Test
  void matchesTreeOutputAcrossScalarTypesEscapesAndContainers() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("\"\\\n", Arrays.asList(null, true, false, '\t', "😀\b\f\r\n\t\"\\", ""));
    values.put("numbers", List.of((byte) 1, (short) 2, 3, 4L, 5.25f, -0.0d,
        new BigInteger("999999999999999999999999"), new BigDecimal("123.4500"), new AtomicInteger(7)));
    values.put("arrays", new Object[] {new int[] {1, 2}, new boolean[] {true, false}, new Object[0]});
    values.put("optional", List.of(Optional.empty(), Optional.of(State.READY)));
    values.put("date", LocalDate.of(2026, 11, 15));
    values.put("empty", Map.of());
    values.put("names", new Names("a", "b", "c", "d"));
    assertThat(Jax.toJson(values)).isEqualTo(treeWrite(values));
    assertThat(Jax.toJson(values)).contains("\"log:Name\"", "\"unit-price\"", "\"a b\"", "\"dollar$key\"");
  }

  @Test
  void preservesCollisionAndInheritedShadowingBehavior() {
    assertThat(Jax.toJson(new Colliding("first", "last"))).isEqualTo("{\"a:b\":\"last\"}");
    assertThat(Jax.toJson(new Child())).isEqualTo(treeWrite(new Child())).isEqualTo("{\"value\":\"parent\"}");
  }

  @Test
  void preservesUnusualMapKeysAndIgnoredPrivateNames() {
    Map<String, Object> map = new IdentityHashMap<>();
    map.put(new String("same"), "first");
    map.put(new String("same"), "last");
    assertThat(Jax.toJson(map)).isEqualTo(treeWrite(map));
    assertThat(Jax.toJson(new IgnoredName())).isEqualTo("{\"value\":\"kept\"}");
  }

  @Test
  void matchesTreeOutputForDeterministicRandomGraphs() {
    Random random = new Random(827341);
    for (int i = 0; i < 200; i++) {
      Envelope value = new Envelope(graph(random, 4));
      assertThat(Jax.toJson(value)).isEqualTo(treeWrite(value));
    }
  }

   static Object graph(Random random, int depth) {
    if (depth == 0) {
      return random.nextBoolean() ? "text\n\"\\😀" : random.nextLong();
    }
    return switch (random.nextInt(6)) {
      case 0 -> null;
      case 1 -> Optional.ofNullable(graph(random, depth - 1));
      case 2 -> new Envelope(graph(random, depth - 1));
      case 3 -> Arrays.asList(graph(random, depth - 1), graph(random, depth - 1));
      case 4 -> new Object[] {graph(random, depth - 1), graph(random, depth - 1)};
      default -> Collections.singletonMap("key:with-special.name", graph(random, depth - 1));
    };
  }

  @Test
  void detectsCyclesButAllowsSharedChildrenAndRecoversAfterFailure() {
    List<Object> list = new ArrayList<>();
    list.add(list);
    Object[] array = new Object[1];
    array[0] = array;
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("self", map);
    List<Object> recordCycle = new ArrayList<>();
    Envelope record = new Envelope(recordCycle);
    recordCycle.add(record);
    Link link = new Link();
    link.value = link;
    for (Object value : List.of(list, array, map, record, link)) {
      assertThatThrownBy(() -> Jax.toJson(value)).isInstanceOf(JsonMappingException.class).hasMessageContaining("Cycle detected");
      assertThatThrownBy(() -> treeWrite(value)).isInstanceOf(JsonMappingException.class).hasMessageContaining("Cycle detected");
    }
    Envelope shared = new Envelope("ok");
    assertThat(Jax.toJson(List.of(shared, shared))).isEqualTo("[{\"value\":\"ok\"},{\"value\":\"ok\"}]");
  }

  @Test
  void preservesFailuresForNonFiniteNumbersInvalidKeysAndFailingAccessors() {
    for (Number number : List.of(Double.NaN, Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN)) {
      assertThatThrownBy(() -> Jax.toJson(number)).isInstanceOf(JsonMappingException.class).hasMessageContaining("Non-finite");
      assertThatThrownBy(() -> treeWrite(number)).isInstanceOf(JsonMappingException.class).hasMessageContaining("Non-finite");
    }
    Number invalid = new AtomicInteger(1) {
      @Override
      public String toString() {
        return "not-a-number";
      }
    };
    assertThatThrownBy(() -> Jax.toJson(invalid)).isInstanceOf(NumberFormatException.class);
    BigDecimal extreme = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE);
    assertThatThrownBy(() -> Jax.toJson(extreme)).isInstanceOf(NumberFormatException.class);
    assertThatThrownBy(() -> Jax.toJson(Map.of(1, "bad"))).isInstanceOf(JsonMappingException.class).hasMessageContaining("keys must be strings");
    assertThatThrownBy(() -> treeWrite(Map.of(1, "bad"))).isInstanceOf(JsonMappingException.class).hasMessageContaining("keys must be strings");
    assertThatThrownBy(() -> Jax.toJson(new BrokenGetter())).isInstanceOf(JsonMappingException.class).hasMessageContaining("POJO properties");
    assertThatThrownBy(() -> Jax.toJson(new BrokenRecord("bad"))).isInstanceOf(JsonMappingException.class).hasMessageContaining("record component");
    assertThatThrownBy(() -> treeWrite(new BrokenGetter())).isInstanceOf(JsonMappingException.class).hasMessageContaining("POJO properties");
    assertThatThrownBy(() -> treeWrite(new BrokenRecord("bad"))).isInstanceOf(JsonMappingException.class).hasMessageContaining("record component");
  }
}
