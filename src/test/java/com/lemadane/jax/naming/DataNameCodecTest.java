package com.lemadane.jax.naming;

import org.junit.jupiter.api.Test;

import com.lemadane.jax.exceptions.NameEncodingException;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataNameCodecTest {

  @DisplayName("Colon Encoding")
  @Test
  void testColonEncoding() {
    String encoded = DataNameCodec.encode("fxg:FacilityInboundEvent");
    assertThat(encoded).isEqualTo("fxg$colon$FacilityInboundEvent");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("fxg:FacilityInboundEvent");
  }

  @DisplayName("Xmlns Prefix Encoding")
  @Test
  void testXmlnsPrefixEncoding() {
    String encoded = DataNameCodec.encode("xmlns:fxg");
    assertThat(encoded).isEqualTo("xmlns$colon$fxg");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("xmlns:fxg");
  }

  @DisplayName("Hyphen Encoding")
  @Test
  void testHyphenEncoding() {
    String encoded = DataNameCodec.encode("Facility-ID");
    assertThat(encoded).isEqualTo("Facility$hyphen$ID");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("Facility-ID");
  }

  @DisplayName("Dot Encoding")
  @Test
  void testDotEncoding() {
    String encoded = DataNameCodec.encode("Facility.ID");
    assertThat(encoded).isEqualTo("Facility$dot$ID");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("Facility.ID");
  }

  @DisplayName("Space Encoding")
  @Test
  void testSpaceEncoding() {
    String encoded = DataNameCodec.encode("display name");
    assertThat(encoded).isEqualTo("display$space$name");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("display name");
  }

  @DisplayName("Dollar Encoding")
  @Test
  void testDollarEncoding() {
    String encoded = DataNameCodec.encode("price$value");
    assertThat(encoded).isEqualTo("price$dollar$value");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("price$value");
  }

  @DisplayName("Unicode Escape Encoding")
  @Test
  void testUnicodeEscapeEncoding() {
    String input = "emoji_😀_test";
    String encoded = DataNameCodec.encode(input);
    assertThat(encoded).contains("$u1F600$");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo(input);
  }

  @DisplayName("Digit At Start Encoding")
  @Test
  void testDigitAtStartEncoding() {
    String encoded = DataNameCodec.encode("0603");
    assertThat(encoded).startsWith("$u0030$");
    assertThat(DataNameCodec.decode(encoded)).isEqualTo("0603");
  }

  @DisplayName("Empty String")
  @Test
  void testEmptyString() {
    assertThat(DataNameCodec.encode("")).isEqualTo("");
    assertThat(DataNameCodec.decode("")).isEqualTo("");
  }

  @DisplayName("Invalid Unicode Escape Throws")
  @Test
  void testInvalidUnicodeEscapeThrows() {
    assertThatThrownBy(() -> DataNameCodec.decode("$uXYZ$"))
        .isInstanceOf(NameEncodingException.class);

    assertThatThrownBy(() -> DataNameCodec.decode("$u123"))
        .isInstanceOf(NameEncodingException.class);
  }

  @DisplayName("Collision Resistance")
  @Test
  void testCollisionResistance() {
    String enc1 = DataNameCodec.encode("a-b");
    String enc2 = DataNameCodec.encode("a.b");
    String enc3 = DataNameCodec.encode("a:b");
    String enc4 = DataNameCodec.encode("a b");
    String enc5 = DataNameCodec.encode("a$b");
    String enc6 = DataNameCodec.encode("a:hyphen:b");

    assertThat(SetOf(enc1, enc2, enc3, enc4, enc5, enc6)).hasSize(6);
    assertThat(DataNameCodec.decode(enc6)).isEqualTo("a:hyphen:b");
  }

  static java.util.Set<String> SetOf(String... items) {
    return java.util.Set.of(items);
  }
}
