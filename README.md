# JAX — JSON-And-XML to Java Object Converter API.

Generate Java records and POJOs from JSON or XML samples, then use them to read and write your documents with one small API. JAX runs on **Java 17+** with **zero runtime dependencies**.

**Tested with deeply nested JSON and XML:** JAX generates, compiles, and round-trips the [complex organization JSON](#complex-organization-json-round-trip) and [supply-chain XML manifest](#complete-supply-chain-xml-example). The JSON example works with all three model styles; the XML manifest uses POJOs or constructor-based classes to preserve attributes, element text, and namespace declarations.

## Features

1. **One Java file per type** — generate separate records or POJOs from JSON and XML using the CLI. Each object type gets its own source file.
2. **JSON and XML mapping** — read and write records and POJOs with automatic input-format detection.
3. **Strict and partial mapping** — validate record structure or select only the fields your application needs.
4. **Typed, nested data** — work with nested objects, lists, arrays, enums, optional values, and precise numeric types.
5. **Input safeguards and clear errors** — bounded nesting and numeric conversion, XML external-entity protection, and mapping errors with field paths.
6. **Schema-free JSON** — read JSON into an insertion-ordered `Document` without declaring a Java model.
7. **XML attributes, element text, and namespaces** — use POJOs with `@XmlAttribute` and `@XmlText` to preserve values such as `<Quantity unit="BOX">120</Quantity>`, and encode namespace prefixes or special characters in Java identifiers.

## Contents

- [Generate Java classes from JSON or XML](#generate-java-classes-from-json-or-xml)
- [Generated file layout](#generated-file-layout)
- [Complex-document support](#complex-document-support)
- [Complex JSON round trip](#complex-organization-json-round-trip)
- [Complete supply-chain XML example](#complete-supply-chain-xml-example)
- [What the API round-trip tests compare](#what-the-api-round-trip-tests-compare)
- [Why XML attributes and text currently require classes](#why-xml-attributes-and-text-currently-require-classes)
- [Troubleshooting](#troubleshooting)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Public API](#public-api)
- [Strict vs. partial mapping](#strict-vs-partial-mapping)
- [POJO mapping](#pojo-mapping)
- [Supported Java types](#supported-java-types)
- [Schema-free JSON](#schema-free-json)
- [XML attributes and namespaces](#xml-attributes-and-namespaces)
- [Mapping examples](#mapping-examples)
- [Safeguards and errors](#safeguards-and-errors)
- [Build and test](#build-and-test)
- [Performance benchmarks](#performance-benchmarks)
- [JAX vs. Jackson and Gson](#jax-vs-jackson-and-gson-complex-fixture-comparison)

## Generate Java classes from JSON or XML

Start with a sample document and let JAX create the Java model. The CLI accepts a UTF-8 `.json` or `.xml` **file path**. If your data is already a Java string, save it to a file first, as shown below.

### 1. Check prerequisites and build the JAR

Build the JAR from source using **JDK 17 or later**. The repository includes the Gradle wrapper, so a separate Gradle installation is not required. The first build needs internet access to download Gradle and build dependencies.

Check that Git and a JDK are available. Both Java commands should report version 17 or later:

```shell
git --version
java -version
javac -version
```

Clone the repository and build it:

```shell
git clone https://github.com/lemadane/jax.git
cd jax
./gradlew jar
```

If you already have a checkout, run `./gradlew jar` from its root directory. On Windows Command Prompt, use `gradlew.bat jar`; in PowerShell, use `.\gradlew.bat jar`.

The executable JAR is created at:

```text
build/libs/jax-0.2.0-SNAPSHOT.jar
```

Use this main JAR for source generation; `-sources.jar` and `-javadoc.jar` artifacts are documentation artifacts, not the CLI. The executable JAR has no third-party runtime dependencies and can be copied to another directory or machine with Java 17+ installed.

For an existing sample file, the command format is:

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo /path/to/order.xml
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create record /path/to/customer.json
```

If you move the JAR, replace `build/libs/jax-0.2.0-SNAPSHOT.jar` with its new path. Generated Java files are written beside the input file, so that directory must be writable.

### 2. Choose the model style

| Style | Generated model | When to use it |
| --- | --- | --- |
| `record` | One public record per Java file | JSON or XML without attributes |
| `pojo` | One mutable class per Java file, with getters and annotated setters | XML with attributes, or models you need to modify |
| `with-constructor` | One mutable class per Java file, with no-argument and all-fields constructors | The same data as `pojo`, with constructor-based initialization |

### Complex-document support

Complexity alone does not require hand-writing the Java model. JAX handles deeply nested objects, repeated elements, lists, and references between separate generated types. These examples are checked by [end-to-end tests](src/test/java/com/lemadane/jax/cli/JaxCliE2E.java) that generate source files, compile them, read the original data, serialize it, and read it again.

| Tested document | `record` | `pojo` | `with-constructor` |
| --- | --- | --- | --- |
| [Complex organization JSON](#complex-organization-json-round-trip): departments, budgets, vendors, contracts, projects, and milestones | Supported; use partial mapping for missing milestone fields in this sample | Supported | Supported |
| XML with nested child elements and repeated values, without attributes or namespace declarations | Supported | Supported | Supported |
| [Supply-chain XML](#complete-supply-chain-xml-example): routing legs, pallets, telemetry, dangerous goods, customs, attributes, and element text | Not currently supported | Supported and round-trip tested | Supported and round-trip tested |

**All styles produce individual Java files, with no nested classes or records.** Choose the style that supports your document's structure. Generated types are inferred from the supplied sample, so review them for optional fields and values that the sample does not contain. See [safeguards and errors](#safeguards-and-errors) for parsing and mapping limits.

#### Generated file layout

**Every style generates one public type per `.java` file.** A nested JSON or XML object becomes a separate record or class, referenced by its parent.

For the customer sample below, the output is:

```text
customer.json
Customer.java      ← name, age, and an Address property
Address.java       ← city
```

The equivalent record declarations are easy to read independently:

| File | Declaration |
| --- | --- |
| `Customer.java` | `public record Customer(String name, int age, Address address) {}` |
| `Address.java` | `public record Address(String city) {}` |

All files are created beside the input. Compile them together, or use `javac -sourcepath .` as shown in the walkthroughs. If two objects would produce the same type name, the generator uses distinct names such as `Address` and `Address2` and updates the references automatically.

Existing files are protected: the CLI checks every output path before creating the source files. Use a fresh directory for each independent model or move the previous generated files before regenerating.

### 3. Follow the XML or JSON walkthrough

Keep your terminal in the repository root for every command below. Choose either walkthrough; each is independent after building the JAR.

#### XML file with attributes

**Step 1 — Create the sample file.** Start JShell from the repository root:

```shell
jshell
```

Paste the following Java snippet into JShell. It writes `order.xml` in the current directory:

```java
import java.nio.file.Files;
import java.nio.file.Path;

String xml = """
        <Order id="A100" version="1">
            <customer>Ada</customer>
            <item sku="B1">
                <name>Notebook</name>
                <quantity>2</quantity>
            </item>
            <item sku="P2">
                <name>Pen</name>
                <quantity>3</quantity>
            </item>
        </Order>
        """;
Files.writeString(Path.of("order.xml"), xml);
```

Exit JShell to return to your terminal:

```text
/exit
```

**Step 2 — Generate the class.** Run:

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo order.xml
```

This creates **`Order.java` and `Item.java`** beside `order.xml`. The following excerpts show their fields; each complete file also includes public getters and `@JsonProperty` setters. Each public type is in its own file.

**`Order.java`**

```java
import com.lemadane.jax.XmlAttribute;
import java.util.List;

public class Order {
    @XmlAttribute("id") String id;
    @XmlAttribute("version") int version;
    String customer;
    List<Item> item;

    // Generated getters and setters omitted here.
}
```

**`Item.java`**

```java
import com.lemadane.jax.XmlAttribute;

public class Item {
    @XmlAttribute("sku") String sku;
    String name;
    int quantity;
    // Generated getters and setters omitted here.
}
```

Root and nested attributes become `@XmlAttribute` instance fields. Repeated `<item>` elements become a `List<Item>`, and nested objects become separate Java classes. Use `pojo` or `with-constructor` for this input: `record` generation rejects XML attributes because record mapping does not populate per-instance attributes.

**Step 3 — Create the application.** Save this complete example as `XmlExample.java` in the repository root. Use the actual generated `Order.java`, not the shortened excerpt above:

```java
import com.lemadane.jax.Jax;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlExample {
    public static void main(String[] args) throws Exception {
        Order order = Jax.toObject(Files.readString(Path.of("order.xml")), Order.class);
        System.out.println(order.id());
        System.out.println(order.item().get(0).sku());
        Files.writeString(Path.of("order-output.xml"), Jax.toXml(order));
    }
}
```

**Step 4 — Compile and run.** On Linux or macOS:

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar Order.java XmlExample.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" XmlExample
```

On Windows (Command Prompt or PowerShell):

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar Order.java XmlExample.java
java -cp ".;build/libs/jax-0.2.0-SNAPSHOT.jar" XmlExample
```

**Step 5 — Check the result.** The console prints:

```text
A100
B1
```

The application also creates `order-output.xml`. It retains the mapped `id`, `version`, and `sku` attributes. Formatting and attribute order may differ from the input.

#### JSON file with nested data

**Step 1 — Create the sample file.** From the repository root, start JShell:

```shell
jshell
```

Paste this Java snippet to write `customer.json`:

```java
import java.nio.file.Files;
import java.nio.file.Path;

String json = """
        {
            "name": "Ada",
            "age": 37,
            "address": {
                "city": "London"
            }
        }
        """;
Files.writeString(Path.of("customer.json"), json);
```

Exit JShell to return to your terminal:

```text
/exit
```

**Step 2 — Generate a record.** Run:

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create record customer.json
```

This creates two separate files beside `customer.json`:

**`Customer.java`**

```java
public record Customer(String name, int age, Address address) {}
```

**`Address.java`**

```java
public record Address(String city) {}
```

**Step 3 — Create the application.** Save this complete example as `JsonExample.java` in the repository root:

```java
import com.lemadane.jax.Jax;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExample {
    public static void main(String[] args) throws Exception {
        Customer customer = Jax.toObject(
                Files.readString(Path.of("customer.json")), Customer.class);
        System.out.println(customer.address().city());
        Files.writeString(Path.of("customer-output.json"), Jax.toJson(customer));
    }
}
```

**Step 4 — Compile and run.** On Linux or macOS:

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar Customer.java JsonExample.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" JsonExample
```

On Windows (Command Prompt or PowerShell):

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar Customer.java JsonExample.java
java -cp ".;build/libs/jax-0.2.0-SNAPSHOT.jar" JsonExample
```

**Step 5 — Check the result.** The console prints:

```text
London
```

The application also creates `customer-output.json` with the customer's mapped data.

To generate a mutable Java class instead, choose one of these commands **instead of** the record command in Step 2. Run only one generation command per input file; the CLI refuses to overwrite `Customer.java` or any referenced type file:

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo customer.json
# Or include an all-fields constructor:
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create with-constructor customer.json
```

### Complex organization JSON round trip

Generate a Java model from an organization response, read its nested data, and write it back to JSON.

**Browse the generated code:** each style has 23 separate Java files, saved permanently in [`examples/complex-organization/`](examples/complex-organization/).

| Model style | Generated Java source |
| --- | --- |
| Records (one file per type) | [record/](examples/complex-organization/record/) — start with [ComplexOrganization.java](examples/complex-organization/record/ComplexOrganization.java) |
| Mutable POJOs (one file per type) | [pojo/](examples/complex-organization/pojo/) — start with [ComplexOrganization.java](examples/complex-organization/pojo/ComplexOrganization.java) |
| POJOs with constructors (one file per type) | [with-constructor/](examples/complex-organization/with-constructor/) — start with [ComplexOrganization.java](examples/complex-organization/with-constructor/ComplexOrganization.java) |

These models use the [complex organization fixture](src/test/resources/com/lemadane/jax/cli/complex-organization.json). Every style has **one public type per file**, including `Environment.java`, `Organization.java`, `Budget.java`, and `Milestones.java`. Copy all Java files from your chosen style's directory into the same application package. No generated type is nested inside another type.

Choose one style and compile it separately; all three variants define `ComplexOrganization`. Add the same package declaration to the files you copy and include JAX on the classpath. The record model uses partial mapping for the original payload; the POJO styles support strict mapping.

To compile the separate record files from the repository root after building the JAR:

```shell
javac -cp build/libs/jax-0.2.0-SNAPSHOT.jar -d build/example-records examples/complex-organization/record/*.java
```

These permanent examples use the same separate-file layout produced by the CLI.

<details>
<summary><strong>Browse all 23 record files by area</strong></summary>

| Area | Record files |
| --- | --- |
| Response | [ComplexOrganization.java](examples/complex-organization/record/ComplexOrganization.java), [Pagination.java](examples/complex-organization/record/Pagination.java) |
| Environment | [Environment.java](examples/complex-organization/record/Environment.java), [Cluster.java](examples/complex-organization/record/Cluster.java), [AutoScaling.java](examples/complex-organization/record/AutoScaling.java), [Thresholds.java](examples/complex-organization/record/Thresholds.java) |
| Organization | [Organization.java](examples/complex-organization/record/Organization.java), [Departments.java](examples/complex-organization/record/Departments.java), [Budget.java](examples/complex-organization/record/Budget.java) |
| Vendors and contracts | [CostCenters.java](examples/complex-organization/record/CostCenters.java), [Allocations.java](examples/complex-organization/record/Allocations.java), [Vendors.java](examples/complex-organization/record/Vendors.java), [Contracts.java](examples/complex-organization/record/Contracts.java), [Sla.java](examples/complex-organization/record/Sla.java), [EscalationContacts.java](examples/complex-organization/record/EscalationContacts.java), [Channels.java](examples/complex-organization/record/Channels.java) |
| Projects and delivery | [Projects.java](examples/complex-organization/record/Projects.java), [Milestones.java](examples/complex-organization/record/Milestones.java), [Deliverables.java](examples/complex-organization/record/Deliverables.java), [Metrics.java](examples/complex-organization/record/Metrics.java), [LatencyMs.java](examples/complex-organization/record/LatencyMs.java), [Blockers.java](examples/complex-organization/record/Blockers.java), [AssignedTo.java](examples/complex-organization/record/AssignedTo.java) |

The POJO and constructor-based directories contain the same file names. Choose one complete directory for your application.

</details>

The walkthrough below generates a separate model named `OrganizationPayload` from its input filename.

| Sample data | What it demonstrates |
| --- | --- |
| Environment and cluster settings | Nested objects, booleans, and decimal thresholds |
| Departments, budgets, and vendor contracts | Multiple levels of objects and lists |
| SLA contacts and notification channels | Deeply nested strings and scalar lists |
| Projects, milestones, and deliverables | List items with different fields |
| Tax identifier and pagination cursor | Explicit `null` values |

**Before you start:** build the executable JAR using the instructions above, then keep your terminal in the repository root.

#### 1. Prepare the input

Start JShell:

```shell
jshell
```

Expand the sample and paste the Java snippet. It creates `organization-payload.json` in the current directory. The same payload is available as a [JSON file](src/test/resources/com/lemadane/jax/cli/complex-organization.json).

<details>
<summary><strong>View the complete JSON sample and file-creation snippet</strong></summary>

```java
import java.nio.file.Files;
import java.nio.file.Path;

String json = """
    {
      "apiVersion": "3.4.2-beta",
      "requestId": "req_98f7a6d5-4c3b-2a1e-0f9e-8d7c6b5a4f3e",
      "timestamp": "2026-11-15T09:49:12.402Z",
      "environment": {
        "region": "ap-southeast-1",
        "cluster": {
          "id": "prod-k8s-alpha-09",
          "nodes": 14,
          "autoScaling": {
            "enabled": true,
            "thresholds": {
              "cpuPercent": 78.5,
              "memoryPercent": 82.0
            }
          }
        }
      },
      "organization": {
        "orgId": "ORG-88412",
        "name": "Aether Dynamics Corp",
        "isVerified": true,
        "taxIdentifier": null,
        "departments": [
          {
            "deptId": "DEPT-ENG-01",
            "name": "Core Infrastructure",
            "budget": {
              "currency": "USD",
              "allocated": 1450000.00,
              "spent": 982340.55,
              "costCenters": [
                {
                  "code": "CC-101",
                  "allocations": [
                    {
                      "category": "Cloud Compute",
                      "monthlyLimits": [120000, 125000, 130000],
                      "vendors": [
                        {
                          "vendorId": "V-AWS",
                          "name": "Amazon Web Services",
                          "contracts": [
                            {
                              "contractId": "CNT-2026-99A",
                              "active": true,
                              "sla": {
                                "uptimeGuarantee": 99.99,
                                "supportTier": "Enterprise",
                                "escalationContacts": [
                                  {
                                    "role": "Technical Account Manager",
                                    "name": "Elena Rostova",
                                    "channels": {
                                      "email": "e.rostova@example.com",
                                      "slackWebhook": "[https://slack.com](https://slack.com)",
                                      "pagerDutyIds": ["PD-991", "PD-992"]
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            "projects": [
              {
                "projectId": "PRJ-NEXUS",
                "status": "IN_PROGRESS",
                "tags": ["distributed-systems", "rust", "zero-trust"],
                "milestones": [
                  {
                    "id": "M1",
                    "title": "Consensus Engine Refactor",
                    "completed": true,
                    "deliverables": [
                      {
                        "artifact": "raft-core-v2.tar.gz",
                        "checksum": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                        "metrics": {
                          "latencyMs": {
                            "p50": 1.2,
                            "p95": 4.8,
                            "p99": 11.4
                          },
                          "throughputRps": 142500
                        }
                      }
                    ]
                  },
                  {
                    "id": "M2",
                    "title": "Multi-Region Failover",
                    "completed": false,
                    "blockers": [
                      {
                        "issueId": "JIRA-4410",
                        "severity": "CRITICAL",
                        "assignedTo": {
                          "employeeId": "EMP-304",
                          "name": "Marcus Vance",
                          "onCall": true
                        }
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      },
      "pagination": {
        "page": 1,
        "pageSize": 50,
        "hasMore": false,
        "nextCursor": null
      }
    }
    """;
Files.writeString(Path.of("organization-payload.json"), json);
```

</details>

Exit JShell before running the next commands:

```text
/exit
```

#### 2. Generate the Java model

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create record organization-payload.json
```

The CLI creates **`OrganizationPayload.java`** and separate files for every referenced object type beside the input file. Each file contains one public record; object fields and lists reference those records by name.

The two milestones have different fields:

| Milestone | Present in the input | Missing from the input |
| --- | --- | --- |
| `M1` | `deliverables` | `blockers` |
| `M2` | `blockers` | `deliverables` |

The generated milestone record contains both fields. Use `toPartialObject` for the first read so missing lists receive empty-list defaults. Present values are still validated.

#### 3. Read and serialize the payload

Save this complete program as **`ComplexJsonExample.java`** beside the generated file:

```java
import com.lemadane.jax.Jax;
import java.nio.file.Files;
import java.nio.file.Path;

public class ComplexJsonExample {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("organization-payload.json"));
        OrganizationPayload payload = Jax.toPartialObject(json, OrganizationPayload.class);

        System.out.println(payload.organization().name());
        var milestones = payload.organization().departments().get(0)
                .projects().get(0).milestones();
        System.out.println(milestones.get(0).deliverables().size());
        System.out.println(milestones.get(1).blockers().get(0).assignedTo().name());

        String serialized = Jax.toJson(payload);
        Files.writeString(Path.of("organization-output.json"), serialized);
        OrganizationPayload reread = Jax.toObject(serialized, OrganizationPayload.class);
        System.out.println(Jax.toDocument(Jax.toJson(reread))
                .equals(Jax.toDocument(serialized)));
    }
}
```

The program writes `organization-output.json`, then reads it again using strict `toObject`. This second read succeeds because serialization includes the missing-list defaults.

#### 4. Compile and run

**Linux / macOS**

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar OrganizationPayload.java ComplexJsonExample.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" ComplexJsonExample
```

**Windows** — use the same `javac` command, then run:

```shell
java -cp ".;build/libs/jax-0.2.0-SNAPSHOT.jar" ComplexJsonExample
```

#### 5. Check the result

Expected console output:

```text
Aether Dynamics Corp
1
Marcus Vance
true
```

The final `true` confirms that reading and serializing the generated output produces equivalent JSON data.

| Output file | Contents |
| --- | --- |
| `OrganizationPayload.java` and referenced `.java` files | One generated record per file for the response and each object type |
| `organization-output.json` | Serialized response, including defaults for missing milestone lists |

Numeric values are preserved, but their spelling or scale may change: `1450000.00` can serialize as `1450000`. Missing milestone lists are written as `[]`.

<details>
<summary><strong>Using a mutable class instead of a record</strong></summary>

Choose `pojo` or `with-constructor` instead of `record` in the generation command. Move all existing generated Java files before trying another style; the CLI does not overwrite files.

| Generation style | Initial mapping | Missing milestone lists |
| --- | --- | --- |
| `record` | `Jax.toPartialObject(json, OrganizationPayload.class)` | Empty lists |
| `pojo` | `Jax.toObject(json, OrganizationPayload.class)` | `null` |
| `with-constructor` | `Jax.toObject(json, OrganizationPayload.class)` | `null` |

</details>

The [end-to-end test](src/test/java/com/lemadane/jax/cli/JaxCliE2E.java) generates and compiles all three styles, compares the entire serialized payload against the expected values and defaults, and verifies strict re-reading.

### Generation conventions

- For JSON, the input filename determines the top-level Java name: `order.json` produces `Order.java`. For XML, the CLI uses the actual root element name, preserving case and encoding special characters: `<Order>` produces `Order.java`, and `<log:GlobalSupplyChainManifest>` produces `log$colon$GlobalSupplyChainManifest.java`, regardless of the input filename.
- Every generated public class or record is written to its own `.java` file beside the input, with no package declaration. Move the complete set into your application and add the same package declaration to each file. Existing output paths are checked before any files are written, and existing files are never overwritten.
- Types are inferred from the sample. Review the generated model for values the sample does not cover, especially identifiers that must remain strings. JSON numeric arrays use every non-null entry to select a common numeric type (`int`, `long`, `BigInteger`, or `BigDecimal`), so an integer followed by a fractional value generates a `List<BigDecimal>` rather than truncating or rejecting that value.
- Nested objects become separate top-level types; JSON arrays and repeated XML elements become `List` properties. Compile the generated files together, or use `javac -sourcepath .` from their directory so referenced source files are compiled automatically.
- If inferred type names repeat, the generator adds a numeric suffix such as `Address2` and updates references. Names are compared without case to avoid filename conflicts on Windows and macOS. Names used by Java scalar types or JAX annotations are reserved; rename the JSON input file or XML root element if its root type conflicts.
- Use a separate input directory for each independent model to avoid clashes with previously generated types.
- Mutable classes have `name()` getters and `name(value)` setters annotated with `@JsonProperty`. Compile constructor-bound classes with `-parameters` so JAX can match constructor arguments. Empty objects generated with `with-constructor` have a single no-argument constructor; nonempty models also have an all-fields constructor.
- XML prefixes and special characters use the [name-encoding convention](#special-character-encoding), such as `$colon$` for `:` and `$hyphen$` for `-`.

### XML elements with attributes and text

Use `pojo` or `with-constructor` for XML such as identifiers, measurements and monetary values:

```java
String xml = """
        <OrganizationID qualifier="DUNS">094827165</OrganizationID>
        """;
```

JAX maps attributes with `@XmlAttribute` and the element's own text with `@XmlText`. A hand-written model can be as small as:

```java
import com.lemadane.jax.XmlAttribute;
import com.lemadane.jax.XmlText;

public class OrganizationID {
    @XmlAttribute public String qualifier;
    @XmlText public String value;
}
```

```java
OrganizationID id = Jax.toObject(xml, OrganizationID.class);
System.out.println(id.qualifier); // DUNS
System.out.println(id.value);     // 094827165
String output = Jax.toXml(id);    // Text stays inside OrganizationID, with no <value> child.
```

The generator names the text field `$text` and uses `String` to preserve leading zeros, whitespace and decimal spelling. Generated constructor parameters also carry `@XmlText`. Hand-written models can use scalar types such as `BigDecimal`, enums, or `Optional<BigDecimal>`; annotate both the backing field and constructor parameter when using constructor binding. Only one text field is allowed per class, including inherited fields.

### Why XML attributes and text currently require classes

**This is a current JAX limitation, not a limitation of Java records.** JAX uses separate mapping implementations for records and ordinary classes. The class implementation supports `@XmlAttribute` and `@XmlText` on fields and constructor parameters; the record implementation does not yet bind these annotations to record components for reading and writing.

For example, the `OrganizationID` element above has two values: its `qualifier` attribute and its text, `094827165`. Use the working POJO example above, or generate it with `--create pojo` or `--create with-constructor`. Annotating components of a hand-written record does **not** enable this mapping in the current release.

Records remain supported for complex JSON and XML represented through child-element components without attributes. Adding equivalent support for attributed XML records requires changes to source generation, record reading, and record serialization, followed by the same complete-document round-trip tests. That support is not included yet.

### Complete supply-chain XML example

The [complete manifest](src/test/resources/com/lemadane/jax/cli/supply-chain-manifest.xml) includes namespace prefixes, XML attributes, organization IDs, three transport legs, containers, pallets, IoT readings, dangerous goods, and customs values. Its pasted markup has been repaired; the supplied values and namespace URIs are retained.

**Browse the saved Java models:** [POJOs](examples/supply-chain/pojo/) and [classes with constructors](examples/supply-chain/with-constructor/) each contain 64 separate source files. Start with [log$colon$GlobalSupplyChainManifest.java](examples/supply-chain/pojo/log$colon$GlobalSupplyChainManifest.java). Choose one complete directory; the two styles use the same type names.

From the repository root, generate and compile its individual Java files:

```bash
./gradlew jar
mkdir -p generated/supply-chain
cp src/test/resources/com/lemadane/jax/cli/supply-chain-manifest.xml generated/supply-chain/
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo generated/supply-chain/supply-chain-manifest.xml
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar generated/supply-chain/*.java
```

Use a fresh directory to try `with-constructor`. Each generated type has its own source file; no nested classes are generated. Namespace declarations become `@XmlAttribute` fields so prefixes remain bound when writing XML. Repeated objects combine their fields recursively; properties absent from some occurrences use nullable types, and numeric types widen when needed.

Once the generated classes are on your application's classpath:

```java
String xml = Files.readString(Path.of("generated/supply-chain/supply-chain-manifest.xml"));
var manifest = Jax.toObject(xml, log$colon$GlobalSupplyChainManifest.class);
String output = Jax.toXml(manifest);
var restored = Jax.toObject(output, log$colon$GlobalSupplyChainManifest.class);
```

The [CLI end-to-end test](src/test/java/com/lemadane/jax/cli/JaxCliE2E.java) generates and compiles both class styles, compares every qualified element name, attribute and leaf value after serialization, and re-reads the result. Formatting, comments, CDATA boundaries and ordering of different child properties are not preserved. This is a mapping test, not validation against an XSD.

**Remaining boundaries:** XML record generation does not support attributes. Mixed content with interleaved text and child elements (for example, `before <b>bold</b> after`) is rejected by the generator and by `@XmlText` mapping because a single text property cannot represent its ordering.

### What the API round-trip tests compare

**Both complete fixtures passed their public-API round-trip tests.** The latest verified run passed all six dedicated cases below, plus the full suite of **396 tests** with **97.28% line coverage**.

| Fixture | API/model style | Verified result |
| --- | --- | --- |
| Complex organization JSON | `Document` | **Passed** — original structure and values preserved |
| Complex organization JSON | Records | **Passed** — all original values preserved, with two empty-list defaults |
| Complex organization JSON | POJOs | **Passed** — all original values preserved, with two `null` defaults |
| Complex organization JSON | Constructor-based classes | **Passed** — all original values preserved, with two `null` defaults |
| Supply-chain XML | POJOs | **Passed** — compared elements, attributes, namespaces, and values preserved |
| Supply-chain XML | Constructor-based classes | **Passed** — compared elements, attributes, namespaces, and values preserved |

XML records with attributes are **not supported** and are not included among these passing cases. Results describe data comparisons, not byte-for-byte equality; the exact comparison rules and JSON defaults are explained below.

The [public-API end-to-end tests](src/test/java/com/lemadane/jax/ComplexDocumentRoundTripE2E.java) use both complete fixtures and the saved Java models independently of the CLI. They compile each model style, call the public `Jax` methods, compare the full result, and repeat the round trip to verify stability. The separate CLI tests also verify source generation.

| API path | Comparison with the original fixture |
| --- | --- |
| Complex JSON → `Document` → JSON | The original structure and values are preserved, including explicit `null` values and absent properties. |
| Complex JSON → records → JSON | All original values are preserved. Partial mapping adds an empty `blockers` list to milestone M1 and an empty `deliverables` list to M2. |
| Complex JSON → either POJO style → JSON | All original values are preserved. The same two missing milestone properties are added as `null`. |
| Supply-chain XML → either POJO style → XML | Every qualified element name, attribute, namespace declaration, leaf value, and repeated-element occurrence is compared. Both styles must preserve these values on the second round trip too. |

**Equivalent data does not mean identical file text.** JSON whitespace, property order, and numeric spelling can change. XML formatting, comments, CDATA boundaries, and ordering between differently named child properties can change; repeated elements with the same name retain their order. The JSON model tests explicitly check the two added defaults, remove only those additions for comparison, and require every original value to match. Use `Document` when preserving the original JSON property presence matters.

Run just these API checks from the repository root:

```bash
./gradlew test --tests 'com.lemadane.jax.ComplexDocumentRoundTripE2E'
```

### Troubleshooting

| Problem | What to check or change |
| --- | --- |
| `java`, `javac`, or `jshell` is not found | Install a JDK 17+ and put its `bin` directory on `PATH`. Check that `java -version` and `javac -version` use the intended JDK. |
| `./gradlew: Permission denied` | On Linux/macOS, run `chmod +x gradlew`, then retry. |
| `Unable to access jarfile` | Run `./gradlew jar` from the repository root and check the JAR path. Quote paths containing spaces. |
| Input file cannot be found | Check the current directory or use an absolute path ending in `.json` or `.xml`. |
| Generated Java file already exists | Move the complete set of previously generated Java files before regenerating or use a fresh directory. Preserve any manual edits. |
| `package com.lemadane.jax does not exist` or `NoClassDefFoundError` | Include the JAX JAR with `-cp` when compiling and running. Runtime classpaths above include both `.` and the JAR. |
| Main class cannot be found | Run from the directory containing the compiled classes; use `:` between classpath entries on Linux/macOS and `;` on Windows. |
| XML root-name mismatch | Match the generated top-level class name to the XML root, using the documented encoding for special characters. |
| Record generation rejects XML attributes | Use `pojo` or `with-constructor`. |

## Installation

Requires **Java 17 or later**. Coordinates: `com.lemadane:jax:0.2.0-SNAPSHOT`.

The CLI walkthroughs above use the JAR directly. To use JAX as a dependency in another application, follow these steps.

**Step 1 — Publish to your local Maven repository.** From the JAX checkout, run:

```shell
./gradlew publishToMavenLocal
```

On Windows Command Prompt use `gradlew.bat publishToMavenLocal`; in PowerShell use `.\gradlew.bat publishToMavenLocal`.

**Step 2 — Configure your consuming application.** Choose Gradle or Maven below. This local installation does not require a published Maven Central artifact.

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.lemadane:jax:0.2.0-SNAPSHOT'
}
```

### Maven

Add this dependency inside your application’s `pom.xml` `<dependencies>` element. Maven checks the local repository automatically.

```xml
<dependency>
    <groupId>com.lemadane</groupId>
    <artifactId>jax</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Quick start

Save this complete example as `Example.java` in the JAX repository root after building the JAR:

```java
import com.lemadane.jax.Jax;

public class Example {
    public record Person(String name, int age) {}

    public static void main(String[] args) {
        Person fromJson = Jax.toObject(
                """
                {
                    "name": "Ada",
                    "age": 37
                }
                """, Person.class);
        Person fromXml = Jax.toObject(
                """
                <Person>
                    <name>Ada</name>
                    <age>37</age>
                </Person>
                """, Person.class);

        System.out.println(fromJson.equals(fromXml)); // true
        System.out.println(Jax.toJson(fromXml));      // {"name":"Ada","age":37}
        System.out.println(Jax.toXml(fromJson));      // XML with a Person root

        Person partial = Jax.toPartialObject(
                """
                {
                    "name": "Ada",
                    "extra": true
                }
                """, Person.class);
        System.out.println(partial);                 // Person[name=Ada, age=0]
    }
}
```

Compile and run on Linux/macOS:

```shell
javac -cp build/libs/jax-0.2.0-SNAPSHOT.jar Example.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" Example
```

On Windows, use the same compile command and run `java -cp ".;build/libs/jax-0.2.0-SNAPSHOT.jar" Example`. The output shows `true`, the JSON and XML representations, and `Person[name=Ada, age=0]`.

The reference snippets below show individual declarations or usage fragments unless labeled as complete examples. Place statements inside a method, and put each public top-level type in its own matching `.java` file. Include the relevant imports and keep the JAX JAR on the classpath.

Use `toObject` when every record component must be present. Use `toPartialObject` when input contains extra fields or only part of the record.

## Public API

Import `com.lemadane.jax.Jax` to access the five entry points:

| Method | Purpose |
| --- | --- |
| `toObject(input, targetType)` | Detect JSON or XML and map it strictly to a record or POJO. |
| `toPartialObject(input, targetType)` | Map declared properties, ignore extra data, and default missing values. |
| `toJson(object)` | Serialize a record or POJO to JSON. |
| `toXml(object)` | Serialize a record or POJO to XML. |
| `toDocument(json)` | Read a JSON object without declaring a model. |

All methods reject `null` arguments with `NullPointerException`.

`toJson()` and `toXml()` serialize either a Java record or a POJO. Record components are read through their accessors; POJO properties are read from public fields or public no-argument accessors. Private fields without a public accessor and static fields are ignored. Nested records and POJOs are serialized recursively. Collections and arrays are written as JSON arrays; for XML, each collection or array item is written as a repeated child element.

## Strict vs. partial mapping

| Behavior | Strict: `toObject` | Partial: `toPartialObject` |
| --- | --- | --- |
| Missing record component | Throws `MissingDataException` | Uses the defaults below |
| Undeclared property or child element | Throws a mapping exception | Ignores it, including in nested objects |
| Present but invalid value | Fails mapping | Fails mapping |
| Missing POJO property | Retains constructor or Java defaults | Retains constructor or Java defaults |

Strict record mapping also validates the XML root name and rejects duplicate XML children for non-collection components. JSON duplicate keys are rejected. Integer conversions reject fractional values and values outside the target type's range.

Missing record components in partial mode use these defaults:

| Component type | Default |
| --- | --- |
| `Optional<T>` | `Optional.empty()` |
| `List<T>` | `List.of()` |
| Other reference types | `null` |
| `boolean` | `false` |
| Numeric primitives | Zero |
| `char` | `'\0'` |


## POJO mapping

For mutable classes, JAX first looks for a unique constructor whose retained parameter names match the input. Compile constructor-bound classes with `-parameters`. If no constructor matches, a no-argument constructor is required.

Remaining properties map through public one-argument setters annotated with `@JsonProperty`. Setters may use `name(value)` or JavaBean-style `setName(value)` naming. XML also supports public instance fields. Unmapped JSON properties or XML elements fail in strict mode.

```java
public class Person {
    String name;
    int age;

    public String name() { return name; }
    @JsonProperty
    public void name(String value) { name = value; }
    public int age() { return age; }
    @JsonProperty
    public void age(int value) { age = value; }
}

Person person = Jax.toObject("""
    {
        "name": "Ada",
        "age": 37
    }
    """, Person.class);
```

Import `com.lemadane.jax.JsonProperty` to opt in setter methods for either JSON or XML mapping. This restriction applies consistently regardless of the input format detected by `Jax.toObject`.

For constructor-bound classes, compile the class with `-parameters` so Jax can match constructor arguments to input property names:

```shell
javac -parameters -sourcepath . -cp build/libs/jax-0.2.0-SNAPSHOT.jar Person.java
```

For XML-to-POJO mapping, use public fields or public one-argument setters annotated with `@JsonProperty`, named after the property (`name(String value)`) or using JavaBean naming (`setName(String value)`):

```java
public class PersonXml {
    public String name;
    public int age;
    public AddressXml address;
}

public class AddressXml {
    public String city;
}

PersonXml personFromXml = Jax.toObject(
        """
        <PersonXml>
            <name>Ada</name>
            <age>37</age>
            <address>
                <city>London</city>
            </address>
        </PersonXml>
        """,
        PersonXml.class);
```

For namespace-prefixed XML elements, encode `:` as `$colon$` in Java class, field, or constructor-parameter names. Other name characters follow the same `DataNameCodec` convention (`-` becomes `$hyphen$`, for example):

```java
public class fxg$colon$Person {
    public String fxg$colon$first$hyphen$name;
}

fxg$colon$Person person = Jax.toObject(
        """
        <fxg:Person xmlns:fxg="urn:example">
            <fxg:first-name>Ada</fxg:first-name>
        </fxg:Person>
        """,
        fxg$colon$Person.class);
```

## Supported Java types

* `String` (preserves leading zeros and identifier strings)

* Primitives & Wrappers (`boolean`/`Boolean`, `byte`/`Byte`, `short`/`Short`, `int`/`Integer`, `long`/`Long`, `float`/`Float`, `double`/`Double`, `char`/`Character`)

* `BigInteger` & `BigDecimal` (exact numeric precision preserved)

* `Optional<T>`

* `List<T>` (collections of records or primitives)

* Arrays

* Nested records

* Enums (`Enum.name()`)

## Schema-free JSON

Import `com.lemadane.jax.Document` for schema-free JSON access.

`toDocument(json)` returns Jax's own insertion-ordered `Document`, a map from JSON property names to inferred Java values. Nested objects become `Document` instances, arrays become lists, and JSON primitives become `String`, `Boolean`, `Integer`, `Long`, `BigDecimal`, or `null`. Integral numbers infer to `Integer`, then `Long`, and otherwise `BigDecimal`; decimal or exponent-form numbers infer to `BigDecimal`.

```java
Document document = Jax.toDocument("""
    {
        "name": "Ada",
        "age": 37,
        "address": {
            "city": "London"
        }
    }
    """);
String name = (String) document.get("name");
int age = (Integer) document.get("age");
Document address = (Document) document.get("address");
String city = (String) address.get("city");
```

## XML attributes and namespaces

Import `com.lemadane.jax.XmlAttribute` for attribute mapping.

For mutable POJOs, annotate instance fields with `@XmlAttribute` to map attributes per object instance. The annotation may provide an explicit XML name; otherwise the annotated Java field name is decoded with `DataNameCodec`. Constructor parameters may also be annotated, enabling immutable constructor-bound classes. Annotated fields are serialized as attributes by `Jax.toXml()` and are not treated as child elements.

```java
public class PersonXml {
    @XmlAttribute
    String id;

    @XmlAttribute("xml:version")
    int version;

    public String name;
}

PersonXml person = Jax.toObject(
        """
        <PersonXml id="A1" xml:version="1">
            <name>Ada</name>
        </PersonXml>
        """,
        PersonXml.class);
```

XML attributes are never read from or written to record static fields. Use a mutable POJO with `@XmlAttribute` fields or annotated constructor parameters for per-instance XML attributes. Prefixed XML names require an explicit namespace declaration; the writer throws `XmlMappingException` rather than emitting unbound prefixes.

Unmatched XML attributes are ignored even in strict mode.

XML collection serialization uses the reserved attributes `jax-empty-list="true"` for an empty collection and `jax-null="true"` for a null collection item. The XML mapper recognizes these markers when reading collections and arrays, allowing strict round trips to preserve empty collections and null list entries.

### Special-character encoding

JAX uses `DataNameCodec` to represent external names as Java identifiers. When reading XML, it matches element names against decoded Java type and property names; when writing XML, it decodes those identifiers back to the original names. The same convention applies to JSON keys and XML attribute names.

For example, `<fxg:first-name>` maps to `fxg$colon$first$hyphen$name`: `:` becomes `$colon$`, and `-` becomes `$hyphen$`. These escapes preserve the original name instead of removing punctuation.

| External Character | Reversible Identifier Escape | Example External Name | Encoded Java Component / Type |
| :--- | :--- | :--- | :--- |
| `:` | `$colon$` | `fxg:FacilityInboundEvent` | `fxg$colon$FacilityInboundEvent` |
| `-` | `$hyphen$` | `Facility-ID` | `Facility$hyphen$ID` |
| `.` | `$dot$` | `Facility.ID` | `Facility$dot$ID` |
| `space` | `$space$` | `display name` | `display$space$name` |
| `$` | `$dollar$` | `price$value` | `price$dollar$value` |
| Other | `$uXXXX$` | `emoji_😀` | `emoji_$u1F600$` |


The space and dollar-sign rows illustrate the shared codec for JSON keys; they are not valid XML element-name characters. Encoding a Java identifier does not make an invalid XML name valid. Other characters that cannot appear in a Java identifier use a hexadecimal Unicode escape such as `$u1F600$`; characters already valid in Java identifiers are preserved.

#### Reading prefixed elements

Use the encoded name for both the root record type and its components. This complete example combines a namespace prefix, a hyphen, and a dot:

```java
import com.lemadane.jax.Jax;
import com.lemadane.jax.naming.DataNameCodec;

public class XmlNamesExample {
    public record fxg$colon$Person(
            String fxg$colon$first$hyphen$name,
            String account$dot$id) {}

    public static void main(String[] args) {
        String xml = """
                <fxg:Person xmlns:fxg="urn:example">
                    <fxg:first-name>Ada</fxg:first-name>
                    <account.id>A123</account.id>
                </fxg:Person>
                """;

        var person = Jax.toObject(xml, fxg$colon$Person.class);
        System.out.println(person.fxg$colon$first$hyphen$name()); // Ada
        System.out.println(person.account$dot$id());             // A123

        System.out.println(DataNameCodec.encode("fxg:first-name"));
        // fxg$colon$first$hyphen$name
        System.out.println(DataNameCodec.decode("fxg$colon$first$hyphen$name"));
        // fxg:first-name
    }
}
```

JAX matches the qualified name, including its prefix. For this model, use `fxg:first-name`; changing it to `other:first-name` does not match even if both prefixes refer to the same namespace URI. Declare each prefix with `xmlns:prefix` in the XML input.

#### Writing prefixed elements

The writer restores `:` and other encoded characters, but it also needs a namespace declaration for each prefix. For a POJO, supply the declaration through an instance field such as:

```java
@XmlAttribute("xmlns:fxg")
public String namespace = "urn:example";
```

Place that field on the POJO being serialized with prefixed names. Records do not supply namespace declarations through static fields, so the record above demonstrates reading only; calling `toXml(person)` without a supported namespace declaration raises `XmlMappingException`.

## Mapping examples

The following snippets show model declarations and usage for larger documents. Add the relevant `Jax` and `java.util.List` imports in your application.

### 1. Standard XML Mapping

This hand-written record model maps child elements only. The `Version` attributes in the input are ignored and will not appear in `outputXml`. To preserve attributes, use the POJO generation walkthrough above.

**XML Input:**

```java
String xml = """
    <RestaurantOrderEvent Version="1.0">
        <KitchenOrderScan Version="1.0">
            <RestaurantID>0603</RestaurantID>
            <OrderType>DineIn</OrderType>
            <OrderTicketNumber>9611019061319607196193</OrderTicketNumber>
            <OrderTimestamp>
                <Date>20260721</Date>
                <Time24hr>123045</Time24hr>
                <Microseconds>527412</Microseconds>
            </OrderTimestamp>
            <PlaceHolder/>
        </KitchenOrderScan>
        <PlaceHolder/>
    </RestaurantOrderEvent>
    """;
```

**Java Records:**

```java
public record RestaurantOrderEvent(
        KitchenOrderScan KitchenOrderScan,
        PlaceHolder PlaceHolder
) {}

public record KitchenOrderScan(
        String RestaurantID,
        String OrderType,
        String OrderTicketNumber,
        OrderTimestamp OrderTimestamp,
        PlaceHolder PlaceHolder
) {}

public record OrderTimestamp(
        String Date,
        String Time24hr,
        String Microseconds
) {}

public record PlaceHolder() {}
```

**Usage:**

```java
final var event = Jax.toObject(xml, RestaurantOrderEvent.class);
final String restaurantID = event.KitchenOrderScan().RestaurantID();
final String outputXml = Jax.toXml(event);
```

### 2. Namespaced XML Mapping

**XML Input:**

```java
String xml = """
    <rest:RestaurantOrderEvent xmlns:rest="urn:restaurant:kitchen">
        <rest:KitchenOrderScan>
            <rest:RestaurantID>0603</rest:RestaurantID>
        </rest:KitchenOrderScan>
    </rest:RestaurantOrderEvent>
    """;
```

**Java Records:**

```java
public record rest$colon$RestaurantOrderEvent(
        rest$colon$KitchenOrderScan rest$colon$KitchenOrderScan
) {}

public record rest$colon$KitchenOrderScan(
        String rest$colon$RestaurantID
) {}
```

**Usage:**

```java
final var event = Jax.toObject(xml, rest$colon$RestaurantOrderEvent.class);
final String restaurantID = event.rest$colon$KitchenOrderScan().rest$colon$RestaurantID();
```

Prefixed XML input maps to the `$colon$`-encoded record component names. Record serialization does not
read static fields to create namespace declarations; `toXml()` rejects prefixed names unless the
namespace is supplied by another supported mapping path, such as a POJO `@XmlAttribute("xmlns:rest")`.

### 3. JSON Mapping

**JSON Input:**

```java
String json = """
    {
      "eventMessageCount": 2,
      "kitchenOrderScans": [
        {
          "restaurantID": "0603",
          "orderType": "DineIn"
        },
        {
          "restaurantID": "0417",
          "orderType": "Takeout"
        }
      ]
    }
    """;
```

**Java Records:**

```java
public record RestaurantOrderEvent(
        int eventMessageCount,
        List<KitchenOrderScan> kitchenOrderScans
) {}

public record KitchenOrderScan(
        String restaurantID,
        String orderType
) {}
```

**Usage:**

```java
final var event = Jax.toObject(json, RestaurantOrderEvent.class);
final String outputJson = Jax.toJson(event);
```

## Safeguards and errors

JSON and XML parsing enforce a maximum nesting depth of 256. Serialization also limits traversal to 256 values along any object-graph path, counting the root, collection items, object properties, and `Optional` contents. Excessive depth raises `JsonMappingException` or `XmlMappingException` rather than overflowing the stack. JSON Unicode escapes require exactly four ASCII hexadecimal digits. Numeric conversions from JSON numbers, JSON strings, XML text, and XML attributes are limited to 1,024 input characters and exponent magnitude 10,000, before constructing or expanding large integer values. Conversions to `float` and `double` reject non-finite results. XML serialization rejects characters not permitted by XML 1.0 and uses character references for tabs and line breaks in attributes.

### XML parser security

XML is streamed with the JDK StAX API into Jax-owned internal `XmlElement` nodes; W3C DOM is not used.
The parser disables DTD processing and external entities, and rejects `DOCTYPE` declarations.

### Mapping errors

Base unchecked exception:

* `DataMappingException`

Focused sub-exceptions with rich path context:

* `XmlMappingException`

* `JsonMappingException`

* `MissingDataException`

* `ExcessDataException`

* `TypeConversionException`

* `NameEncodingException`

* `RecordConstructionException`

Example rich path messages:

* `Missing XML element <OrderTicketNumber> required by com.example.KitchenOrderScan.OrderTicketNumber at /RestaurantOrderEvent/KitchenOrderScan.`

* `Excess JSON property "table" at $.table. No matching record component exists in com.example.KitchenOrderScan.`

* `Cannot convert JSON value "abc" to int for com.example.OrderData.itemCount at $.itemCount.`

## Build and test

To build the library, run tests, checkstyle static analysis, and verify JaCoCo coverage (>= 97%):

```bash
./gradlew clean build
```

`build` runs the tests, Checkstyle, and the 97% line-coverage gate. The [Java CI workflow](.github/workflows/ci.yml) runs these checks on every push and pull request and uploads verification reports, including on failure. Publishing on `main` or a published release requires this job to succeed. To block merges when verification fails, configure the repository ruleset or branch protection to require the `build-and-test` status check.

### Verification coverage

The latest local Java 17 verification passed the full build, Checkstyle, and coverage gate:

| Check | Result |
| --- | --- |
| Core tests | 396 passed; no failures or skipped tests |
| Library comparison tests | 7 passed: five library cases and two tree-writer controls |
| Line coverage | 97.28% (required: 97%) |
| Branch coverage | 92.53% |

Generation tests compile every emitted file and verify that all generated types are top-level. They cover all three model styles, JSON and XML round trips, XML attributes and text, duplicate type names, protection of existing files, and the complex organization payload. The complete supply-chain XML is generated and round-trip tested with both POJO styles; XML records with attributes remain unsupported. Dedicated public-API tests also compile the saved JSON and XML models and compare both complete fixtures independently of CLI generation. These figures describe the verified revision; rerun the build after changes for current results.

## Performance benchmarks

The JMH suite benchmarks parse/serialize/parse round trips for strict and partial
JSON/XML mapping, inferred JSON documents in a JSON round trip, and JSON/XML
serialization of both records and POJOs. Fixtures include five levels of nested
objects; XML fixtures include attributes, namespaces, and special element names.
Run it with:

```shell
./gradlew jmh
```

The default configuration uses three warmup iterations and five measurement iterations
of one second each, with one fork. Results are reported in microseconds per operation
and written in JMH JSON format to `build/results/jmh/results.json`.

### Sample Results

The following results are from one run of the five-level fixtures (JDK 25.0.2,
JMH 1.36). `±` is the JMH 99.9% confidence error:

| Benchmark | Average time (µs/op) |
|---|---:|
| `toDocumentJsonRoundTrip` | 33.943 ± 21.622 |
| `toJsonPojoRoundTrip` | 39.666 ± 44.619 |
| `toJsonRecordRoundTrip` | 16.631 ± 2.274 |
| `toObjectJsonRoundTrip` | 38.275 ± 14.893 |
| `toObjectXmlRoundTrip` | 548.564 ± 1347.661 |
| `toPartialObjectJsonRoundTrip` | 31.798 ± 13.307 |
| `toPartialObjectXmlRoundTrip` | 281.357 ± 483.888 |
| `toXmlPojoRoundTrip` | 185.662 ± 419.079 |
| `toXmlRecordRoundTrip` | 88.825 ± 202.631 |

These are illustrative, environment-specific measurements, not performance guarantees.
The benchmark JVM had an instrumentation agent attached, and the wide confidence errors
show that these results are noisy; use repeated runs on a controlled machine for comparisons.

## JAX vs. Jackson and Gson: complex-fixture comparison

See [Reader binding follow-up](#reader-binding-follow-up) for the latest implementation and its measurements. Earlier tables are retained as baselines.

This is a **separate comparison** from the JAX-only benchmark above. It uses the same complete [organization JSON](src/test/resources/com/lemadane/jax/cli/complex-organization.json) and [supply-chain XML](src/test/resources/com/lemadane/jax/cli/supply-chain-manifest.xml) used by the API round-trip tests—not smaller substitute payloads.

### Correctness results

All **five library comparison cases passed**. Two additional cases now verify the retained tree-writer controls:

| Fixture | JAX | Jackson | Gson |
| --- | --- | --- | --- |
| Complex organization JSON | Passed | Passed | Passed |
| Supply-chain XML | Passed | Passed with Jackson XML | Not applicable: no native XML mapper |

The [comparison tests](src/comparisonTest/java/com/lemadane/jax/comparison/LibraryComparisonTest.java) use the same saved POJO classes for every library: 23 JSON model files and 64 XML model files. They validate the full result after a read → write → read round trip.

- **JSON:** every original value is preserved. All three libraries add the same two missing milestone properties as `null`. Numbers are compared by decimal value, so differences in numeric spelling do not count as data loss.
- **XML:** every element and attribute is compared by namespace URI and local name, together with text values and repeated-element order. Prefix aliases, namespace-declaration placement, formatting, comments, and ordering between differently named child properties are not required to match. Jackson may choose different prefixes; this test checks XML meaning, not identical file text.
- **Configuration:** Jackson uses field visibility to read the shared generated POJOs. A [comparison-only annotation adapter](src/comparison/java/com/lemadane/jax/comparison/JaxXmlIntrospector.java) translates JAX XML names, attributes, and text metadata for Jackson; namespace URIs come from the original fixture. XML null properties are omitted. Gson uses `serializeNulls()` so explicit JSON null values are preserved. The adapter does not convert payload data.

Jackson XML is a [separate Jackson data-format module](https://github.com/FasterXML/jackson-dataformat-xml). [Gson provides JSON serialization and deserialization](https://github.com/google/gson); no XML conversion workaround is included in this comparison. Records, schema-free documents, source-generation speed, malformed-input handling, and other workloads are outside this comparison.

### Measured performance

Measured on **2026-11-15** using **JAX 0.2.0-SNAPSHOT**, **Jackson 2.20.1**, **Gson 2.13.2**, and **JMH 1.36**. These are the pinned versions tested, not a claim about every version of these libraries.

One operation is **input string → typed POJO → output string → typed POJO**. JMH consumes the returned object. File loading, class loading, mapper construction, and correctness comparisons are outside the timed operation; mapper instances are reused.

| Format | Library | Average time, µs/op ↓ | Allocated KiB/op ↓ |
| --- | --- | ---: | ---: |
| JSON | JAX | 354.29 ± 9.31 | 498.68 |
| JSON | Jackson | 35.39 ± 1.38 | 15.34 |
| JSON | Gson | 43.84 ± 5.63 | 31.16 |
| XML | JAX | 2,643.75 ± 663.14 | 2,915.49 |
| XML | Jackson XML | 462.11 ± 17.68 | 150.87 |

`±` is JMH's **99.9% confidence error** across ten measured iterations (five per fork). Allocations are measured by JMH's GC profiler and are not retained heap size or peak memory usage.

Jackson had the lowest measured time and allocation for both fixtures. For JSON, JAX took about **10.0×** Jackson’s time and **8.1×** Gson’s time. For XML, JAX took about **5.7×** Jackson XML’s time, with a wider confidence interval. These results identify performance and allocation improvements to pursue in JAX; they do not establish a universal ranking across workloads.

**Run conditions:** Amazon Corretto **17.0.17+10-LTS**, Linux amd64, Intel Core i7-10710U, 12 available logical processors; one benchmark thread, two JVM forks, five 1-second warmup iterations and five 1-second measurement iterations per fork, with a fixed 512 MiB heap. Inputs are **3,773 UTF-8 bytes of JSON** and **13,948 UTF-8 bytes of XML**. Both XML implementations use **Woodstox 7.1.1** in this comparison; adding Jackson XML supplies that StAX provider on the comparison classpath.

**Measurement limit:** A **Dynatrace monitoring agent was injected into all ten measured JVM forks**. Its actual arguments are recorded below. Instrumentation can affect both timing and allocation, so this is an instrumented local comparison. These are local, warmed-up measurements on two fixtures, not production latency guarantees. Do not compare the absolute times directly with the older JAX-only table, which used different fixtures and a different JDK.

### Reproduce and inspect the results

```bash
# Correctness only: five library cases and two tree-writer controls
./gradlew comparisonTest

# Separate timing run; correctness checks must pass first
./gradlew --no-daemon --max-workers=1 compareLibraries
```

Use JDK 17 to reproduce this run. The first run downloads the pinned comparison dependencies. Jackson, Gson, and JMH are confined to comparison configurations and are not runtime dependencies of the published JAX library. `build` also runs all seven correctness checks; performance measurements are opt-in and have no timing threshold in CI.

Fresh timing results go to `build/results/library-comparison/`. The recorded run is saved permanently under [`benchmarks/library-comparison/2026-11-15/`](benchmarks/library-comparison/2026-11-15/):

- [Raw JMH scores, confidence intervals, per-iteration data, and allocation metrics](benchmarks/library-comparison/2026-11-15/results.json)
- [Environment, pinned versions, fixture sizes, and SHA-256 hashes](benchmarks/library-comparison/2026-11-15/environment.json)
- [Actual JVM arguments from all measured forks](benchmarks/library-comparison/2026-11-15/forks.json)
- [Five passing correctness cases](benchmarks/library-comparison/2026-11-15/correctness.xml)

The [benchmark methods](src/comparison/java/com/lemadane/jax/comparison/LibraryComparisonBenchmark.java) and [shared workload/configuration](src/comparison/java/com/lemadane/jax/comparison/ComparisonWorkload.java) are included for review.

### Optimization follow-up

JAX now caches POJO constructors, setters, fields, getters, and XML text metadata by class using `ClassValue`. It retains input-dependent constructor selection and validation on every call. XML serialization reuses inherited namespace mappings until a new declaration requires a copy. JSON parsing and escaping avoid temporary builders for strings without escapes.

The same complete fixtures and JMH settings were rerun after these changes. The original comparison above is retained as the baseline; its raw files have not been overwritten.

| Format | JAX before, µs/op | JAX after, µs/op | JAX speedup | Jackson in new run, µs/op | Gson in new run, µs/op |
| --- | ---: | ---: | ---: | ---: | ---: |
| JSON | 354.29 | 111.86 ± 3.75 | 3.17× | 35.38 ± 2.79 | 46.14 ± 6.42 |
| XML | 2,643.75 | 1,189.28 ± 147.04 | 2.22× | 447.00 ± 18.26 | N/A |

| Format | JAX allocation before, KiB/op | JAX allocation after, KiB/op | Reduction |
| --- | ---: | ---: | ---: |
| JSON | 498.68 | 119.57 | 76.0% |
| XML | 2,915.49 | 788.86 | 72.9% |

**JAX improved substantially but has not matched Jackson on these fixtures.** In the new run, JAX still takes about **3.2×** Jackson’s JSON time and **2.7×** its XML time; Gson also remains faster for JSON. Further work should target the remaining intermediate trees and mapping allocations, guided by profiling. The `±` values use the same JMH confidence convention as the original table.

Validation: **367 core tests and five library comparison cases passed**, with **97.21% line coverage** and the 97% coverage gate unchanged. New regressions exercise concurrent JSON/XML round trips, inherited accessors, attributes and text, validation after cache warmup, and plain/escaped Unicode strings. XML namespace-scope tests also pass.

The monitoring-agent limitation still applies. Speedups compare separate local runs and are not production guarantees. See the [optimized raw JMH results](benchmarks/library-comparison/2026-11-15/optimized/results.json), [environment](benchmarks/library-comparison/2026-11-15/optimized/environment.json), [fork arguments](benchmarks/library-comparison/2026-11-15/optimized/forks.json), and [correctness results](benchmarks/library-comparison/2026-11-15/optimized/correctness.xml). Run `./gradlew build compareLibraries` with JDK 17 to verify and repeat the measurements.

The [validation counts and coverage counters](benchmarks/library-comparison/2026-11-15/optimized/validation.json) and [source hashes](benchmarks/library-comparison/2026-11-15/optimized/source-sha256.json) identify the verified implementation.

### AST optimization follow-up

At this stage, JAX used intermediate syntax trees: its recursive-descent JSON parser produced `JsonNode` objects, and its StAX-based XML reader produced `XmlElement` objects. Mapping walked those trees to construct Java objects; serialization also built a tree before writing text.

That update reduced tree-processing overhead while retaining that architecture:

- JSON parsing reuses one diagnostic-path buffer per parse instead of allocating a complete path string for every property and array item. Errors still report the full location.
- Immutable JSON `true`, `false`, and `null` nodes are shared during parsing and serialization.
- XML nodes reuse their read-only collection views, preserving live updates while the tree is built.
- XML text traversal returns existing leaf text directly and uses one builder for nested mixed content.

The same full fixtures, JDK, heap, warmup, forks, and round-trip operation were used again:

| Format | Library | Average time, µs/op ↓ | Allocated KiB/op ↓ |
| --- | --- | ---: | ---: |
| JSON | JAX | 129.33 ± 11.72 | 99.03 |
| JSON | Jackson | 48.58 ± 9.10 | 15.34 |
| JSON | Gson | 52.05 ± 1.84 | 31.05 |
| XML | JAX | 1,546.98 ± 146.02 | 782.83 |
| XML | Jackson XML | 595.34 ± 39.20 | 150.91 |

Compared with the previous optimization run, **JAX JSON allocation fell from 119.57 to 99.03 KiB/op (17.2%)**. XML allocation stayed roughly flat, changing from 788.86 to 782.83 KiB/op (0.8%). **This run does not establish a latency improvement:** timings increased for JAX and for the unchanged Jackson/Gson implementations, indicating different run conditions. Jackson and Gson remain faster on the supported fixtures. Reducing or avoiding complete intermediate trees remains a larger architectural opportunity.

**Validation:** 375 core tests and five comparison cases passed, with 97.23% line coverage and 91.90% branch coverage. New regressions check exact nested error locations after sibling traversal, retained read-only XML views, and text order after child edits. The 97% coverage gate remains unchanged.

These remain instrumented local measurements: Dynatrace was present in every benchmark fork. The `±` values are JMH's 99.9% confidence errors. The original and previous optimization results above are retained for comparison.

Recorded evidence: [raw JMH results](benchmarks/library-comparison/2026-11-15/ast/results.json), [environment](benchmarks/library-comparison/2026-11-15/ast/environment.json), [fork arguments](benchmarks/library-comparison/2026-11-15/ast/forks.json), [comparison correctness](benchmarks/library-comparison/2026-11-15/ast/correctness.xml), [test/coverage totals](benchmarks/library-comparison/2026-11-15/ast/validation.json), and [source hashes](benchmarks/library-comparison/2026-11-15/ast/source-sha256.json). Reproduce with JDK 17 and `./gradlew build compareLibraries`.

### Direct serialization follow-up

JAX now writes JSON and XML directly from Java objects, avoiding a complete output tree on the normal path. JSON retains the tree writer for unusual maps or Java fields whose decoded names collide, preserving their existing behavior. Name encoding, namespace scope, attributes, text, collection markers, escaping, cycle detection, and depth limits remain in place. Reading still uses the existing AST mappers.

The same complete fixtures were tested again. One operation remains **string → typed POJO → string → typed POJO**. Two additional benchmark methods use the retained tree writers with the same readers, allowing the write-path change to be compared within one run.

| Format | Implementation | Average time, µs/op ↓ | Allocated KiB/op ↓ |
| --- | --- | ---: | ---: |
| JSON | JAX direct writer | 95.30 ± 1.31 | 88.33 |
| JSON | JAX tree-writer control | 105.83 ± 5.96 | 99.03 |
| JSON | Jackson | 35.28 ± 1.98 | 15.34 |
| JSON | Gson | 38.56 ± 5.91 | 31.05 |
| XML | JAX direct writer | 1,109.04 ± 126.26 | 719.73 |
| XML | JAX tree-writer control | 1,104.18 ± 90.93 | 769.27 |
| XML | Jackson XML | 462.55 ± 28.36 | 150.87 |

**JSON round-trip time fell by 9.9% and allocation by 10.8%** relative to the tree-writer control in this run. **XML allocation fell by 6.4%; no XML latency improvement was established**, because the timing difference is within the confidence errors. JAX still takes about **2.7× Jackson's JSON time**, **2.5× Gson's JSON time**, and **2.4× Jackson XML's time** on these fixtures.

**Validation:** 384 core tests and seven comparison cases passed, with **97.38% line coverage** and **92.41% branch coverage**. The 97% line gate is unchanged. The two added comparison cases verify the complete fixtures through the retained tree writers and require their output to match direct serialization exactly. New regressions cover deterministic random nested graphs, decoded-name collisions, inherited fields, unusual map keys, primitive arrays, empty/null values, failing accessors, numeric boundaries, namespace rebinding, and attributes with text.

A direct JSON reader was also prototyped and tested. It validated the entire input before binding, preserving the rule that malformed input cannot call constructors or setters. However, with the same direct writer, it measured **157.65 ± 10.05 µs/op**, versus **118.66 ± 1.86 µs/op** for the AST reader in that experiment: about **33% longer**, with slightly more allocation. **That reader was not retained.** Its [results and reproducible candidate patch](benchmarks/library-comparison/2026-11-15/direct-reader-experiment/README.md) are saved separately. This experiment shows why removing a tree alone is not sufficient: an extra validation pass can outweigh the savings. These experimental timings come from a different run and should not be substituted into the retained-implementation table above.

Settings remain JDK 17, JMH 1.36, one thread, two forks, five 1-second warmup and measurement iterations per fork, and a fixed 512 MiB heap. Both XML implementations use Woodstox 7.1.1. Dynatrace was present in all 14 measured forks of the retained implementation. `±` is JMH's 99.9% confidence error; allocations are bytes allocated per operation, not retained or peak memory. These are local instrumented measurements on two fixtures, not production guarantees.

Recorded evidence: [raw JMH results](benchmarks/library-comparison/2026-11-15/direct-writers/results.json), [environment](benchmarks/library-comparison/2026-11-15/direct-writers/environment.json), [fork arguments](benchmarks/library-comparison/2026-11-15/direct-writers/forks.json), [comparison correctness](benchmarks/library-comparison/2026-11-15/direct-writers/correctness.xml), [test/coverage totals](benchmarks/library-comparison/2026-11-15/direct-writers/validation.json), and [source hashes](benchmarks/library-comparison/2026-11-15/direct-writers/source-sha256.json). The implementation was restored to these exact source hashes after rejecting the reader prototype. Reproduce with JDK 17 and `./gradlew build compareLibraries`.

### Reader binding follow-up

The JSON and XML POJO readers now cache setter parameter types and skip constructor matching for classes with only a no-argument constructor. XML serialization also avoids copying text and attribute values that need no escaping, while still validating every character. Parsing, encoded names, namespace handling, attributes, constructor selection, and strict/partial mapping retain their existing behavior.

The same complete fixtures were measured again with **string → typed POJO → string → typed POJO**. These are the latest results; tree-writer controls use the current readers and shared escaping code.

| Format | Implementation | Average time, µs/op ↓ | Allocated KiB/op ↓ |
| --- | --- | ---: | ---: |
| JSON | JAX | 106.09 ± 1.76 | 80.92 |
| JSON | JAX tree-writer control | 121.74 ± 6.49 | 91.93 |
| JSON | Jackson | 38.75 ± 6.75 | 15.34 |
| JSON | Gson | 35.67 ± 1.17 | 31.05 |
| XML | JAX | 1,360.47 ± 133.55 | 685.03 |
| XML | JAX tree-writer control | 1,380.94 ± 110.35 | 735.42 |
| XML | Jackson XML | 566.36 ± 39.60 | 150.89 |

Compared with a fresh run of the saved pre-change sources, **JSON allocation fell 8.4%** (88.33 → 80.92 KiB/op) and **XML allocation fell 5.0%** (721.06 → 685.03 KiB/op). JSON mean time moved from 123.53 ± 16.73 to 106.09 ± 1.76 µs/op; XML moved from 1,395.02 ± 183.72 to 1,360.47 ± 133.55 µs/op. Timing confidence intervals overlap, and the control libraries also moved between these separate runs, so the measurements do not isolate a reliable latency improvement from the code change. The clearest improvement is lower allocation. JAX still takes about **2.7× Jackson's JSON time**, **3.0× Gson's JSON time**, and **2.4× Jackson XML's time** in the latest run.

**Validation:** 396 core tests and seven comparison cases passed, with **97.28% line coverage** and **92.53% branch coverage**. The 97% line gate and Checkstyle both pass. Twelve added regressions cover cached setters, inherited/encoded names, generic collections and arrays, constructor selection, delayed failures, Unicode escaping, and invalid characters before and after an escape. Validation also caught and corrected an unrelated blank entry where `private` belonged in the source generator's Java keyword list.

An XML field/child indexing candidate was tested and **discarded**: it increased XML allocation by 2.1% without a clear timing benefit. Its results are retained separately in the [experiment report](benchmarks/library-comparison/2026-11-15/reader-bindings/README.md#rejected-xml-index-experiment).

Settings remain Java 17, one thread, two forks, five 1-second warmup and measurement iterations, and a 512 MiB heap. Both XML libraries use Woodstox 7.1.1. Dynatrace was active in all 14 forks of each run. `±` is JMH's 99.9% confidence error; allocation is per operation, not retained heap. These local measurements are not production guarantees.

Evidence: [before results](benchmarks/library-comparison/2026-11-15/reader-bindings/before/results.json), [latest results](benchmarks/library-comparison/2026-11-15/reader-bindings/after/results.json), [environment](benchmarks/library-comparison/2026-11-15/reader-bindings/after/environment.json), [forks](benchmarks/library-comparison/2026-11-15/reader-bindings/after/forks.json), [correctness](benchmarks/library-comparison/2026-11-15/reader-bindings/after/correctness.xml), [test/coverage totals](benchmarks/library-comparison/2026-11-15/reader-bindings/after/validation.json), [source hashes](benchmarks/library-comparison/2026-11-15/reader-bindings/after/source-sha256.json), and [methodology and reproduction](benchmarks/library-comparison/2026-11-15/reader-bindings/README.md). Reproduce the retained implementation with Java 17 and `./gradlew build compareLibraries`.

## License

Licensed under the [MIT License](LICENSE).
