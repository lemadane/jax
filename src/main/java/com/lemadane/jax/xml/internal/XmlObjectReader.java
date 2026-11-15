package com.lemadane.jax.xml.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;
import com.lemadane.jax.JsonProperty;
import com.lemadane.jax.convert.TypeConverter;
import com.lemadane.jax.exceptions.ExcessDataException;
import com.lemadane.jax.exceptions.RecordConstructionException;
import com.lemadane.jax.exceptions.TypeConversionException;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.naming.DataNameCodec;
import com.lemadane.jax.reflect.RecordIntrospector;

/**
* Maps XML elements to mutable Java classes through their public instance fields or setters.
*/
public final class XmlObjectReader {

  static final String EMPTY_LIST_ATTRIBUTE = "jax-empty-list";
  static final String NULL_VALUE_ATTRIBUTE = "jax-null";

  XmlObjectReader() {
  }

   static final ClassValue<List<Constructor<?>>> CONSTRUCTORS = new ClassValue<>() {
    @Override
    protected List<Constructor<?>> computeValue(Class<?> type) {
      return List.of(type.getDeclaredConstructors());
    }
  };

  static final ClassValue<Boolean> DEFAULT_ONLY = new ClassValue<>() {
    @Override
    protected Boolean computeValue(Class<?> type) {
      return CONSTRUCTORS.get(type).stream().allMatch(constructor -> constructor.getParameterCount() == 0);
    }
  };

  public static <T> T toObject(String xml, Class<T> targetType) {
    return toObject(xml, targetType, true);
  }

  public static <T> T toObject(String xml, Class<T> targetType, boolean strict) {
    if (targetType.isRecord()) {
      throw new RecordConstructionException("Target type is a Java record; use the record XML mapping path");
    }
    if (targetType.isInterface() || Modifier.isAbstract(targetType.getModifiers())) {
      throw new XmlMappingException("Target type must be a concrete class: " + targetType.getName());
    }

    XmlElement root = XmlRecordReader.parseXmlDocument(xml);
    String rootName = DataNameCodec.decode(root.name());
    String typeName = DataNameCodec.decode(targetType.getSimpleName());
    if (!rootName.equals(typeName) && !root.name().equals(targetType.getSimpleName())) {
      throw new XmlMappingException(
          "Root XML element <" + root.name() + "> does not match requested type " + targetType.getName()
      );
    }
    return readObject(root, targetType, "/" + root.name(), strict);
  }

