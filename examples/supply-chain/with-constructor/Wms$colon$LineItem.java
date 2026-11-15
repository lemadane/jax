import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$LineItem {
    @XmlAttribute("lineNumber") int lineNumber;
    @XmlAttribute("sku") String sku;
    @XmlAttribute("batchNumber") String batchNumber;
    @XmlAttribute("expirationDate") String expirationDate;
    String wms$colon$Description;
    Wms$colon$Quantity wms$colon$Quantity;
    Wms$colon$SerialNumbers wms$colon$SerialNumbers;
    Cus$colon$CustomsValuation cus$colon$CustomsValuation;
    Wms$colon$DangerousGoods wms$colon$DangerousGoods;

    public Wms$colon$LineItem() {
    }

    public Wms$colon$LineItem(@XmlAttribute("lineNumber") int lineNumber, @XmlAttribute("sku") String sku, @XmlAttribute("batchNumber") String batchNumber, @XmlAttribute("expirationDate") String expirationDate, String wms$colon$Description, Wms$colon$Quantity wms$colon$Quantity, Wms$colon$SerialNumbers wms$colon$SerialNumbers, Cus$colon$CustomsValuation cus$colon$CustomsValuation, Wms$colon$DangerousGoods wms$colon$DangerousGoods) {
        this.lineNumber = lineNumber;
        this.sku = sku;
        this.batchNumber = batchNumber;
        this.expirationDate = expirationDate;
        this.wms$colon$Description = wms$colon$Description;
        this.wms$colon$Quantity = wms$colon$Quantity;
        this.wms$colon$SerialNumbers = wms$colon$SerialNumbers;
        this.cus$colon$CustomsValuation = cus$colon$CustomsValuation;
        this.wms$colon$DangerousGoods = wms$colon$DangerousGoods;
    }

    public int lineNumber() {
        return lineNumber;
    }
    @JsonProperty
    public void lineNumber(int value) {
        this.lineNumber = value;
    }

    public String sku() {
        return sku;
    }
    @JsonProperty
    public void sku(String value) {
        this.sku = value;
    }

    public String batchNumber() {
        return batchNumber;
    }
    @JsonProperty
    public void batchNumber(String value) {
        this.batchNumber = value;
    }

    public String expirationDate() {
        return expirationDate;
    }
    @JsonProperty
    public void expirationDate(String value) {
        this.expirationDate = value;
    }

    public String wms$colon$Description() {
        return wms$colon$Description;
    }
    @JsonProperty
    public void wms$colon$Description(String value) {
        this.wms$colon$Description = value;
    }

    public Wms$colon$Quantity wms$colon$Quantity() {
        return wms$colon$Quantity;
    }
    @JsonProperty
    public void wms$colon$Quantity(Wms$colon$Quantity value) {
        this.wms$colon$Quantity = value;
    }

    public Wms$colon$SerialNumbers wms$colon$SerialNumbers() {
        return wms$colon$SerialNumbers;
    }
    @JsonProperty
    public void wms$colon$SerialNumbers(Wms$colon$SerialNumbers value) {
        this.wms$colon$SerialNumbers = value;
    }

    public Cus$colon$CustomsValuation cus$colon$CustomsValuation() {
        return cus$colon$CustomsValuation;
    }
    @JsonProperty
    public void cus$colon$CustomsValuation(Cus$colon$CustomsValuation value) {
        this.cus$colon$CustomsValuation = value;
    }

    public Wms$colon$DangerousGoods wms$colon$DangerousGoods() {
        return wms$colon$DangerousGoods;
    }
    @JsonProperty
    public void wms$colon$DangerousGoods(Wms$colon$DangerousGoods value) {
        this.wms$colon$DangerousGoods = value;
    }
}
