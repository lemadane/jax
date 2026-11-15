package com.lemadane.jax.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.lemadane.jax.exceptions.TypeConversionException;

/**
* Utility for converting scalar values to declared Java record component types.
*/
public final class TypeConverter {

  static final int MAX_NUMERIC_INPUT_LENGTH = 1024;
  static final int MAX_ABSOLUTE_EXPONENT = 10000;

  TypeConverter() {
  }

  /**
  * Converts a raw value (String, Number, Boolean, etc.) to the target type.
  *
  * @param targetType the declared target Java type.
  * @param rawValue the raw value to convert.
  * @param pathContext path context description for exception reporting.
  * @return the converted value.
  * @throws TypeConversionException if conversion fails or type is incompatible.
  */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Object convert(Class<?> targetType, Object rawValue, String pathContext) {
    if (rawValue == null) {
      if (targetType.isPrimitive()) {
        throw new TypeConversionException(
            "Cannot convert null value to primitive type " + targetType.getName() + " at " + pathContext
        );
      }
      return null;
    }

    if (isNumericType(targetType)) {
      validateNumericInput(rawValue, targetType, pathContext);
    }

    if (targetType.isInstance(rawValue)) {
      return rawValue;
    }

    String strVal = rawValue.toString().trim();

    if (targetType == String.class) {
      return rawValue.toString();
    }

    if (targetType == boolean.class || targetType == Boolean.class) {
      if (rawValue instanceof Boolean b) {
        return b;
      }
      if ("true".equalsIgnoreCase(strVal)) {
        return Boolean.TRUE;
      } else if ("false".equalsIgnoreCase(strVal)) {
        return Boolean.FALSE;
      }
      throw new TypeConversionException(
          "Cannot convert value \"" + strVal + "\" to boolean at " + pathContext
      );
    }

    if (targetType == byte.class || targetType == Byte.class) {
      try {
        return exactInteger(rawValue).byteValueExact();
      } catch (NumberFormatException | ArithmeticException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to byte at " + pathContext, e
        );
      }
    }

    if (targetType == short.class || targetType == Short.class) {
      try {
        return exactInteger(rawValue).shortValueExact();
      } catch (NumberFormatException | ArithmeticException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to short at " + pathContext, e
        );
      }
    }

    if (targetType == int.class || targetType == Integer.class) {
      try {
        return exactInteger(rawValue).intValueExact();
      } catch (NumberFormatException | ArithmeticException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to int at " + pathContext, e
        );
      }
    }

    if (targetType == long.class || targetType == Long.class) {
      try {
        return exactInteger(rawValue).longValueExact();
      } catch (NumberFormatException | ArithmeticException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to long at " + pathContext, e
        );
      }
    }

    if (targetType == float.class || targetType == Float.class) {
      try {
        float result;
        if (rawValue instanceof Number n) {
          result = n.floatValue();
        } else {
          result = Float.parseFloat(strVal);
        }
        if (!Float.isFinite(result)) {
          throw new NumberFormatException("value is outside the finite float range");
        }
        return result;
      } catch (NumberFormatException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to float at " + pathContext, e
        );
      }
    }

    if (targetType == double.class || targetType == Double.class) {
      try {
        double result;
        if (rawValue instanceof Number n) {
          result = n.doubleValue();
        } else {
          result = Double.parseDouble(strVal);
        }
        if (!Double.isFinite(result)) {
          throw new NumberFormatException("value is outside the finite double range");
        }
        return result;
      } catch (NumberFormatException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to double at " + pathContext, e
        );
      }
    }

    if (targetType == char.class || targetType == Character.class) {
      if (strVal.length() == 1) {
        return strVal.charAt(0);
      }
      throw new TypeConversionException(
          "Cannot convert value \"" + strVal + "\" to char at " + pathContext
      );
    }

    if (targetType == BigInteger.class) {
      try {
        return exactInteger(rawValue).toBigIntegerExact();
      } catch (NumberFormatException | ArithmeticException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to BigInteger at " + pathContext, e
        );
      }

    }

    if (targetType == BigDecimal.class) {
      try {
        return new BigDecimal(strVal);
      } catch (NumberFormatException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to BigDecimal at " + pathContext, e
        );
      }
    }

    if (targetType.isEnum()) {
      try {
        return Enum.valueOf((Class<Enum>) targetType, strVal);
      } catch (IllegalArgumentException e) {
        throw new TypeConversionException(
            "Cannot convert value \"" + strVal + "\" to enum " + targetType.getName() + " at " + pathContext, e
        );
      }
    }

    throw new TypeConversionException(
        "Unsupported target type " + targetType.getName() + " for value \"" + strVal + "\" at " + pathContext
    );
  }

  static BigDecimal exactInteger(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof BigInteger integer) {
      return new BigDecimal(integer);
    }
    return new BigDecimal(value.toString().trim());
  }

  static boolean isNumericType(Class<?> type) {
    return type == byte.class || type == Byte.class
        || type == short.class || type == Short.class
        || type == int.class || type == Integer.class
        || type == long.class || type == Long.class
        || type == float.class || type == Float.class
        || type == double.class || type == Double.class
        || type == BigInteger.class || type == BigDecimal.class;
  }

  static void validateNumericInput(Object value, Class<?> targetType, String path) {
    String numericText = value.toString();
    if (numericText.length() > MAX_NUMERIC_INPUT_LENGTH) {
      throw new TypeConversionException(
          "Numeric input exceeds maximum length of " + MAX_NUMERIC_INPUT_LENGTH
              + " characters for " + targetType.getName() + " at " + path
      );
    }
    numericText = numericText.trim();
    int exponentIndex = Math.max(numericText.indexOf('e'), numericText.indexOf('E'));
    if (exponentIndex < 0) {
      return;
    }

    int index = exponentIndex + 1;
    boolean negative = false;
    if (index < numericText.length() && (numericText.charAt(index) == '+' || numericText.charAt(index) == '-')) {
      negative = numericText.charAt(index) == '-';
      index++;
    }
    int exponent = 0;
    for (; index < numericText.length(); index++) {
      char digit = numericText.charAt(index);
      int digitValue = -1;
      if (digit >= '0' && digit <= '9') {
        digitValue = digit - '0';
      } else if (digit >= '\u0660' && digit <= '\u0669') {
        digitValue = digit - '\u0660';
      } else if (digit >= '\u06F0' && digit <= '\u06F9') {
        digitValue = digit - '\u06F0';
      } else if (digit >= '\u0966' && digit <= '\u096F') {
        digitValue = digit - '\u0966';
      } else if (digit >= '\u09E6' && digit <= '\u09EF') {
        digitValue = digit - '\u09E6';
      } else {
        throw exponentLimitException(targetType, path);
      }

      if (exponent > MAX_ABSOLUTE_EXPONENT / 10
          || exponent == MAX_ABSOLUTE_EXPONENT / 10 && digitValue > MAX_ABSOLUTE_EXPONENT % 10) {
        throw exponentLimitException(targetType, path);
      }
      exponent = exponent * 10 + digitValue;
    }
    if (negative) {
      exponent = -exponent;
    }
    if (Math.abs(exponent) > MAX_ABSOLUTE_EXPONENT) {
      throw exponentLimitException(targetType, path);
    }
  }

  static TypeConversionException exponentLimitException(Class<?> targetType, String path) {
    return new TypeConversionException(
        "Numeric exponent exceeds maximum absolute value of " + MAX_ABSOLUTE_EXPONENT
            + " for " + targetType.getName() + " at " + path
    );
  }
}
