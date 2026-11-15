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

   static final ClassValue<List<Accessor>> ACCESSORS = new ClassValue<>() {
    @Override
    protected List<Accessor> computeValue(Class<?> type) {
      List<Accessor> accessors = new ArrayList<>();
      for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
        for (Field field : current.getDeclaredFields()) {
          if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
            accessors.add(new Accessor(field, findGetter(type, field.getName())));
          }
        }
      }
      return List.copyOf(accessors);
    }
  };

  public static List<Property> read(Object object) throws IllegalAccessException, InvocationTargetException {
    List<Accessor> accessors = ACCESSORS.get(object.getClass());
    List<Property> properties = new ArrayList<>(accessors.size());
    for (Accessor accessor : accessors) {
      Field field = accessor.field();
      Object value;
      if (Modifier.isPublic(field.getModifiers()) && field.canAccess(object)) {
        value = field.get(object);
      } else if (accessor.getter() != null) {
        value = accessor.getter().invoke(object);
      } else {
        continue;
      }
      properties.add(new Property(field.getName(), value));
    }
    return properties;
  }

   record Accessor(Field field, Method getter) {}

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
