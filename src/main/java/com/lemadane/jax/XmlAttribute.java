package com.lemadane.jax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* Marks a POJO field or constructor parameter as an XML attribute.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface XmlAttribute {

  /**
  * The XML attribute name. When empty, the annotated member name is decoded with {@code DataNameCodec}.
  *
  * @return the external XML attribute name.
  */
  String value() default "";
}
