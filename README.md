# JAX — JSON-And-XML-To-Record

Map JSON and XML to Java records and POJOs, and serialize them back with one small API. JAX runs on **Java 17+** with **zero runtime dependencies**.

## Features

1. **JSON and XML mapping** — read and write records and POJOs with automatic input-format detection.
2. **Strict and partial mapping** — validate record structure or select only the fields your application needs.
3. **Typed, nested data** — work with nested objects, lists, arrays, enums, optional values, and precise numeric types.
4. **Input safeguards and clear errors** — bounded nesting and numeric conversion, XML external-entity protection, and mapping errors with field paths.
5. **Schema-free JSON** — read JSON into an insertion-ordered `Document` without declaring a Java model.
6. **XML attributes and special names** — map instance attributes and encode namespace prefixes or special characters in Java identifiers.
7. **Source generation** — generate records or POJOs from sample JSON and XML using the CLI.

## Contents

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
- [Command-line source generation](#command-line-source-generation)
- [Build and test](#build-and-test)
- [Performance benchmarks](#performance-benchmarks)

## Installation

Requires **Java 17 or later**. Coordinates: `com.lemadane:jax:0.2.0-SNAPSHOT`.

The dependency declarations below require a repository containing the artifact. To use a local checkout, run `./gradlew publishToMavenLocal` and add `mavenLocal()` to your Gradle repositories; Maven uses its local repository automatically.

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.lemadane:jax:0.2.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>com.lemadane</groupId>
    <artifactId>jax</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Quick start

Save this example as `Example.java` and run it with JAX on the classpath:

```java
import com.lemadane.jax.Jax;

public class Example {
    public record Person(String name, int age) {}

    public static void main(String[] args) {
        Person fromJson = Jax.toObject(
                "{\"name\":\"Ada\",\"age\":37}", Person.class);
        Person fromXml = Jax.toObject(
                "<Person><name>Ada</name><age>37</age></Person>", Person.class);

        System.out.println(fromJson.equals(fromXml)); // true
        System.out.println(Jax.toJson(fromXml));      // {"name":"Ada","age":37}
        System.out.println(Jax.toXml(fromJson));      // XML with a Person root

        Person partial = Jax.toPartialObject(
                "{\"name\":\"Ada\",\"extra\":true}", Person.class);
        System.out.println(partial);                 // Person[name=Ada, age=0]
    }
}
```

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

Person person = Jax.toObject("{\"name\":\"Ada\",\"age\":37}", Person.class);
```

Import `com.lemadane.jax.JsonProperty` to opt in setter methods for either JSON or XML mapping. This restriction applies consistently regardless of the input format detected by `Jax.toObject`.

For constructor-bound classes, compile the class with `-parameters` so Jax can match constructor arguments to input property names:

```shell
javac -parameters Person.java
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
        "<PersonXml><name>Ada</name><age>37</age><address><city>London</city></address></PersonXml>",
        PersonXml.class);
```

For namespace-prefixed XML elements, encode `:` as `$colon$` in Java class, field, or constructor-parameter names. Other name characters follow the same `DataNameCodec` convention (`-` becomes `$hyphen$`, for example):

```java
public class fxg$colon$Person {
    public String fxg$colon$first$hyphen$name;
}

fxg$colon$Person person = Jax.toObject(
        "<fxg:Person xmlns:fxg=\"urn:example\"><fxg:first-name>Ada</fxg:first-name></fxg:Person>",
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
Document document = Jax.toDocument("{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\"}}");
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
        "<PersonXml id=\"A1\" xml:version=\"1\"><name>Ada</name></PersonXml>",
        PersonXml.class);
```

XML attributes are never read from or written to record static fields. Use a mutable POJO with `@XmlAttribute` fields or annotated constructor parameters for per-instance XML attributes. Prefixed XML names require an explicit namespace declaration; the writer throws `XmlMappingException` rather than emitting unbound prefixes.

Unmatched XML attributes are ignored even in strict mode.

XML collection serialization uses the reserved attributes `jax-empty-list="true"` for an empty collection and `jax-null="true"` for a null collection item. The XML mapper recognizes these markers when reading collections and arrays, allowing strict round trips to preserve empty collections and null list entries.

### Special-character encoding

XML element/attribute names and JSON keys containing characters problematic in Java identifiers are reversibly escaped using `$` as the marker:

| External Character | Reversible Identifier Escape | Example External Name | Encoded Java Component / Type |
| :--- | :--- | :--- | :--- |
| `:` | `$colon$` | `fxg:FacilityInboundEvent` | `fxg$colon$FacilityInboundEvent` |
| `-` | `$hyphen$` | `Facility-ID` | `Facility$hyphen$ID` |
| `.` | `$dot$` | `Facility.ID` | `Facility$dot$ID` |
| `space` | `$space$` | `display name` | `display$space$name` |
| `$` | `$dollar$` | `price$value` | `price$dollar$value` |
| Other | `$uXXXX$` | `emoji_😀` | `emoji_$u1F600$` |

## Mapping examples

The following snippets show model declarations and usage for larger documents. Add the relevant `Jax` and `java.util.List` imports in your application.

### 1. Standard XML Mapping

**XML Input:**

```xml
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

```xml
<rest:RestaurantOrderEvent xmlns:rest="urn:restaurant:kitchen">
    <rest:KitchenOrderScan>
        <rest:RestaurantID>0603</rest:RestaurantID>
    </rest:KitchenOrderScan>
</rest:RestaurantOrderEvent>
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

```json
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

## Command-line source generation

The executable JAR can generate a Java record, mutable POJO, or mutable POJO with an all-fields
constructor from a JSON or XML sample. The generated source is written beside the input file, named
from the input filename, and has no package declaration so you can move it to your chosen package.
It refuses to overwrite an existing Java source file.

```shell
./gradlew jar

java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create record person.xml
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create pojo person.json
java -jar build/libs/jax-0.2.0-SNAPSHOT.jar --create with-constructor person.json
```

Generated mutable classes have `name()` getters and `name(value)` setters (without `get`/`set`
prefixes). POJOs use a no-argument constructor; `with-constructor` also generates an all-fields
constructor. Nested objects become nested Java types, repeated XML elements and JSON arrays become
`List` properties, and XML attributes are marked with `@XmlAttribute`. For XML mapping with
`Jax.toObject`, compile constructor-bound classes with `-parameters`, or use the generated no-arg
constructor and setters. XML samples with attributes cannot be generated as records because record
mapping does not populate per-instance attributes; choose `pojo` or `with-constructor` to preserve
them as instance properties.

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
