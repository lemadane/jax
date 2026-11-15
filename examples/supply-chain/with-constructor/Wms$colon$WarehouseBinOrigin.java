import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$WarehouseBinOrigin {
    @XmlAttribute("zone") String zone;
    @XmlAttribute("aisle") String aisle;
    @XmlAttribute("rack") String rack;
    @XmlAttribute("level") String level;

    public Wms$colon$WarehouseBinOrigin() {
    }

    public Wms$colon$WarehouseBinOrigin(@XmlAttribute("zone") String zone, @XmlAttribute("aisle") String aisle, @XmlAttribute("rack") String rack, @XmlAttribute("level") String level) {
        this.zone = zone;
        this.aisle = aisle;
        this.rack = rack;
        this.level = level;
    }

    public String zone() {
        return zone;
    }
    @JsonProperty
    public void zone(String value) {
        this.zone = value;
    }

    public String aisle() {
        return aisle;
    }
    @JsonProperty
    public void aisle(String value) {
        this.aisle = value;
    }

    public String rack() {
        return rack;
    }
    @JsonProperty
    public void rack(String value) {
        this.rack = value;
    }

    public String level() {
        return level;
    }
    @JsonProperty
    public void level(String value) {
        this.level = value;
    }
}
