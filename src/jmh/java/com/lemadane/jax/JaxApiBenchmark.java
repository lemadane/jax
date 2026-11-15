package com.lemadane.jax;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5)
@Fork(1)
public class JaxApiBenchmark {

  public record BenchmarkOrder(
      String order$hyphen$id,
      Customer customer,
      List<Line> lines,
      BigDecimal total,
      boolean expedited
  ) {}

  public record Customer(String name, Address address$dot$info) {}

  public record Address(String city, Geo geo$hyphen$position) {}

  public record Geo(String country, Location location$dot$detail) {}

  public record Location(String postal$hyphen$code, String longitude$dot$value) {}

  public record Line(String sku$hyphen$code, int quantity, BigDecimal unit$dot$price) {}

  public static class fxg$Shipment {
    @XmlAttribute("xmlns:fxg")
    public String fxg$xmlns;

    @XmlAttribute("fxg:shipment-id")
    public String shipment$hyphen$id;

    @XmlAttribute("revision.number")
    public int revision$dot$number;

    public fxg$Customer$hyphen$data fxg$customer$hyphen$data;
    public List<fxg$Line$hyphen$Item> fxg$line$hyphen$item;
    public BigDecimal total;
    public boolean expedited;
  }

  public static class fxg$Customer$hyphen$data {
    public String name;
    public fxg$Address$dot$info fxg$address$dot$info;
  }

  public static class fxg$Address$dot$info {
    public String city;
    public fxg$Geo$hyphen$position fxg$geo$hyphen$position;
  }

  public static class fxg$Geo$hyphen$position {
    public String country;
    public fxg$Location$u00B7$detail fxg$location$u00B7$detail;
  }

  public static class fxg$Location$u00B7$detail {
    public String fxg$postal$hyphen$code;
    public String fxg$longitude$dot$value;
  }

  public static class fxg$Line$hyphen$Item {
    public String fxg$sku$hyphen$code;
    public int quantity;
    public BigDecimal fxg$price$dot$amount;
  }

  String strictJson;
  String partialJson;
  String strictXml;
  String partialXml;
  BenchmarkOrder order;
  fxg$Shipment shipment;

