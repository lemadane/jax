import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Wms$colon$CargoManifest {
    @XmlAttribute("totalContainers") int totalContainers;
    @XmlAttribute("totalPallets") int totalPallets;
    @XmlAttribute("totalGrossWeightKg") java.math.BigDecimal totalGrossWeightKg;
    @XmlAttribute("totalVolumeCbm") java.math.BigDecimal totalVolumeCbm;
    Wms$colon$Container wms$colon$Container;

    public int totalContainers() {
        return totalContainers;
    }
    @JsonProperty
    public void totalContainers(int value) {
        this.totalContainers = value;
    }

    public int totalPallets() {
        return totalPallets;
    }
    @JsonProperty
    public void totalPallets(int value) {
        this.totalPallets = value;
    }

    public java.math.BigDecimal totalGrossWeightKg() {
        return totalGrossWeightKg;
    }
    @JsonProperty
    public void totalGrossWeightKg(java.math.BigDecimal value) {
        this.totalGrossWeightKg = value;
    }

    public java.math.BigDecimal totalVolumeCbm() {
        return totalVolumeCbm;
    }
    @JsonProperty
    public void totalVolumeCbm(java.math.BigDecimal value) {
        this.totalVolumeCbm = value;
    }

    public Wms$colon$Container wms$colon$Container() {
        return wms$colon$Container;
    }
    @JsonProperty
    public void wms$colon$Container(Wms$colon$Container value) {
        this.wms$colon$Container = value;
    }
}
