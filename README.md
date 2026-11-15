# JAX — JSON-And-XML-To-Record

Production-ready, zero-dependency standard Java 17+ library for bi-directional mapping between JSON and XML documents and strongly typed Java records, plus mapping JSON documents to mutable Java classes.

Built purely on Java reflection and standard APIs, `Jax` requires no third-party JSON libraries and works seamlessly across Spring Boot, Quarkus, Micronaut, and standalone JVM applications.

JSON and XML input are parsed into Jax-owned abstract syntax trees before mapping. Serialization follows the reverse path: Java values become JSON or XML nodes, which are then emitted as text. XML parsing uses JDK StAX and Jax-owned nodes rather than W3C DOM.

---

## Key Requirements & Coordinates

* **Java Version**: Minimum required JVM version is **Java 17**.
* **Maven Coordinates**: `com.lemadane:jax:0.1.0-SNAPSHOT`
* **Base Package**: `com.lemadane.jax`

```java
MyRecord record = Jax.toObject(json, MyRecord.class);
// converts JSON to a Java Record.

MyRecord record = Jax.toObject(xml, MyRecord.class);
// converts XML to a Java Record.

String json = Jax.toJson(anObject);
// converts a POJO or Java Record to JSON.

String xml = Jax.toXml(anObject);
// converts a POJO or Java Record to XML.

Record record = Jax.toPartialObject(json, Record.class);
// JSON data maps to declared Java Record components, ignoring undeclared properties.

Record record = Jax.toPartialObject(xml, Record.class);
// XML data maps to declared Java Record components, ignoring undeclared elements.

```

---

## Installation

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.lemadane:jax:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>com.lemadane</groupId>
    <artifactId>jax</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## Public API

The library provides a thread-safe final utility class:

### `com.lemadane.jax.Jax`

```java
public final class Jax {
    public static <T> T toObject(String json, Class<T> targetType);
    public static <T> T toPartialObject(String input, Class<T> targetType);
    public static Document toDocument(String json);
    public static String toJson(Object object);
    public static String toXml(Object object);
}
```

* Public methods reject `null` arguments with a clear `NullPointerException`.
* `toObject(input, targetType)` detects JSON objects (starting with `{`) and XML documents (starting with `<`). For records, it uses strict mapping. For mutable classes, it first looks for a unique constructor whose retained parameter names match input properties/elements. Constructor-bound classes must be compiled with `-parameters`; if no constructor matches, a no-argument constructor is required. Remaining JSON properties and XML elements map through public one-argument setters annotated with `@JsonProperty`, named either after the property (`name(String value)`) or with JavaBean naming (`setName(String value)`). This opt-in avoids invoking unrelated business methods during deserialization. Public instance fields remain supported for XML mapping. Nested POJOs, records, and list/array values are supported. Unmapped JSON properties or XML elements raise mapping exceptions.
* `toPartialObject(input, targetType)` provides partial mapping for either records or mutable classes. It ignores undeclared properties/elements recursively. Missing record components use partial-mode defaults; missing mutable-class properties retain constructor or Java defaults. Present but invalid values still fail mapping.

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
        fxg$Person.class);
