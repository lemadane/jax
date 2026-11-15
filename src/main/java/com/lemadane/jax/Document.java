package com.lemadane.jax;

import java.util.LinkedHashMap;
import java.util.Map;

/**
* Insertion-ordered JSON object whose nested objects are also Documents.
*
* <p>Scalar values are represented by {@link String}, {@link Boolean}, {@link Integer},
* {@link Long}, {@link java.math.BigDecimal}, or {@code null}. JSON arrays are represented
* by lists of these values.</p>
*/
public final class Document extends LinkedHashMap<String, Object> {

  static final long serialVersionUID = 1L;

  public Document() {
  }

  public Document(Map<String, ?> values) {
    super(values);
  }
}
