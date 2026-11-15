# Rejected direct JSON reader experiment

This candidate was tested but **is not part of the retained implementation**. The final source tree retains the direct JSON/XML writers and the existing AST readers.

The candidate validated the complete JSON input before invoking constructors or setters, then bound no-argument POJOs directly. Constructor-based values fell back to the AST mapper. This preserved malformed-input behavior, but required another pass over the input.

The comparison used the same complete organization JSON, generated POJOs, direct writer, and read → write → read operation for both reader paths:

| Reader | Benchmark method | Time, µs/op | Allocated KiB/op |
| --- | --- | ---: | ---: |
| Direct reader candidate | `jaxJsonRoundTrip` | 157.65 ± 10.05 | 89.97 |
| Retained AST reader control | `jaxJsonTreeReaderRoundTrip` | 118.66 ± 1.86 | 88.34 |

The candidate took **32.9% longer** and allocated **1.8% more**. It was rejected despite passing 418 core tests and nine comparison cases, with 97.41% line coverage. The extra syntax-validation pass is the architectural tradeoff; this experiment does not isolate the cost of each individual method.

The other controls in [results.json](results.json) separate the reader and writer paths: `jaxJsonTreePipelineRoundTrip` uses both AST paths, and `jaxJsonTreeWriterRoundTrip` uses the candidate reader with the tree writer. XML reading was unchanged. These controls execute within one JMH invocation, but each method has its own JVM forks; the measurements are not simultaneous paired trials.

Settings: JDK 17, JMH 1.36, one thread, two forks per method, five 1-second warmup iterations and five 1-second measurement iterations per fork, 512 MiB heap. `±` is JMH's 99.9% confidence error. Dynatrace instrumentation was present in all 18 forks. These local results are not production guarantees and should not be mixed with scores from the separate retained-writer run.

Evidence:

- [Raw results](results.json)
- [Environment and fixture hashes](environment.json)
- [Actual fork arguments](forks.json)
- [Comparison correctness](correctness.xml)
- [Test and coverage totals](validation.json)
- [Candidate source hashes](source-sha256.json)
- [Candidate patch](candidate.patch), relative to the retained writer implementation

To reproduce the candidate, use a disposable copy of the retained implementation, apply `candidate.patch` with `git apply`, and run `./gradlew build compareLibraries` with JDK 17. The patch restores the additional comparison controls and regression tests. The source hashes describe the candidate, not the final retained implementation.
