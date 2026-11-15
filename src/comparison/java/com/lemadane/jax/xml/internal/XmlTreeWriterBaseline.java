package com.lemadane.jax.xml.internal;

import java.util.Collections;
import java.util.IdentityHashMap;

/** Comparison-only bridge to the original tree serialization pipeline. */
public final class XmlTreeWriterBaseline {
   XmlTreeWriterBaseline() {
  }

  public static String stringify(Object value) {
    var visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    var tree = XmlObjectWriter.toElement(value, XmlObjectWriter.rootName(value), visited);
    StringBuilder output = new StringBuilder();
    XmlObjectWriter.writeElement(tree, output);
    return output.toString();
  }
}
