package com.lemadane.jax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* Maps a scalar POJO field or constructor parameter to an element's text content.
* An element may also have attributes, but cannot have child elements.
* Annotate the backing field as well as the constructor parameter for round trips.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface XmlText {
}
