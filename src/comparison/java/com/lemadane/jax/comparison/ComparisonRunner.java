package com.lemadane.jax.comparison;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Separate entry point: never changes or replaces the original JAX benchmark suite. */
public final class ComparisonRunner {
   ComparisonRunner() {
  }

  public static void main(String[] args) throws Exception {
    Path output = Path.of(args[0]);
    Files.createDirectories(output);
    ComparisonWorkload workload = new ComparisonWorkload();
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("startedAt", Instant.now().toString());
    metadata.put("javaVersion", System.getProperty("java.runtime.version"));
    metadata.put("javaVendor", System.getProperty("java.vendor"));
    metadata.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
    metadata.put("architecture", System.getProperty("os.arch"));
    metadata.put("availableProcessors", Runtime.getRuntime().availableProcessors());
    Path cpuInfo = Path.of("/proc/cpuinfo");
    if (Files.isReadable(cpuInfo)) {
      metadata.put("cpu", Files.readAllLines(cpuInfo).stream()
          .filter(line -> line.startsWith("model name")).findFirst().orElse("unknown"));
    }
    metadata.put("gitHead", command("git", "rev-parse", "HEAD"));
    metadata.put("workingTreeDirty", !command("git", "status", "--porcelain").isEmpty());
    metadata.put("jax", "0.2.0-SNAPSHOT");
    metadata.put("jackson", workload.jacksonJson.version().toString());
    metadata.put("gson", "2.13.2");
    metadata.put("jmh", "1.36");
    metadata.put("xmlInputProvider", XMLInputFactory.newFactory().getClass().getName());
    metadata.put("xmlInputProviderVersion", XMLInputFactory.newFactory().getClass().getPackage().getImplementationVersion());
    metadata.put("launcherJvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
    List<String> forkArguments = List.of("-Xms512m", "-Xmx512m", "-Dcomparison.outputDirectory=" + output);
    metadata.put("forkJvmArguments", forkArguments);
    metadata.put("forks", 2);
    metadata.put("threads", 1);
    metadata.put("warmup", "5 iterations x 1 second per fork");
    metadata.put("measurement", "5 iterations x 1 second per fork");
    metadata.put("operation", "String -> typed POJO -> String -> typed POJO; returned object consumed by JMH");
    metadata.put("jsonFixture", fixture(workload.json));
    metadata.put("xmlFixture", fixture(workload.xml));
    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("environment.json").toFile(), metadata);

    var options = new OptionsBuilder()
        .include(LibraryComparisonBenchmark.class.getName() + ".*")
        .warmupIterations(5).warmupTime(TimeValue.seconds(1))
        .measurementIterations(5).measurementTime(TimeValue.seconds(1))
        .forks(2).threads(1)
        .jvmArgs(forkArguments.toArray(String[]::new))
        .shouldFailOnError(true)
        .addProfiler(GCProfiler.class)
        .resultFormat(ResultFormatType.JSON)
        .result(output.resolve("results.json").toString())
        .build();
    new Runner(options).run();
  }

   static String command(String... arguments) throws Exception {
    Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    return process.waitFor() == 0 ? output : "unavailable";
  }

   static Map<String, Object> fixture(String input) throws Exception {
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    return Map.of("utf8Bytes", bytes.length,
        "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
  }
}
