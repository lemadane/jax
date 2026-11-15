package com.lemadane.jax.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
* Reads POJO fields directly or through public no-argument accessors.
*/
public final class PojoPropertyReader {

  PojoPropertyReader() {
  }

  public static List<Property> read(Object object) throws IllegalAccessException, InvocationTargetException {
    List<Property> properties = new ArrayList<>();
    for (Class<?> current = object.getClass(); current != null && current != Object.class;
        current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
          continue;
        }
        Object value;
        if (Modifier.isPublic(field.getModifiers()) && field.canAccess(object)) {
          value = field.get(object);
        } else {
          Method getter = findGetter(object.getClass(), field.getName());
          if (getter == null) {
            continue;
          }
          value = getter.invoke(object);
        }
        properties.add(new Property(field.getName(), value));
      }
    }
    return properties;
  }

  static Method findGetter(Class<?> type, String propertyName) {
    String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    for (String methodName : List.of(propertyName, "get" + capitalized, "is" + capitalized)) {
      try {
        Method method = type.getMethod(methodName);
        if (!Modifier.isStatic(method.getModifiers()) && method.getReturnType() != void.class
            && !method.isBridge() && !method.isSynthetic()) {
          return method;
        }
      } catch (NoSuchMethodException ignored) {
        continue;
      }
    }
    return null;
  }

  public record Property(String name, Object value) {
  }
}
