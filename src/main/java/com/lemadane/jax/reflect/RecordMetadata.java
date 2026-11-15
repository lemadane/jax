package com.lemadane.jax.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
* Metadata holding reflection details for a Java record class.
*/
public final class RecordMetadata {

  final Class<?> recordType;
  final String recordSimpleName;
  final String decodedRecordName;
  final Constructor<?> canonicalConstructor;
  final List<ComponentMetadata> components;

  public RecordMetadata(
      Class<?> recordType,
      String recordSimpleName,
      String decodedRecordName,
      Constructor<?> canonicalConstructor,
      List<ComponentMetadata> components
  ) {
    this.recordType = recordType;
    this.recordSimpleName = recordSimpleName;
    this.decodedRecordName = decodedRecordName;
    this.canonicalConstructor = canonicalConstructor;
    this.components = Collections.unmodifiableList(components);
  }

  public Class<?> recordType() {
    return recordType;
  }

  public String recordSimpleName() {
    return recordSimpleName;
  }

  public String decodedRecordName() {
    return decodedRecordName;
  }

  public Constructor<?> canonicalConstructor() {
    return canonicalConstructor;
  }

  public List<ComponentMetadata> components() {
    return components;
  }

  /**
  * Metadata holding details for a single record component.
  */
  public static final class ComponentMetadata {
    final String name;
    final String decodedName;
    final Class<?> type;
    final Type genericType;
    final Method accessor;
    final boolean isList;
    final Class<?> listElementType;
    final boolean isOptional;
    final Class<?> optionalValueType;

    public ComponentMetadata(
        String name,
        String decodedName,
        Class<?> type,
        Type genericType,
        Method accessor
    ) {
      this.name = name;
      this.decodedName = decodedName;
      this.type = type;
      this.genericType = genericType;
      this.accessor = accessor;
      this.isList = List.class.isAssignableFrom(type);
      this.listElementType = isList ? resolveGenericTypeArgument(genericType) : null;
      this.isOptional = Optional.class.isAssignableFrom(type);
      this.optionalValueType = isOptional ? resolveGenericTypeArgument(genericType) : null;
    }

    static Class<?> resolveGenericTypeArgument(Type type) {
      if (type instanceof ParameterizedType pt) {
        Type[] args = pt.getActualTypeArguments();
        if (args.length > 0) {
          if (args[0] instanceof Class<?> clazz) {
            return clazz;
          } else if (args[0] instanceof ParameterizedType nestedPt && nestedPt.getRawType() instanceof Class<?> rawClazz) {
            return rawClazz;
          }
        }
      }
      return Object.class;
    }

    public String name() {
      return name;
    }

    public String decodedName() {
      return decodedName;
    }

    public Class<?> type() {
      return type;
    }

    public Type genericType() {
      return genericType;
    }

    public Method accessor() {
      return accessor;
    }

    public boolean isList() {
      return isList;
    }

    public Class<?> listElementType() {
      return listElementType;
    }

    public Type listElementGenericType() {
      return genericTypeArgument(genericType);
    }

    public Type optionalValueGenericType() {
      return genericTypeArgument(genericType);
    }

    static Type genericTypeArgument(Type type) {
      if (type instanceof ParameterizedType parameterizedType
          && parameterizedType.getActualTypeArguments().length > 0) {
        return parameterizedType.getActualTypeArguments()[0];
      }
      return Object.class;
    }

    public boolean isOptional() {
      return isOptional;
    }

    public Class<?> optionalValueType() {
      return optionalValueType;
    }

  }
}
