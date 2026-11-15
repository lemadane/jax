package com.lemadane.jax;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.TypeConversionException;

import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedAndEdgeCasesTest {

  public static class OrdinaryClass {
    String name;
  }

  public record ValidRecord(String name) {}

  public record FailingConstructorRecord(String val) {
    public FailingConstructorRecord {
      if ("invalid".equals(val)) {
        throw new IllegalArgumentException("Validation failed in constructor");
      }
    }
  }

  @DisplayName("Xml Null Arguments Rejected")
  @Test
  void testXmlNullArgumentsRejected() {
    assertThatThrownBy(() -> Jax.toObject(null, ValidRecord.class))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toObject("<xml/>", null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toXml(null))
        .isInstanceOf(NullPointerException.class);
  }

  @DisplayName("Json Null Arguments Rejected")
  @Test
  void testJsonNullArgumentsRejected() {
    assertThatThrownBy(() -> Jax.toObject("{}", null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toObject(null, ValidRecord.class))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toJson(null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toPartialObject(null, ValidRecord.class))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> Jax.toPartialObject("{}", null))
        .isInstanceOf(NullPointerException.class);
  }

  @DisplayName("Mutable Class Target Accepted")
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testMutableClassTargetAccepted() {
    Class clazz = OrdinaryClass.class;

    assertThat(Jax.toObject("{}", clazz)).isInstanceOf(OrdinaryClass.class);
    assertThat(Jax.toObject("<OrdinaryClass/>", clazz)).isInstanceOf(OrdinaryClass.class);

    assertThat(Jax.toPartialObject("{}", clazz)).isInstanceOf(OrdinaryClass.class);
  }

  @DisplayName("Record Constructor Failure Reporting")
  @Test
  void testRecordConstructorFailureReporting() {
    String xml = "<FailingConstructorRecord><val>invalid</val></FailingConstructorRecord>";
    assertThatThrownBy(() -> Jax.toObject(xml, FailingConstructorRecord.class))
        .isInstanceOf(RecordConstructionException.class)
        .hasMessageContaining("Validation failed in constructor");

    String json = "{\"val\":\"invalid\"}";
    assertThatThrownBy(() -> Jax.toObject(json, FailingConstructorRecord.class))
        .isInstanceOf(RecordConstructionException.class)
        .hasMessageContaining("Validation failed in constructor");
  }

  @DisplayName("Concurrent Metadata Caching")
  @Test
  void testConcurrentMetadataCaching() throws InterruptedException {
    int threads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger successCount = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < 100; j++) {
            ValidRecord rec = Jax.toObject("{\"name\":\"test\"}", ValidRecord.class);
            assertThat(rec.name()).isEqualTo("test");
          }
          successCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    boolean completed = latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(completed).isTrue();
    assertThat(successCount.get()).isEqualTo(threads);
  }

  @DisplayName("Exception Path Rich Messages")
  @Test
  void testExceptionPathRichMessages() {
    record Sub(int count) {}
    record Root(Sub sub) {}

    String xml = "<Root><sub><count>invalid_int</count></sub></Root>";
    assertThatThrownBy(() -> Jax.toObject(xml, Root.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("/Root/sub/count");

    String json = "{\"sub\":{\"count\":\"invalid_int\"}}";
    assertThatThrownBy(() -> Jax.toObject(json, Root.class))
        .isInstanceOf(TypeConversionException.class)
        .hasMessageContaining("$.sub.count");
  }
}
