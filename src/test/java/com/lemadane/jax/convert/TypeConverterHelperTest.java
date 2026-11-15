package com.lemadane.jax.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.lemadane.jax.exceptions.TypeConversionException;

import static org.assertj.core.api.Assertions.*;

class TypeConverterHelperTest {
  @Test
  void normalizesExactIntegerInputsWithoutRounding() {
    BigDecimal decimal = new BigDecimal("123.50");
    assertThat(TypeConverter.exactInteger(decimal)).isSameAs(decimal);
    assertThat(TypeConverter.exactInteger(new BigInteger("123"))).isEqualTo(new BigDecimal("123"));
    assertThat(TypeConverter.exactInteger(" 123 ")).isEqualTo(new BigDecimal("123"));
    assertThatThrownBy(() -> TypeConverter.convert(int.class, decimal, "$.n"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("$.n");
  }

  @Test
  void recognizesOnlySupportedNumericTypes() {
    for (Class<?> type : List.of(byte.class, Byte.class, short.class, Short.class, int.class, Integer.class,
        long.class, Long.class, float.class, Float.class, double.class, Double.class, BigInteger.class, BigDecimal.class)) {
      assertThat(TypeConverter.isNumericType(type)).as(type.getName()).isTrue();
    }
    for (Class<?> type : List.of(String.class, boolean.class, char.class, Object.class, Number.class)) {
      assertThat(TypeConverter.isNumericType(type)).as(type.getName()).isFalse();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"1e10000", "1e-10000", "1E+10000", "1e١٠٠٠٠", "1e۱۰۰۰۰", "1e१००००", "1e১০০০০"})
  void acceptsExponentLimitAcrossSupportedDigitSets(String value) {
    assertThatCode(() -> TypeConverter.validateNumericInput(value, BigDecimal.class, "$.n"))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1e10001", "1e-10001", "1e١٠٠٠١", "1e۱۰۰۰۱", "1e१०००१", "1e১০০০১", "1ex"})
  void rejectsOutOfRangeAndInvalidExponents(String value) {
    assertThatThrownBy(() -> TypeConverter.validateNumericInput(value, BigDecimal.class, "$.n"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("$.n");
  }

  @Test
  void countsWhitespaceInNumericLengthLimit() {
    assertThatCode(() -> TypeConverter.validateNumericInput(" ".repeat(1023) + "1", int.class, "$"))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> TypeConverter.validateNumericInput(" ".repeat(1024) + "1", int.class, "$"))
        .isInstanceOf(TypeConversionException.class).hasMessageContaining("maximum length");
  }
}
