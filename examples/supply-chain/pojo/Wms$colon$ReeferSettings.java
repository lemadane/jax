import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$ReeferSettings {
    @XmlAttribute("active") boolean active;
    Wms$colon$SetPointTemperature wms$colon$SetPointTemperature;
    Wms$colon$MinAllowedTemp wms$colon$MinAllowedTemp;
    Wms$colon$MaxAllowedTemp wms$colon$MaxAllowedTemp;
    int wms$colon$RelativeHumidityPercent;
    int wms$colon$VentilationCmh;

    public boolean active() {
        return active;
    }
    @JsonProperty
    public void active(boolean value) {
        this.active = value;
    }

    public Wms$colon$SetPointTemperature wms$colon$SetPointTemperature() {
        return wms$colon$SetPointTemperature;
    }
    @JsonProperty
    public void wms$colon$SetPointTemperature(Wms$colon$SetPointTemperature value) {
        this.wms$colon$SetPointTemperature = value;
    }

    public Wms$colon$MinAllowedTemp wms$colon$MinAllowedTemp() {
        return wms$colon$MinAllowedTemp;
    }
    @JsonProperty
    public void wms$colon$MinAllowedTemp(Wms$colon$MinAllowedTemp value) {
        this.wms$colon$MinAllowedTemp = value;
    }

    public Wms$colon$MaxAllowedTemp wms$colon$MaxAllowedTemp() {
        return wms$colon$MaxAllowedTemp;
    }
    @JsonProperty
    public void wms$colon$MaxAllowedTemp(Wms$colon$MaxAllowedTemp value) {
        this.wms$colon$MaxAllowedTemp = value;
    }

    public int wms$colon$RelativeHumidityPercent() {
        return wms$colon$RelativeHumidityPercent;
    }
    @JsonProperty
    public void wms$colon$RelativeHumidityPercent(int value) {
        this.wms$colon$RelativeHumidityPercent = value;
    }

    public int wms$colon$VentilationCmh() {
        return wms$colon$VentilationCmh;
    }
    @JsonProperty
    public void wms$colon$VentilationCmh(int value) {
        this.wms$colon$VentilationCmh = value;
    }
}
