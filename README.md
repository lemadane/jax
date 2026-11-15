# JAX — JSON-And-XML to Java Object Converter API.

Generate Java records and POJOs from JSON or XML samples, then use them to read and write your documents with one small API. JAX runs on **Java 17+** with **zero runtime dependencies**.

## Features

1. **Source generation** — generate records or POJOs from sample JSON and XML using the CLI.
2. **JSON and XML mapping** — read and write records and POJOs with automatic input-format detection.
3. **Strict and partial mapping** — validate record structure or select only the fields your application needs.
4. **Typed, nested data** — work with nested objects, lists, arrays, enums, optional values, and precise numeric types.
5. **Input safeguards and clear errors** — bounded nesting and numeric conversion, XML external-entity protection, and mapping errors with field paths.
6. **Schema-free JSON** — read JSON into an insertion-ordered `Document` without declaring a Java model.
7. **XML attributes and special names** — map instance attributes and encode namespace prefixes or special characters in Java identifiers.

## Contents

- [Generate Java classes from JSON or XML](#generate-java-classes-from-json-or-xml)
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
| `record` | Records with nested records | JSON or XML without attributes |
| `pojo` | Mutable classes with getters and annotated setters | XML with attributes, or models you need to modify |
| `with-constructor` | Mutable classes with no-argument and all-fields constructors | The same data as `pojo`, with constructor-based initialization |

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

This creates `Order.java` beside `order.xml`. The following excerpt shows the generated fields; the complete output also includes public getters and `@JsonProperty` setters:

```java
import com.lemadane.jax.XmlAttribute;
import java.util.List;

public class Order {
    @XmlAttribute("id") String id;
    @XmlAttribute("version") int version;
    String customer;
    List<Item> item;

    public static class Item {
        @XmlAttribute("sku") String sku;
        String name;
        int quantity;
        // Generated getters and setters omitted here.
    }
    // Generated getters and setters omitted here.
}
```

Root and nested attributes become `@XmlAttribute` instance fields. Repeated `<item>` elements become a `List<Item>`, and nested elements become nested Java classes. Use `pojo` or `with-constructor` for this input: `record` generation rejects XML attributes because record mapping does not populate per-instance attributes.

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
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar Order.java XmlExample.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" XmlExample
```

On Windows (Command Prompt or PowerShell):

```shell
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar Order.java XmlExample.java
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

This creates `Customer.java`:

```java
public record Customer(String name, int age, Address address) {
    public record Address(String city) {}
}
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
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar Customer.java JsonExample.java
java -cp ".:build/libs/jax-0.2.0-SNAPSHOT.jar" JsonExample
```

On Windows (Command Prompt or PowerShell):

```shell
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar Customer.java JsonExample.java
java -cp ".;build/libs/jax-0.2.0-SNAPSHOT.jar" JsonExample
```

**Step 5 — Check the result.** The console prints:

```text
London
```

The application also creates `customer-output.json` with the customer's mapped data.

To generate a mutable Java class instead, choose one of these commands **instead of** the record command in Step 2. Run only one generation command per input file; the CLI refuses to overwrite `Customer.java`:

```shell
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo customer.json
# Or include an all-fields constructor:
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create with-constructor customer.json
```

### Generation conventions

- The input filename determines the top-level Java name: `order.xml` produces `Order.java`. For strict XML mapping, choose a filename that produces a class name matching the root element.
- Generated files are written beside the input with no package declaration. Move them into your application and add its package declaration as needed. Existing Java files are never overwritten.
- Types are inferred from the sample. Review the generated model for values the sample does not cover, especially identifiers that must remain strings.
- Nested objects become nested types; JSON arrays and repeated XML elements become `List` properties.
- Mutable classes have `name()` getters and `name(value)` setters annotated with `@JsonProperty`. Compile constructor-bound classes with `-parameters` so JAX can match constructor arguments.
- XML prefixes and special characters use the [name-encoding convention](#special-character-encoding), such as `$colon$` for `:` and `$hyphen$` for `-`.

### Troubleshooting

| Problem | What to check or change |
| --- | --- |
| `java`, `javac`, or `jshell` is not found | Install a JDK 17+ and put its `bin` directory on `PATH`. Check that `java -version` and `javac -version` use the intended JDK. |
| `./gradlew: Permission denied` | On Linux/macOS, run `chmod +x gradlew`, then retry. |
| `Unable to access jarfile` | Run `./gradlew jar` from the repository root and check the JAR path. Quote paths containing spaces. |
| Input file cannot be found | Check the current directory or use an absolute path ending in `.json` or `.xml`. |
| Generated Java file already exists | Move or rename the existing generated file before regenerating. Preserve any manual edits. |
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
javac -parameters -cp build/libs/jax-0.2.0-SNAPSHOT.jar Person.java
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

## License

Licensed under the [MIT License](LICENSE).
