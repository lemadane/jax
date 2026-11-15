import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Wms$colon$HandlingUnits {
    List<Wms$colon$HandlingUnit> wms$colon$HandlingUnit;

    public Wms$colon$HandlingUnits() {
    }

    public Wms$colon$HandlingUnits(List<Wms$colon$HandlingUnit> wms$colon$HandlingUnit) {
        this.wms$colon$HandlingUnit = wms$colon$HandlingUnit;
    }

    public List<Wms$colon$HandlingUnit> wms$colon$HandlingUnit() {
        return wms$colon$HandlingUnit;
    }
    @JsonProperty
    public void wms$colon$HandlingUnit(List<Wms$colon$HandlingUnit> value) {
        this.wms$colon$HandlingUnit = value;
    }
}