  static <T> T readObject(XmlElement element, Class<T> targetType, String path, boolean strict) {
    List<XmlElement> childElements = childElements(element);
    ConstructorResult<T> result = createFromConstructor(targetType, element, childElements, path, strict);
    T instance = result.instance();
    List<Field> fields = publicInstanceFields(targetType);

    Field textField = XmlTextSupport.field(targetType);
    if (textField != null && !result.consumedProperties().contains("#text")) {
      Object value = readTextValue(textField.getType(), textField.getGenericType(), element, path);
      setField(textField, instance, value, targetType, path + "/text()");
    }

    for (Field field : attributeFields(targetType)) {
      String attributeName = xmlAttributeName(field);
      if (result.consumedAttributes().contains(attributeName) || !element.hasAttribute(attributeName)) {
        continue;
      }
      String attributePath = path + "/@" + attributeName;
      Object value = readAttributeValue(field.getType(), field.getGenericType(), element.attribute(attributeName), attributePath);
      setField(field, instance, value, targetType, attributePath);
    }

    for (XmlElement child : childElements) {
      String childName = DataNameCodec.decode(child.name());
      if (strict && findField(fields, childName) == null && findSetter(targetType, childName, path) == null
          && !result.consumedProperties().contains(childName)) {
        throw new ExcessDataException(
            "Excess XML element <" + child.name() + "> at " + path
                + ". No matching public field or setter exists in " + targetType.getName()
        );
      }
    }

    for (Field field : fields) {
      String fieldName = DataNameCodec.decode(field.getName());
      if (field.isAnnotationPresent(XmlAttribute.class)) {
        continue;
      }
      if (result.consumedProperties().contains(fieldName)) {
        continue;
      }
      List<XmlElement> matches = matchingElements(childElements, fieldName);
      if (matches.isEmpty()) {
        continue;
      }
      String fieldPath = path + "/" + fieldName;
      Object value;
      if (field.getType().isArray()) {
        value = readArray(field.getType().getComponentType(), matches, fieldPath, strict);
      } else if (List.class.isAssignableFrom(field.getType())) {
        value = readList(field.getGenericType(), matches, fieldPath, strict);
      } else {
        if (matches.size() > 1) {
          throw new XmlMappingException(
              "Multiple XML elements <" + fieldName + "> map to non-collection field "
                  + targetType.getName() + "." + field.getName()
          );
        }
        value = readValue(field.getType(), field.getGenericType(), matches.get(0), fieldPath, strict);
      }
      try {
        if (!field.canAccess(instance) && !field.trySetAccessible()) {
          throw new XmlMappingException("Cannot access public field " + field.getName() + " on " + targetType.getName());
        }
        field.set(instance, value);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new XmlMappingException(
            "Failed to set public field " + field.getName() + " on " + targetType.getName() + " at " + fieldPath,
            e
        );
      }
    }
    Set<String> appliedSetters = new HashSet<>();
    for (XmlElement child : childElements) {
      String propertyName = DataNameCodec.decode(child.name());
      if (result.consumedProperties().contains(propertyName) || findField(fields, propertyName) != null
          || !appliedSetters.add(propertyName)) {
        continue;
      }
      SetterBinding binding = findSetterBinding(targetType, propertyName, path);
      if (binding == null) {
        continue;
      }
      Method setter = binding.method();
      List<XmlElement> matches = matchingElements(childElements, propertyName);
      if (matches.isEmpty()) {
        continue;
      }
      Class<?> valueType = binding.parameterType();
      Type genericType = binding.genericType();
      String propertyPath = path + "/" + propertyName;
      Object value;
      if (valueType.isArray()) {
        value = readArray(valueType.getComponentType(), matches, propertyPath, strict);
      } else if (List.class.isAssignableFrom(valueType)) {
        value = readList(genericType, matches, propertyPath, strict);
      } else {
        if (matches.size() > 1) {
          throw new XmlMappingException(
              "Multiple XML elements <" + propertyName + "> map to non-collection setter "
                  + targetType.getName() + "." + setter.getName()
          );
        }
        value = readValue(valueType, genericType, matches.get(0), propertyPath, strict);
      }
      try {
        setter.invoke(instance, value);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
        throw new XmlMappingException(
            "Failed to invoke setter " + setter.getName() + " on " + targetType.getName()
                + " at " + propertyPath,
            e
        );
      }
    }
    return instance;
  }

