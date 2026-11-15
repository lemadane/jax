import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Wms$colon$Container {
    @XmlAttribute("containerNumber") String containerNumber;
    @XmlAttribute("isoSizeType") String isoSizeType;
    @XmlAttribute("description") String description;
    Wms$colon$Seals wms$colon$Seals;
    Wms$colon$WeightsAndMeasures wms$colon$WeightsAndMeasures;
    Wms$colon$ReeferSettings wms$colon$ReeferSettings;
    Iot$colon$TelemetryDevice iot$colon$TelemetryDevice;
    Wms$colon$HandlingUnits wms$colon$HandlingUnits;

    public String containerNumber() {
        return containerNumber;
    }
    @JsonProperty
    public void containerNumber(String value) {
        this.containerNumber = value;
    }

    public String isoSizeType() {
        return isoSizeType;
    }
    @JsonProperty
    public void isoSizeType(String value) {
        this.isoSizeType = value;
    }

    public String description() {
        return description;
    }
    @JsonProperty
    public void description(String value) {
        this.description = value;
    }

    public Wms$colon$Seals wms$colon$Seals() {
        return wms$colon$Seals;
    }
    @JsonProperty
    public void wms$colon$Seals(Wms$colon$Seals value) {
        this.wms$colon$Seals = value;
    }

    public Wms$colon$WeightsAndMeasures wms$colon$WeightsAndMeasures() {
        return wms$colon$WeightsAndMeasures;
    }
    @JsonProperty
    public void wms$colon$WeightsAndMeasures(Wms$colon$WeightsAndMeasures value) {
        this.wms$colon$WeightsAndMeasures = value;
    }

    public Wms$colon$ReeferSettings wms$colon$ReeferSettings() {
        return wms$colon$ReeferSettings;
    }
    @JsonProperty
    public void wms$colon$ReeferSettings(Wms$colon$ReeferSettings value) {
        this.wms$colon$ReeferSettings = value;
    }

    public Iot$colon$TelemetryDevice iot$colon$TelemetryDevice() {
        return iot$colon$TelemetryDevice;
    }
    @JsonProperty
    public void iot$colon$TelemetryDevice(Iot$colon$TelemetryDevice value) {
        this.iot$colon$TelemetryDevice = value;
    }

    public Wms$colon$HandlingUnits wms$colon$HandlingUnits() {
        return wms$colon$HandlingUnits;
    }
    @JsonProperty
    public void wms$colon$HandlingUnits(Wms$colon$HandlingUnits value) {
        this.wms$colon$HandlingUnits = value;
    }
}
