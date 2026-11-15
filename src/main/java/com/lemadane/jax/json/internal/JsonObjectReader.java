package com.lemadane.jax.json.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.naming.DataNameCodec;

/**
* Maps JSON objects to mutable Java classes through their property setters.
*/
public final class JsonObjectReader {

  JsonObjectReader() {
  }

  static final ClassValue<ConstructorBindings> CONSTRUCTORS = new ClassValue<>() {
    @Override
    protected ConstructorBindings computeValue(Class<?> type) {
      Constructor<?> defaultConstructor = null;
      List<Constructor<?>> parameterized = new ArrayList<>();
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 0) {
          defaultConstructor = constructor;
        } else {
          parameterized.add(constructor);
        }
      }
      return new ConstructorBindings(defaultConstructor, List.copyOf(parameterized));
    }
  };

  public static <T> T toObject(String json, Class<T> targetType) {
    return toObject(json, targetType, true);
  }

  public static <T> T toObject(String json, Class<T> targetType, boolean strict) {
    JsonParser.JsonNode root = JsonParser.parse(json);
    if (!(root instanceof JsonParser.JsonNode.JsonObjectNode objectNode)) {
      throw new JsonMappingException("A JSON object is expected but " + describe(root) + " was supplied at $");
    }
    if (targetType.isRecord()) {
      throw new RecordConstructionException("Target type is a Java record; use the record JSON mapping path");
    }
    return readObject(targetType, objectNode, "$", strict);
  }

  static <T> T readObject(
      Class<T> targetType,
      JsonParser.JsonNode.JsonObjectNode objectNode,
      String path,
      boolean strict
  ) {
    Map<String, JsonParser.JsonNode> properties = objectNode.members();
    ConstructorResult<T> result = createFromConstructor(targetType, properties, path, strict);
    T instance = result.instance();
    for (var entry : objectNode.members().entrySet()) {
      if (result.consumedProperties().contains(entry.getKey())) {
        continue;
      }
      String propertyName = entry.getKey();
      String propertyPath = path + "." + entry.getKey();
      SetterBinding setter = findSetterBinding(targetType, propertyName, propertyPath);
      if (setter == null) {
        String encodedName = DataNameCodec.encode(propertyName);
        if (!encodedName.equals(propertyName)) {
          setter = findSetterBinding(targetType, encodedName, propertyPath);
        }
      }
      if (setter == null) {
        if (!strict) {
          continue;
        }
        throw new ExcessDataException(
            "No setter for JSON property \"" + entry.getKey() + "\" on " + targetType.getName() + " at " + propertyPath
        );
      }

      Object value = readValue(setter.parameterType(), setter.genericType(), entry.getValue(), propertyPath, strict);
      try {
        setter.method().invoke(instance, value);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        throw new JsonMappingException("Setter for property \"" + entry.getKey() + "\" failed at " + propertyPath, cause);
      } catch (ReflectiveOperationException | IllegalArgumentException e) {
        throw new JsonMappingException("Failed to set property \"" + entry.getKey() + "\" at " + propertyPath, e);
      }
    }
    return instance;
  }

  static <T> ConstructorResult<T> createFromConstructor(
      Class<T> targetType,
      Map<String, JsonParser.JsonNode> properties,
      String path,
      boolean strict
  ) {
    ConstructorBindings bindings = CONSTRUCTORS.get(targetType);
    if (bindings.parameterized().isEmpty()) {
      return new ConstructorResult<>(createInstance(targetType), Set.of());
    }
    List<Constructor<?>> matchingConstructors = new ArrayList<>();
    List<Set<String>> matchingPropertySets = new ArrayList<>();
    boolean missingParameterNames = false;
    for (Constructor<?> constructor : bindings.parameterized()) {
      Set<String> consumedProperties = new LinkedHashSet<>();
      boolean matches = true;
      for (var parameter : constructor.getParameters()) {
        if (!parameter.isNamePresent()) {
          missingParameterNames = true;
          matches = false;
          break;
        }
        String propertyName = findPropertyName(properties, parameter.getName());
        if (propertyName == null) {
          matches = false;
          break;
        }
        consumedProperties.add(propertyName);
      }
      if (matches) {
        matchingConstructors.add(constructor);
        matchingPropertySets.add(consumedProperties);
      }
    }

    if (matchingConstructors.size() > 1) {
      throw new JsonMappingException("Multiple constructors match JSON properties for " + targetType.getName() + " at " + path);
    }
    if (matchingConstructors.size() == 1) {
      Constructor<?> constructor = matchingConstructors.get(0);
      try {
        if (!constructor.trySetAccessible()) {
          throw new JsonMappingException("Cannot access matching constructor for " + targetType.getName() + " at " + path);
        }
        var parameters = constructor.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
          String propertyName = findPropertyName(properties, parameters[i].getName());
          JsonParser.JsonNode valueNode = properties.get(propertyName);
          String propertyPath = path + "." + propertyName;
          arguments[i] = readValue(
              parameters[i].getType(),
              parameters[i].getParameterizedType(),
              valueNode,
              propertyPath,
              strict
          );
        }
        @SuppressWarnings("unchecked")
        T instance = (T) constructor.newInstance(arguments);
        return new ConstructorResult<>(instance, matchingPropertySets.get(0));
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        throw new JsonMappingException("Matching constructor for " + targetType.getName() + " failed at " + path, cause);
      } catch (ReflectiveOperationException | IllegalArgumentException e) {
        throw new JsonMappingException("Failed to invoke matching constructor for " + targetType.getName() + " at " + path, e);
      }
    }

    try {
      return new ConstructorResult<>(createInstance(targetType), Set.of());
    } catch (JsonMappingException e) {
      if (missingParameterNames) {
        throw new JsonMappingException(
            "Could not match a constructor for " + targetType.getName()
                + "; compile the class with the -parameters option to retain constructor parameter names",
            e
        );
      }
      throw e;
    }
  }

  static String findPropertyName(Map<String, JsonParser.JsonNode> properties, String parameterName) {
    if (properties.containsKey(parameterName)) {
      return parameterName;
    }
    String decodedName = DataNameCodec.decode(parameterName);
    if (properties.containsKey(decodedName)) {
      return decodedName;
    }
    String encodedName = DataNameCodec.encode(parameterName);
    if (properties.containsKey(encodedName)) {
      return encodedName;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  static <T> Constructor<T> defaultConstructor(Class<T> type) throws NoSuchMethodException {
    Constructor<?> constructor = CONSTRUCTORS.get(type).defaultConstructor();
    if (constructor != null) {
      return (Constructor<T>) constructor;
    }
    throw new NoSuchMethodException(type.getName());
  }

  static <T> T createInstance(Class<T> targetType) {
    if (targetType.isInterface() || Modifier.isAbstract(targetType.getModifiers())) {
      throw new JsonMappingException("Target type must be a concrete class: " + targetType.getName());
    }
    if (targetType.isRecord()) {
      throw new RecordConstructionException(
          "Target type is a Java record; use Jax.toObject(String, Class) instead: " + targetType.getName()
      );
    }
    try {
      Constructor<T> constructor = defaultConstructor(targetType);
      if (!constructor.trySetAccessible()) {
        throw new JsonMappingException("No accessible no-argument constructor for " + targetType.getName());
      }
      return constructor.newInstance();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      throw new JsonMappingException("No-argument constructor for " + targetType.getName() + " failed", cause);
    } catch (ReflectiveOperationException e) {
      throw new JsonMappingException("Could not create an instance of " + targetType.getName() + "; a no-argument constructor is required", e);
    }
  }

  static final ClassValue<Map<String, List<SetterBinding>>> SETTERS = new ClassValue<>() {
    @Override
    protected Map<String, List<SetterBinding>> computeValue(Class<?> type) {
      Map<String, List<SetterBinding>> methods = new HashMap<>();
      for (Method method : type.getMethods()) {
        if (!Modifier.isStatic(method.getModifiers()) && !method.isBridge() && !method.isSynthetic()
            && method.getParameterCount() == 1 && method.isAnnotationPresent(JsonProperty.class)) {
          methods.computeIfAbsent(method.getName(), key -> new ArrayList<>())
              .add(new SetterBinding(method));
        }
      }
      methods.replaceAll((name, matches) -> List.copyOf(matches));
      return Map.copyOf(methods);
    }
  };

  static Method findSetter(Class<?> targetType, String propertyName, String path) {
    SetterBinding binding = findSetterBinding(targetType, propertyName, path);
    return binding == null ? null : binding.method();
  }

  static SetterBinding findSetterBinding(Class<?> targetType, String propertyName, String path) {
    Map<String, List<SetterBinding>> methods = SETTERS.get(targetType);
    List<SetterBinding> matches = methods.get(propertyName);
    if (matches == null && !propertyName.isEmpty()) {
      matches = methods.get("set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1));
    }
    if (matches == null) {
      return null;
    }
    if (matches.size() > 1) {
      throw new JsonMappingException("Multiple setters match property \"" + propertyName + "\" at " + path);
    }
    return matches.get(0);
  }

  static Object readValue(
      Class<?> targetType,
      Type genericType,
      JsonParser.JsonNode node,
      String path,
      boolean strict
  ) {
    if (node instanceof JsonParser.JsonNode.JsonNullNode) {
      if (Optional.class.isAssignableFrom(targetType)) {
        return Optional.empty();
      }
      if (targetType.isPrimitive()) {
        throw new TypeConversionException("Cannot convert null value to primitive type " + targetType.getName() + " at " + path);
      }
      return null;
    }

    if (Optional.class.isAssignableFrom(targetType)) {
      Class<?> valueType = getTypeArgument(genericType);
      Type valueGenericType = getGenericTypeArgument(genericType);
      return Optional.ofNullable(readValue(valueType, valueGenericType, node, path, strict));
    }

    if (List.class.isAssignableFrom(targetType)) {
      if (!(node instanceof JsonParser.JsonNode.JsonArrayNode arrayNode)) {
        throw new TypeConversionException("Expected a JSON array for " + targetType.getName() + " at " + path);
      }
      Type elementGenericType = getGenericTypeArgument(genericType);
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < arrayNode.elements().size(); i++) {
        values.add(readValue(rawType(elementGenericType), elementGenericType, arrayNode.elements().get(i), path + "[" + i + "]", strict));
      }
      return Collections.unmodifiableList(values);
    }

    if (targetType.isArray()) {
      if (!(node instanceof JsonParser.JsonNode.JsonArrayNode arrayNode)) {
        throw new TypeConversionException("Expected a JSON array for " + targetType.getName() + " at " + path);
      }
      Class<?> elementType = targetType.getComponentType();
      Object array = java.lang.reflect.Array.newInstance(elementType, arrayNode.elements().size());
      for (int i = 0; i < arrayNode.elements().size(); i++) {
        Object value = readValue(
            elementType,
            elementType,
            arrayNode.elements().get(i),
            path + "[" + i + "]",
            strict
        );
        java.lang.reflect.Array.set(array, i, value);
      }
      return array;
    }

    if (targetType == Object.class) {
      return toJavaValue(node, path);
    }

    if (node instanceof JsonParser.JsonNode.JsonObjectNode objectNode) {
      return readObject(targetType, objectNode, path, strict);
    }
    if (node instanceof JsonParser.JsonNode.JsonStringNode stringNode) {
      return TypeConverter.convert(targetType, stringNode.value(), path);
    }
    if (node instanceof JsonParser.JsonNode.JsonNumberNode numberNode) {
      return TypeConverter.convert(targetType, numberNode.bigDecimalValue(), path);
    }
    if (node instanceof JsonParser.JsonNode.JsonBooleanNode booleanNode) {
      return TypeConverter.convert(targetType, booleanNode.value(), path);
    }
    throw new TypeConversionException("Cannot convert " + describe(node) + " to " + targetType.getName() + " at " + path);
  }

  static Class<?> getTypeArgument(Type genericType) {
    return rawType(getGenericTypeArgument(genericType));
  }

  static Type getGenericTypeArgument(Type genericType) {
    if (genericType instanceof ParameterizedType parameterizedType) {
      Type[] arguments = parameterizedType.getActualTypeArguments();
      if (arguments.length > 0) {
        return arguments[0];
      }
    }
    return Object.class;
  }

  static Class<?> rawType(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof java.lang.reflect.GenericArrayType arrayType) {
      return java.lang.reflect.Array.newInstance(rawType(arrayType.getGenericComponentType()), 0).getClass();
    }
    return Object.class;
  }

  static Object toJavaValue(JsonParser.JsonNode node, String path) {
    if (node instanceof JsonParser.JsonNode.JsonNullNode) {
      return null;
    }
    if (node instanceof JsonParser.JsonNode.JsonStringNode stringNode) {
      return stringNode.value();
    }
    if (node instanceof JsonParser.JsonNode.JsonNumberNode numberNode) {
      return numberNode.bigDecimalValue();
    }
    if (node instanceof JsonParser.JsonNode.JsonBooleanNode booleanNode) {
      return booleanNode.value();
    }
    if (node instanceof JsonParser.JsonNode.JsonArrayNode arrayNode) {
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < arrayNode.elements().size(); i++) {
        values.add(toJavaValue(arrayNode.elements().get(i), path + "[" + i + "]"));
      }
      return Collections.unmodifiableList(values);
    }
    throw new TypeConversionException("Cannot map JSON object to Object without a declared target type at " + path);
  }

  static String describe(JsonParser.JsonNode node) {
    if (node instanceof JsonParser.JsonNode.JsonObjectNode) {
      return "JSON object";
    }
    if (node instanceof JsonParser.JsonNode.JsonArrayNode) {
      return "JSON array";
    }
    if (node instanceof JsonParser.JsonNode.JsonStringNode) {
      return "JSON string";
    }
    if (node instanceof JsonParser.JsonNode.JsonNumberNode) {
      return "JSON number";
    }
    if (node instanceof JsonParser.JsonNode.JsonBooleanNode) {
      return "JSON boolean";
    }
    return "JSON null";
  }

  record ConstructorResult<T>(T instance, Set<String> consumedProperties) {}

  record ConstructorBindings(Constructor<?> defaultConstructor, List<Constructor<?>> parameterized) {}

  static final class SetterBinding {
    final Method method;
    final Class<?> parameterType;
    volatile Type genericType;

    SetterBinding(Method method) {
      this.method = method;
      parameterType = method.getParameterTypes()[0];
    }

    Method method() {
      return method;
    }

    Class<?> parameterType() {
      return parameterType;
    }

    Type genericType() {
      Type cached = genericType;
      if (cached == null) {
        // Resolve generic signatures only when selected, preserving failures for unused setters.
        cached = method.getGenericParameterTypes()[0];
        genericType = cached;
      }
      return cached;
    }
  }
}