  static <T> ConstructorResult<T> createFromConstructor(
      Class<T> targetType,
      XmlElement element,
      List<XmlElement> elements,
      String path,
      boolean strict
  ) {
    if (DEFAULT_ONLY.get(targetType)) {
      return new ConstructorResult<>(createInstance(targetType, path), Set.of(), Set.of());
    }
    List<Constructor<?>> matchingConstructors = new ArrayList<>();
    List<Set<String>> matchingPropertySets = new ArrayList<>();
    boolean missingParameterNames = false;
    for (Constructor<?> constructor : CONSTRUCTORS.get(targetType)) {
      if (constructor.getParameterCount() == 0) {
        continue;
      }
      Set<String> consumedProperties = new LinkedHashSet<>();
      boolean matches = true;
      for (var parameter : constructor.getParameters()) {
        if (parameter.isAnnotationPresent(XmlText.class)) {
          XmlTextSupport.validate(parameter, parameter.getType());
          if (!consumedProperties.add("#text")) {
            throw new XmlMappingException("Only one @XmlText constructor parameter is allowed at " + path);
          }
          continue;
        }
        boolean attribute = parameter.isAnnotationPresent(XmlAttribute.class);
        XmlAttribute attributeAnnotation = parameter.getAnnotation(XmlAttribute.class);
        if (!parameter.isNamePresent() && (!attribute || attributeAnnotation.value().isEmpty())) {
          missingParameterNames = true;
          matches = false;
          break;
        }
        String propertyName = attribute ? xmlAttributeName(parameter) : DataNameCodec.decode(parameter.getName());
        if (attribute) {
          if (!element.hasAttribute(propertyName)) {
            matches = false;
            break;
          }
          continue;
        }
        List<XmlElement> matchesForParameter = matchingElements(elements, propertyName);
        boolean collectionParameter = List.class.isAssignableFrom(parameter.getType()) || parameter.getType().isArray();
        if (matchesForParameter.isEmpty() || (!collectionParameter && matchesForParameter.size() != 1)) {
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
      throw new XmlMappingException("Multiple constructors match XML elements for " + targetType.getName() + " at " + path);
    }
    if (matchingConstructors.size() == 1) {
      Constructor<?> constructor = matchingConstructors.get(0);
      try {
        if (!constructor.trySetAccessible()) {
          throw new XmlMappingException("Cannot access matching constructor for " + targetType.getName() + " at " + path);
        }
        var parameters = constructor.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
          if (parameters[i].isAnnotationPresent(XmlText.class)) {
            arguments[i] = readTextValue(parameters[i].getType(), parameters[i].getParameterizedType(), element, path);
            continue;
          }
          boolean attribute = parameters[i].isAnnotationPresent(XmlAttribute.class);
          String propertyName = attribute ? xmlAttributeName(parameters[i]) : DataNameCodec.decode(parameters[i].getName());
          if (attribute) {
            arguments[i] = readAttributeValue(
                parameters[i].getType(),
                parameters[i].getParameterizedType(),
                element.attribute(propertyName),
                path + "/@" + propertyName
            );
            continue;
          }
          List<XmlElement> matches = matchingElements(elements, propertyName);
          String propertyPath = path + "/" + propertyName;
          if (List.class.isAssignableFrom(parameters[i].getType())) {
            arguments[i] = readList(parameters[i].getParameterizedType(), matches, propertyPath, strict);
          } else if (parameters[i].getType().isArray()) {
            arguments[i] = readArray(parameters[i].getType().getComponentType(), matches, propertyPath, strict);
          } else {
            arguments[i] = readValue(
                parameters[i].getType(),
                parameters[i].getParameterizedType(),
                matches.get(0),
                propertyPath,
                strict
            );
          }
        }
        @SuppressWarnings("unchecked")
        T instance = (T) constructor.newInstance(arguments);
        Set<String> consumedAttributes = new LinkedHashSet<>();
        for (var parameter : parameters) {
          if (parameter.isAnnotationPresent(XmlAttribute.class)) {
            consumedAttributes.add(xmlAttributeName(parameter));
          }
        }
        return new ConstructorResult<>(instance, matchingPropertySets.get(0), consumedAttributes);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        throw new XmlMappingException("Matching constructor for " + targetType.getName() + " failed at " + path, cause);
      } catch (ReflectiveOperationException | IllegalArgumentException e) {
        throw new XmlMappingException("Failed to invoke matching constructor for " + targetType.getName() + " at " + path, e);
      }
    }

    try {
      return new ConstructorResult<>(createInstance(targetType, path), Set.of(), Set.of());
    } catch (XmlMappingException e) {
      if (missingParameterNames) {
        throw new XmlMappingException(
            "Could not match a constructor for " + targetType.getName()
                + "; compile the class with the -parameters option to retain constructor parameter names",
            e
        );
      }
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
   static <T> Constructor<T> defaultConstructor(Class<T> type) throws NoSuchMethodException {
    for (Constructor<?> constructor : CONSTRUCTORS.get(type)) {
      if (constructor.getParameterCount() == 0) {
        return (Constructor<T>) constructor;
      }
    }
    throw new NoSuchMethodException(type.getName());
  }

  static <T> T createInstance(Class<T> targetType, String path) {
    try {
      var constructor = defaultConstructor(targetType);
      if (!constructor.trySetAccessible()) {
        throw new XmlMappingException("No accessible no-argument constructor for " + targetType.getName() + " at " + path);
      }
      return constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new XmlMappingException(
          "Could not create an instance of " + targetType.getName() + "; a no-argument constructor is required at " + path,
          e
      );
    }
  }

   static final ClassValue<List<Field>> PUBLIC_FIELDS = new ClassValue<>() {
    @Override
    protected List<Field> computeValue(Class<?> type) {
      return inspectPublicFields(type);
    }
  };

  static List<Field> publicInstanceFields(Class<?> type) {
    return PUBLIC_FIELDS.get(type);
  }

   static List<Field> inspectPublicFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Field field : type.getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()
          && !field.isAnnotationPresent(XmlAttribute.class) && !field.isAnnotationPresent(XmlText.class)) {
        fields.add(field);
      }
    }
    return List.copyOf(fields);
  }

