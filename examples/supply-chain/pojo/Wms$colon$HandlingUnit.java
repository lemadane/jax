import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$HandlingUnit {
    @XmlAttribute("sscc") String sscc;
    @XmlAttribute("packagingType") String packagingType;
    @XmlAttribute("stackable") boolean stackable;
    Wms$colon$Dimensions wms$colon$Dimensions;
    Wms$colon$GrossWeight2 wms$colon$GrossWeight;
    Wms$colon$WarehouseBinOrigin wms$colon$WarehouseBinOrigin;
    Wms$colon$LineItems wms$colon$LineItems;

    public String sscc() {
        return sscc;
    }
    @JsonProperty
    public void sscc(String value) {
        this.sscc = value;
    }

    public String packagingType() {
        return packagingType;
    }
    @JsonProperty
    public void packagingType(String value) {
        this.packagingType = value;
    }

    public boolean stackable() {
        return stackable;
    }
    @JsonProperty
    public void stackable(boolean value) {
        this.stackable = value;
    }

    public Wms$colon$Dimensions wms$colon$Dimensions() {
        return wms$colon$Dimensions;
    }
    @JsonProperty
    public void wms$colon$Dimensions(Wms$colon$Dimensions value) {
        this.wms$colon$Dimensions = value;
    }

    public Wms$colon$GrossWeight2 wms$colon$GrossWeight() {
        return wms$colon$GrossWeight;
    }
    @JsonProperty
    public void wms$colon$GrossWeight(Wms$colon$GrossWeight2 value) {
        this.wms$colon$GrossWeight = value;
    }

    public Wms$colon$WarehouseBinOrigin wms$colon$WarehouseBinOrigin() {
        return wms$colon$WarehouseBinOrigin;
    }
    @JsonProperty
    public void wms$colon$WarehouseBinOrigin(Wms$colon$WarehouseBinOrigin value) {
        this.wms$colon$WarehouseBinOrigin = value;
    }

    public Wms$colon$LineItems wms$colon$LineItems() {
        return wms$colon$LineItems;
    }
    @JsonProperty
    public void wms$colon$LineItems(Wms$colon$LineItems value) {
        this.wms$colon$LineItems = value;
    }
}
