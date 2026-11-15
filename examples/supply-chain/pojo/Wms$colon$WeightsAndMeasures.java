import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$WeightsAndMeasures {
    Wms$colon$TareWeight wms$colon$TareWeight;
    Wms$colon$PayloadWeight wms$colon$PayloadWeight;
    Wms$colon$GrossWeight wms$colon$GrossWeight;
    Wms$colon$VGM wms$colon$VGM;

    public Wms$colon$TareWeight wms$colon$TareWeight() {
        return wms$colon$TareWeight;
    }
    @JsonProperty
    public void wms$colon$TareWeight(Wms$colon$TareWeight value) {
        this.wms$colon$TareWeight = value;
    }

    public Wms$colon$PayloadWeight wms$colon$PayloadWeight() {
        return wms$colon$PayloadWeight;
    }
    @JsonProperty
    public void wms$colon$PayloadWeight(Wms$colon$PayloadWeight value) {
        this.wms$colon$PayloadWeight = value;
    }

    public Wms$colon$GrossWeight wms$colon$GrossWeight() {
        return wms$colon$GrossWeight;
    }
    @JsonProperty
    public void wms$colon$GrossWeight(Wms$colon$GrossWeight value) {
        this.wms$colon$GrossWeight = value;
    }

    public Wms$colon$VGM wms$colon$VGM() {
        return wms$colon$VGM;
    }
    @JsonProperty
    public void wms$colon$VGM(Wms$colon$VGM value) {
        this.wms$colon$VGM = value;
    }
}
