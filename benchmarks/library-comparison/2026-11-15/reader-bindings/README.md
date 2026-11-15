# Reader bindings and XML escaping

These runs use the complex organization JSON and supply-chain XML fixtures. Each operation is **String → typed POJO → String → typed POJO**, with the same generated POJOs for JAX, Jackson, and Gson. Two JAX tree-writer methods remain as controls.

The retained implementation caches setter parameter types in both readers and no-argument-only constructor eligibility. XML escaping validates every code point, allocates a buffer only when escaping is necessary, and otherwise returns the original string. The XML child/field index candidate described below was rejected.

The **before** main sources were snapshotted, compiled with Java 17 and `javac -parameters`, and placed first on an isolated classpath with the previously compiled comparison harness and fixture resources. No edited main classes were present. The **after** run uses normal Gradle compilation of the retained sources.

All seven methods use two forks, one thread, five one-second warmup iterations, five one-second measurement iterations, a fixed 512 MiB heap, and the JMH GC profiler. Setup and file loading are outside timing. Environment and fork records identify the JDK, XML provider, fixture hashes, and instrumentation; Dynatrace was active in all 14 baseline forks. Compare confidence intervals and allocations alongside mean times. These are local measurements, not deployment guarantees.

## Evidence and reproduction

`before/` contains results, environment, fork records, seven passing comparison checks, exact commands, hashes, and compilation provenance. `after/` records the retained implementation. Commands and provenance retain the original temporary paths, which need adaptation on another machine. Source hashes identify the measured code more precisely than `gitHead`, because changes were uncommitted.

The copied baseline harness differs from the freshly compiled harness only in 13 private-to-package-access flags across six classes. Method bytecode and every other class byte match; affected instance helpers belong to a final class. `before/harness-equivalence.json` records the audit. Fresh compilation preserves the timed operations, although those six class hashes differ.

To reconstruct the complete historical baseline, apply `before/main.patch` in a disposable checkout and verify `before/source-sha256.json`. Compile its main sources separately and put those classes first, excluding retained main output, when running `com.lemadane.jax.comparison.ComparisonRunner` with the same harness, resources, and dependencies. The runner's sole argument is the results directory.

For the retained implementation, select Java 17 and run `./gradlew clean build compareLibraries`. This validates correctness before measurement and leaves the original JAX-only benchmark task unchanged.

The baseline also contains an unrelated generator defect: a blank keyword entry where `private` belonged. The retained code fixes it and strengthens its regression check. Generation is never called by these timed workloads, which use saved POJOs; historical source reconstruction is for benchmark reproduction.

## Rejected XML index experiment

`child-index-experiment/` preserves the candidate results, validation, comparison checks, source hashes, and XML-reader patch. Indexing XML fields and children increased allocation about **2.1%**, with no clear speed benefit:

| XML round trip | Mean ± error (µs/op) | Allocated (KiB/op) |
|---|---:|---:|
| Before | 1,395.02 ± 183.72 | 721.06 |
| Child/field index candidate | 1,441.90 ± 249.42 | 736.56 |

To recreate only that rejected XML implementation, start from the retained sources in a disposable copy and run:

```sh
git apply --include=src/main/java/com/lemadane/jax/xml/internal/XmlObjectReader.java \
  --include=src/main/java/com/lemadane/jax/xml/internal/XmlObjectWriter.java \
  benchmarks/library-comparison/2026-11-15/reader-bindings/before/main.patch
git apply benchmarks/library-comparison/2026-11-15/reader-bindings/child-index-experiment/candidate-from-baseline.patch
./gradlew comparisonTest compareLibraries
```

This keeps the current JSON reader and CLI and matches `child-index-experiment/source-sha256.json`. Use the comparison tasks: current core tests describe retained behavior and differ from the candidate's original tests.

## Exploratory profile

`before/xml-profile-summary.json` and its command describe a separate brief XML JFR run. Samples highlighted name decoding, repeated binding lookups, parsing, writer buffer growth, and factory discovery. Sample weights do not establish precise per-operation costs. Raw recordings and the large export remain temporary.