   static final ClassValue<List<Field>> ATTRIBUTE_FIELDS = new ClassValue<>() {
    @Override
    protected List<Field> computeValue(Class<?> type) {
      return inspectAttributeFields(type);
    }
  };

  static List<Field> attributeFields(Class<?> type) {
    return ATTRIBUTE_FIELDS.get(type);
  }

   static List<Field> inspectAttributeFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (field.isAnnotationPresent(XmlAttribute.class)
            && !Modifier.isStatic(field.getModifiers())
            && !field.isSynthetic()) {
          fields.add(field);
        }
      }
    }
    return List.copyOf(fields);
  }

  static String xmlAttributeName(java.lang.reflect.AnnotatedElement member) {
    XmlAttribute annotation = member.getAnnotation(XmlAttribute.class);
    if (annotation.value().isEmpty()) {
      if (member instanceof Field field) {
        return DataNameCodec.decode(field.getName());
      }
      if (member instanceof java.lang.reflect.Parameter parameter) {
        return DataNameCodec.decode(parameter.getName());
      }
      throw new XmlMappingException("@XmlAttribute is only supported on fields and constructor parameters");
    }
    return annotation.value();
  }

  static Object readTextValue(Class<?> targetType, Type genericType, XmlElement element, String path) {
    if (!element.children().isEmpty()) {
      throw new XmlMappingException("@XmlText cannot be combined with child elements at " + path);
    }
    XmlTextSupport.validateType(Optional.class.isAssignableFrom(targetType) ? typeArgument(genericType) : targetType);
    return readValue(targetType, genericType, element, path + "/text()", true);
  }

  static Object readAttributeValue(Class<?> targetType, Type genericType, String value, String path) {
    if (Optional.class.isAssignableFrom(targetType)) {
      return Optional.ofNullable(TypeConverter.convert(typeArgument(genericType), value, path));
    }
    return TypeConverter.convert(targetType, value, path);
  }

  static void setField(Field field, Object instance, Object value, Class<?> targetType, String path) {
    try {
      if (!field.canAccess(instance) && !field.trySetAccessible()) {
        throw new XmlMappingException("Cannot access annotated XML attribute field " + field.getName()
            + " on " + targetType.getName());
      }
      field.set(instance, value);
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new XmlMappingException(
          "Failed to set XML attribute field " + field.getName() + " on " + targetType.getName() + " at " + path,
          e
      );
    }
  }

  static Field findField(List<Field> fields, String decodedName) {
    for (Field field : fields) {
      if (DataNameCodec.decode(field.getName()).equals(decodedName) || field.getName().equals(decodedName)) {
        return field;
      }
    }
    return null;
  }

  static final ClassValue<Map<String, List<SetterBinding>>> SETTERS = new ClassValue<>() {
    @Override
    protected Map<String, List<SetterBinding>> computeValue(Class<?> type) {
      Map<String, List<SetterBinding>> methods = new HashMap<>();
      for (Method method : type.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()
            || method.getParameterCount() != 1 || !method.isAnnotationPresent(JsonProperty.class)) {
          continue;
        }
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
          name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        SetterBinding binding = new SetterBinding(method);
        methods.computeIfAbsent(name, key -> new ArrayList<>()).add(binding);
        String decoded = DataNameCodec.decode(name);
        if (!decoded.equals(name)) {
          methods.computeIfAbsent(decoded, key -> new ArrayList<>()).add(binding);
        }
      }
      methods.replaceAll((name, matches) -> List.copyOf(matches));
      return Map.copyOf(methods);
    }
  };

  static Method findSetter(Class<?> type, String decodedName, String path) {
    SetterBinding binding = findSetterBinding(type, decodedName, path);
    return binding == null ? null : binding.method();
  }

  static SetterBinding findSetterBinding(Class<?> type, String decodedName, String path) {
    List<SetterBinding> matches = SETTERS.get(type).get(decodedName);
    if (matches == null) {
      return null;
    }
    if (matches.size() > 1) {
      throw new XmlMappingException(
          "Multiple one-argument setters match XML property " + decodedName + " on " + type.getName() + " at " + path
      );
    }
    return matches.get(0);
  }

  static List<XmlElement> childElements(XmlElement element) {
    return element.children();
  }

  static List<XmlElement> matchingElements(List<XmlElement> elements, String decodedName) {
    List<XmlElement> matches = new ArrayList<>();
    for (XmlElement element : elements) {
      String actualName = DataNameCodec.decode(element.name());
      if (actualName.equals(decodedName) || element.name().equals(decodedName)) {
        matches.add(element);
      }
    }
    return matches;
  }

  static Object readList(Type genericType, List<XmlElement> elements, String path, boolean strict) {
    for (XmlElement element : elements) {
      validateMarkerAttributes(element, path);
    }
    if (elements.size() == 1 && isEmptyCollectionMarker(elements.get(0))) {
      return List.of();
    }
    Type elementGenericType = genericTypeArgument(genericType);
    Class<?> elementType = rawType(elementGenericType);
    List<Object> values = new ArrayList<>(elements.size());
    for (int i = 0; i < elements.size(); i++) {
      values.add(readValue(elementType, elementGenericType, elements.get(i), path + "[" + i + "]", strict));
    }
    return Collections.unmodifiableList(values);
  }

  static Object readArray(Class<?> elementType, List<XmlElement> elements, String path, boolean strict) {
    for (XmlElement element : elements) {
      validateMarkerAttributes(element, path);
    }
    if (elements.size() == 1 && isEmptyCollectionMarker(elements.get(0))) {
      return Array.newInstance(elementType, 0);
    }
    Object array = Array.newInstance(elementType, elements.size());
    for (int i = 0; i < elements.size(); i++) {
      Array.set(array, i, readValue(elementType, elementType, elements.get(i), path + "[" + i + "]", strict));
    }
    return array;
  }

  static Object readValue(Class<?> targetType, Type genericType, XmlElement element, String path, boolean strict) {
    validateMarkerAttributes(element, path);
    
    // Handle Optional first so that NULL_VALUE_ATTRIBUTE is properly wrapped in Optional.empty()
    if (Optional.class.isAssignableFrom(targetType)) {
      Type valueGenericType = genericTypeArgument(genericType);
      Class<?> valueType = rawType(valueGenericType);
      if (element.hasAttribute(NULL_VALUE_ATTRIBUTE)
          && "true".equals(element.attribute(NULL_VALUE_ATTRIBUTE))) {
        return Optional.empty();
      }
      if (isEmptyCollectionMarker(element)) {
        return Optional.of(readValue(valueType, valueGenericType, element, path, strict));
      }
      if (isEmptyElement(element)) {
        if (valueType == String.class) {
          return Optional.of("");
        }
        if (List.class.isAssignableFrom(valueType) || valueType.isArray()) {
          return Optional.empty();
        }
        if (isPojo(valueType)) {
          return Optional.empty();
        }
        return Optional.empty();
      }
      return Optional.ofNullable(readValue(valueType, valueGenericType, element, path, strict));
    }
    
    if (element.hasAttribute(NULL_VALUE_ATTRIBUTE)
        && "true".equals(element.attribute(NULL_VALUE_ATTRIBUTE))) {
      if (targetType.isPrimitive()) {
        throw new TypeConversionException("Cannot map null XML collection item to primitive type "
            + targetType.getName() + " at " + path);
      }
      return null;
    }
    if (isEmptyCollectionMarker(element) && !List.class.isAssignableFrom(targetType) && !targetType.isArray()) {
      throw new TypeConversionException("Empty-list marker does not match " + targetType.getName() + " at " + path);
    }
    if (targetType.isRecord()) {
      return XmlRecordReader.readRecord(
          RecordIntrospector.inspect(targetType),
          element,
          path,
          strict
      );
    }
    if (List.class.isAssignableFrom(targetType)) {
      List<XmlElement> children = childElements(element);
      return readList(genericType, children, path, strict);
    }
    if (targetType.isArray()) {
      return readArray(targetType.getComponentType(), childElements(element), path, strict);
    }
    if (isPojo(targetType)) {
      return readObject(element, targetType, path, strict);
    }

    String text = element.textContent();
    if (text == null || text.isEmpty()) {
      if (targetType == String.class) {
        return "";
      }
      if (targetType.isPrimitive()) {
        throw new TypeConversionException(
            "Empty XML element <" + element.name() + "> cannot be mapped to primitive type "
                + targetType.getName() + " at " + path
        );
      }
      return null;
    }
    return TypeConverter.convert(targetType, text, path);
  }

  static boolean isPojo(Class<?> type) {
    return !type.isPrimitive()
        && !type.isEnum()
        && !type.getPackageName().startsWith("java.")
        && !type.isArray()
        && !List.class.isAssignableFrom(type)
        && !Optional.class.isAssignableFrom(type);
  }

  static boolean isEmptyElement(XmlElement element) {
    return element.isEmpty();
  }

  static boolean isEmptyCollectionMarker(XmlElement element) {
    return "true".equals(element.attribute(EMPTY_LIST_ATTRIBUTE));
  }

  static void validateMarkerAttributes(XmlElement element, String path) {
    boolean emptyList = element.hasAttribute(EMPTY_LIST_ATTRIBUTE);
    boolean nullValue = element.hasAttribute(NULL_VALUE_ATTRIBUTE);
    if (!emptyList && !nullValue) {
      return;
    }
    String marker = emptyList ? EMPTY_LIST_ATTRIBUTE : NULL_VALUE_ATTRIBUTE;
    if (emptyList && nullValue) {
      throw new XmlMappingException("Conflicting XML collection markers at " + path);
    }
    if (!"true".equals(element.attribute(marker))) {
      throw new XmlMappingException("Invalid XML collection marker " + marker + " at " + path);
    }
    if (!element.content().isEmpty()) {
      throw new XmlMappingException("XML collection marker " + marker + " cannot have content at " + path);
    }
  }

  static Type genericTypeArgument(Type genericType) {
    if (genericType instanceof ParameterizedType parameterizedType
        && parameterizedType.getActualTypeArguments().length > 0) {
      return parameterizedType.getActualTypeArguments()[0];
    }
    return Object.class;
  }

  static Class<?> rawType(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterizedType
        && parameterizedType.getRawType() instanceof Class<?> rawClass) {
      return rawClass;
    }
    if (type instanceof java.lang.reflect.GenericArrayType arrayType) {
      return Array.newInstance(rawType(arrayType.getGenericComponentType()), 0).getClass();
    }
    return Object.class;
  }

  static Class<?> typeArgument(Type genericType) {
    if (genericType instanceof ParameterizedType parameterizedType) {
      Type[] arguments = parameterizedType.getActualTypeArguments();
      if (arguments.length > 0) {
        Type argument = arguments[0];
        if (argument instanceof Class<?> argumentClass) {
          return argumentClass;
        }
        if (argument instanceof ParameterizedType nestedType
            && nestedType.getRawType() instanceof Class<?> rawClass) {
          return rawClass;
        }
      }
    }
    return Object.class;
  }

  record ConstructorResult<T>(T instance, Set<String> consumedProperties, Set<String> consumedAttributes) {}

  static final class SetterBinding {
    private final Method method;
    private final Class<?> parameterType;
    private volatile Type genericType;

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
