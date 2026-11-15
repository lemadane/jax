package com.lemadane.jax.xml.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.exceptions.XmlMappingException;

final class XmlTextSupport {

  XmlTextSupport() {
  }

  static Field field(Class<?> type) {
    Field result = null;
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (!field.isAnnotationPresent(XmlText.class) || Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
          continue;
        }
        validate(field, field.getType());
        if (result != null) {
          throw new XmlMappingException("Only one @XmlText field is allowed on " + type.getName());
        }
        result = field;
      }
    }
    return result;
  }

  static void validate(AnnotatedElement member, Class<?> type) {
    if (member.isAnnotationPresent(XmlAttribute.class)) {
      throw new XmlMappingException("A member cannot have both @XmlText and @XmlAttribute");
    }
    validateType(type);
  }

  static void validateType(Class<?> type) {
    if (type.isArray() || Collection.class.isAssignableFrom(type) || java.util.Map.class.isAssignableFrom(type) || type.isRecord()
        || XmlObjectReader.isPojo(type)) {
      throw new XmlMappingException("@XmlText requires a scalar value: " + type.getName());
    }
  }

  static void append(Object object, XmlElement element) {
    Field field = field(object.getClass());
    if (field == null) {
      return;
    }
    if (!element.children().isEmpty()) {
      throw new XmlMappingException("@XmlText cannot be combined with child elements on " + object.getClass().getName());
    }
    try {
      if (!field.canAccess(object) && !field.trySetAccessible()) {
        throw new XmlMappingException("Cannot access @XmlText field " + field.getName());
      }
      Object value = field.get(object);
      if (value instanceof Optional<?> optional) {
        value = optional.orElse(null);
      }
      if (value != null) {
        validate(field, value.getClass());
        element.addText(value instanceof Enum<?> enumValue ? enumValue.name() : value.toString());
      }
    } catch (IllegalAccessException e) {
      throw new XmlMappingException("Cannot read @XmlText field " + field.getName(), e);
    }
  }
}