```

`toDocument(json)` returns Jax's own insertion-ordered `Document`, a map from JSON property names to inferred Java values. Nested objects become `Document` instances, arrays become lists, and JSON primitives become `String`, `Boolean`, `Integer`, `Long`, `BigDecimal`, or `null`. Integral numbers infer to `Integer`, then `Long`, and otherwise `BigDecimal`; decimal or exponent-form numbers infer to `BigDecimal`.

```java
Document document = Jax.toDocument("{\"name\":\"Ada\",\"age\":37,\"address\":{\"city\":\"London\"}}");
String name = (String) document.get("name");
int age = (Integer) document.get("age");
Document address = (Document) document.get("address");
String city = (String) address.get("city");
```

`toJson()` and `toXml()` serialize either a Java record or a POJO. Record components are read through their accessors; POJO properties are read from public fields or public no-argument accessors. Private fields without a public accessor and static fields are ignored. Nested records and POJOs are serialized recursively. Collections and arrays are written as JSON arrays; for XML, each collection or array item is written as a repeated child element.

JSON and XML parsing enforce a maximum nesting depth of 256. Numeric conversions from JSON numbers, JSON strings, XML text, and XML attributes are limited to 1,024 input characters and exponent magnitude 10,000, before constructing or expanding large integer values. Conversions to `float` and `double` reject non-finite results. XML serialization rejects characters not permitted by XML 1.0 and uses character references for tabs and line breaks in attributes.

XML collection serialization uses the reserved attributes `jax-empty-list="true"` for an empty collection and `jax-null="true"` for a null collection item. The XML mapper recognizes these markers when reading collections and arrays, allowing strict round trips to preserve empty collections and null list entries.

## Performance Benchmarks

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

---

## Strict vs. Partial Parsing

### Strict Mode (`Jax.toObject(input, recordType)`)
Enforces strict 1-to-1 structural completeness:
* Throws an exception if a record component has no matching XML element or JSON property (`MissingDataException`).
* Throws an exception if an undeclared XML element or JSON property is encountered (`ExcessDataException`).
* Validates root XML element name against the target simple record name.
* Rejects duplicate XML child elements for non-List components.
* Rejects duplicate JSON object keys (`JsonMappingException`).
* Rejects JSON documents nested more than 256 object/array levels deep with `JsonMappingException`.

### Partial Mode (`Jax.toPartialObject(input, targetType)`)
Selective mapping into declared record components or mutable-class properties:
* Maps XML elements and JSON properties matching declared components, `@JsonProperty`-annotated setters, or supported public fields.
* Ignores undeclared elements and properties, including in nested objects.
* Missing components default to standard defaults:
  * Reference types -> `null`
  * `Optional<T>` -> `Optional.empty()`
  * `List<T>` -> `List.of()`
  * `boolean` -> `false`
  * `byte`, `short`, `int`, `long` -> `0` / `0L`
  * `float`, `double` -> `0.0f` / `0.0d`
  * `char` -> `'\0'`
* Present but malformed values still throw a `TypeConversionException`.
* Integer conversions reject fractional values and values outside the target type's range.

---

## XML Attribute Mapping

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

### Command-line source generation

The executable JAR can generate a Java record, mutable POJO, or mutable POJO with an all-fields
constructor from a JSON or XML sample. The generated source is written beside the input file, named
from the input filename, and has no package declaration so you can move it to your chosen package.
It refuses to overwrite an existing Java source file.

```shell
java -jar jax-0.1.0-SNAPSHOT.jar --create record person.xml
java -jar jax-0.1.0-SNAPSHOT.jar --create pojo person.json
java -jar jax-0.1.0-SNAPSHOT.jar --create with-constructor person.json
```

Generated mutable classes have `name()` getters and `name(value)` setters (without `get`/`set`
prefixes). POJOs use a no-argument constructor; `with-constructor` also generates an all-fields
constructor. Nested objects become nested Java types, repeated XML elements and JSON arrays become
`List` properties, and XML attributes are marked with `@XmlAttribute`. For XML mapping with
`Jax.toObject`, compile constructor-bound classes with `-parameters`, or use the generated no-arg
constructor and setters. XML samples with attributes cannot be generated as records because record
mapping does not populate per-instance attributes; choose `pojo` or `with-constructor` to preserve
them as instance properties.

* Unmatched XML attributes are ignored and do not trigger excess data errors during strict parsing.

---

## Reversible Special-Character Encoding (`DataNameCodec`)

XML element/attribute names and JSON keys containing characters problematic in Java identifiers are reversibly escaped using `$` as the marker:

| External Character | Reversible Identifier Escape | Example External Name | Encoded Java Component / Type |
| :--- | :--- | :--- | :--- |
| `:` | `$colon$` | `fxg:FacilityInboundEvent` | `fxg$colon$FacilityInboundEvent` |
| `-` | `$hyphen$` | `Facility-ID` | `Facility$hyphen$ID` |
| `.` | `$dot$` | `Facility.ID` | `Facility$dot$ID` |
| `space` | `$space$` | `display name` | `display$space$name` |
| `$` | `$dollar$` | `price$value` | `price$dollar$value` |
| Other | `$uXXXX$` | `emoji_😀` | `emoji_$u1F600$` |

---

## Complete Runnable Examples

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
public record rest$RestaurantOrderEvent(
        rest$KitchenOrderScan rest$KitchenOrderScan
) {}

public record rest$KitchenOrderScan(
        String rest$RestaurantID
) {}
```

**Usage:**

```java
final var event = Jax.toObject(xml, rest$RestaurantOrderEvent.class);
final String restaurantID = event.rest$KitchenOrderScan().rest$RestaurantID();
```

Prefixed XML input maps to the `$`-encoded record component names. Record serialization does not
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

---

## Supported Java Component Types

* `String` (preserves leading zeros and identifier strings)
* Primitives & Wrappers (`boolean`/`Boolean`, `byte`/`Byte`, `short`/`Short`, `int`/`Integer`, `long`/`Long`, `float`/`Float`, `double`/`Double`, `char`/`Character`)
* `BigInteger` & `BigDecimal` (exact numeric precision preserved)
* `Optional<T>`
* `List<T>` (collections of records or primitives)
* Nested records
* Enums (`Enum.name()`)

---

## XML Parsing Security Policy

XML is streamed with the JDK StAX API into Jax-owned internal `XmlElement` nodes; W3C DOM is not used.
The parser disables DTD processing and external entities, and rejects `DOCTYPE` declarations.

---

## Error Model

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

---

## Build & Test Commands

To build the library, run tests, checkstyle static analysis, and verify JaCoCo coverage (>= 90%):

```bash
./gradlew clean test
./gradlew jacocoTestReport
./gradlew build
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](file:///home/lem/Projects/java/json-xml-utility/LICENSE) file for details.