  @Setup
  public void setup() {
    strictJson = """
        {
          "order-id":"ORD-42",
          "customer":{
          "name":"Ada Lovelace",
          "address.info":{
            "city":"London",
            "geo-position":{
            "country":"UK",
            "location.detail":{"postal-code":"N1 9GU","longitude.value":"-0.1276"}
            }
          }
          },
          "lines":[
          {"sku-code":"A-1","quantity":2,"unit.price":12.50},
          {"sku-code":"B-2","quantity":3,"unit.price":33.00}
          ],
          "total":124.00,
          "expedited":true
        }
        """;
    partialJson = """
        {
          "order-id":"ORD-42",
          "customer":{
          "name":"Ada Lovelace",
          "address.info":{
            "city":"London",
            "geo-position":{
            "country":"UK",
            "location.detail":{"postal-code":"N1 9GU","longitude.value":"-0.1276","ignored":"value"}
            },
            "ignored":"value"
          }
          },
          "lines":[
          {"sku-code":"A-1","quantity":2,"unit.price":12.50,"ignored":true},
          {"sku-code":"B-2","quantity":3,"unit.price":33.00}
          ],
          "total":124.00,
          "expedited":true,
          "ignored":"value"
        }
        """;
    strictXml = """
        <fxg:Shipment xmlns:fxg="urn:jax:benchmark" fxg:shipment-id="S-42" revision.number="7">
          <fxg:customer-data>
          <name>Ada Lovelace</name>
          <fxg:address.info>
            <city>London</city>
            <fxg:geo-position>
            <country>UK</country>
            <fxg:location·detail><fxg:postal-code>N1 9GU</fxg:postal-code><fxg:longitude.value>-0.1276</fxg:longitude.value></fxg:location·detail>
            </fxg:geo-position>
          </fxg:address.info>
          </fxg:customer-data>
          <fxg:line-item><fxg:sku-code>A-1</fxg:sku-code><quantity>2</quantity><fxg:price.amount>12.50</fxg:price.amount></fxg:line-item>
          <fxg:line-item><fxg:sku-code>B-2</fxg:sku-code><quantity>3</quantity><fxg:price.amount>33.00</fxg:price.amount></fxg:line-item>
          <total>124.00</total>
          <expedited>true</expedited>
        </fxg:Shipment>
        """;
    partialXml = """
        <fxg:Shipment xmlns:fxg="urn:jax:benchmark" fxg:shipment-id="S-42" revision.number="7">
          <fxg:customer-data>
          <name>Ada Lovelace</name>
          <fxg:address.info>
            <city>London</city>
            <fxg:geo-position>
            <country>UK</country>
            <fxg:location·detail><fxg:postal-code>N1 9GU</fxg:postal-code><fxg:longitude.value>-0.1276</fxg:longitude.value><ignored>value</ignored></fxg:location·detail>
            </fxg:geo-position>
            <ignored>value</ignored>
          </fxg:address.info>
          </fxg:customer-data>
          <fxg:line-item><fxg:sku-code>A-1</fxg:sku-code><quantity>2</quantity><fxg:price.amount>12.50</fxg:price.amount><ignored>true</ignored></fxg:line-item>
          <fxg:line-item><fxg:sku-code>B-2</fxg:sku-code><quantity>3</quantity><fxg:price.amount>33.00</fxg:price.amount></fxg:line-item>
          <total>124.00</total>
          <expedited>true</expedited>
          <ignored>value</ignored>
        </fxg:Shipment>
        """;

    Location location = new Location("N1 9GU", "-0.1276");
    Geo geo = new Geo("UK", location);
    Address address = new Address("London", geo);
    Customer customer = new Customer("Ada Lovelace", address);
    order = new BenchmarkOrder(
        "ORD-42",
        customer,
        List.of(
            new Line("A-1", 2, new BigDecimal("12.50")),
            new Line("B-2", 3, new BigDecimal("33.00"))
        ),
        new BigDecimal("124.00"),
        true
    );

    shipment = Jax.toObject(strictXml, fxg$Shipment.class);
  }

  @Benchmark
  public BenchmarkOrder toObjectJsonRoundTrip() {
    BenchmarkOrder mapped = Jax.toObject(strictJson, BenchmarkOrder.class);
    return Jax.toObject(Jax.toJson(mapped), BenchmarkOrder.class);
  }

  @Benchmark
  public fxg$Shipment toObjectXmlRoundTrip() {
    fxg$Shipment mapped = Jax.toObject(strictXml, fxg$Shipment.class);
    return Jax.toObject(Jax.toXml(mapped), fxg$Shipment.class);
  }

  @Benchmark
  public BenchmarkOrder toPartialObjectJsonRoundTrip() {
    BenchmarkOrder mapped = Jax.toPartialObject(partialJson, BenchmarkOrder.class);
    return Jax.toPartialObject(Jax.toJson(mapped), BenchmarkOrder.class);
  }

  @Benchmark
  public fxg$Shipment toPartialObjectXmlRoundTrip() {
    fxg$Shipment mapped = Jax.toPartialObject(partialXml, fxg$Shipment.class);
    return Jax.toPartialObject(Jax.toXml(mapped), fxg$Shipment.class);
  }

  @Benchmark
  public Document toDocumentJsonRoundTrip() {
    BenchmarkOrder mapped = Jax.toObject(strictJson, BenchmarkOrder.class);
    return Jax.toDocument(Jax.toJson(mapped));
  }

  @Benchmark
  public BenchmarkOrder toJsonRecordRoundTrip() {
    return Jax.toObject(Jax.toJson(order), BenchmarkOrder.class);
  }

  @Benchmark
  public Document toJsonPojoRoundTrip() {
    return Jax.toDocument(Jax.toJson(shipment));
  }

  @Benchmark
  public BenchmarkOrder toXmlRecordRoundTrip() {
    String xml = Jax.toXml(order);
    return Jax.toObject(xml, BenchmarkOrder.class);
  }

  @Benchmark
  public fxg$Shipment toXmlPojoRoundTrip() {
    return Jax.toObject(Jax.toXml(shipment), fxg$Shipment.class);
  }
}
