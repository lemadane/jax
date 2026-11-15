package com.lemadane.jax.convert;

import java.util.List;
import java.util.Optional;

import com.lemadane.jax.reflect.RecordMetadata;

/**
* Provides default values for absent record components during partial parsing.
*/
public final class DefaultValueProvider {

  DefaultValueProvider() {
  }

  /**
  * Computes the default value for a component in partial parsing mode.
  *
  * @param component the component metadata.
  * @return the default value for absent component.
  */
  public static Object getDefaultValue(RecordMetadata.ComponentMetadata component) {
    Class<?> type = component.type();

    if (component.isOptional()) {
      return Optional.empty();
    }

    if (component.isList()) {
      return List.of();
    }

    if (type.isPrimitive()) {
      if (type == boolean.class) {
        return false;
      } else if (type == byte.class) {
        return (byte) 0;
      } else if (type == short.class) {
        return (short) 0;
      } else if (type == int.class) {
        return 0;
      } else if (type == long.class) {
        return 0L;
      } else if (type == float.class) {
        return 0.0f;
      } else if (type == double.class) {
        return 0.0d;
      } else if (type == char.class) {
        return '\0';
      }
    }

    return null;
  }
}
