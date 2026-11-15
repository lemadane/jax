package com.lemadane.jax.comparison;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class LibraryComparisonBenchmark {
   ComparisonWorkload workload;

  @Setup
  public void setup() throws Exception {
    workload = new ComparisonWorkload();
    String directory = System.getProperty("comparison.outputDirectory");
    if (directory != null) {
      var details = new java.util.LinkedHashMap<String, Object>();
      details.put("pid", ProcessHandle.current().pid());
      details.put("javaVersion", System.getProperty("java.runtime.version"));
      details.put("actualJvmArguments", java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
      details.put("xmlInputProvider", javax.xml.stream.XMLInputFactory.newFactory().getClass().getName());
      workload.jacksonJson.writerWithDefaultPrettyPrinter().writeValue(
          java.nio.file.Path.of(directory, "fork-" + ProcessHandle.current().pid() + ".json").toFile(), details);
    }
  }

  @Benchmark
  public Object jaxJsonRoundTrip() {
    return workload.jaxJsonRoundTrip();
  }

  @Benchmark
  public Object jaxJsonTreeWriterRoundTrip() {
    return workload.jaxJsonTreeWriterRoundTrip();
  }

  @Benchmark
  public Object jaxXmlTreeWriterRoundTrip() {
    return workload.jaxXmlTreeWriterRoundTrip();
  }

  @Benchmark
  public Object jacksonJsonRoundTrip() throws Exception {
    return workload.jacksonJsonRoundTrip();
  }

  @Benchmark
  public Object gsonJsonRoundTrip() {
    return workload.gsonJsonRoundTrip();
  }

  @Benchmark
  public Object jaxXmlRoundTrip() {
    return workload.jaxXmlRoundTrip();
  }

  @Benchmark
  public Object jacksonXmlRoundTrip() throws Exception {
    return workload.jacksonXmlRoundTrip();
  }
}
