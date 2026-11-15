package com.lemadane.jax.json.internal;

import java.util.Collections;
import java.util.IdentityHashMap;

/** Comparison-only bridge to the original tree serialization pipeline. */
public final class JsonTreeWriterBaseline {
   JsonTreeWriterBaseline() {
  }

  public static String stringify(Object value) {
    var visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    var tree = JsonRecordWriter.toNode(value, visited);
    StringBuilder output = new StringBuilder();
    JsonRecordWriter.writeNode(tree, output);
    return output.toString();
  }
}
