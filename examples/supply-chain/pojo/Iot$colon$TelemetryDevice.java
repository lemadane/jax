import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Iot$colon$TelemetryDevice {
    @XmlAttribute("deviceID") String deviceID;
    @XmlAttribute("batteryPercent") int batteryPercent;
    @XmlAttribute("firmware") String firmware;
    Iot$colon$SensorReadings iot$colon$SensorReadings;

    public String deviceID() {
        return deviceID;
    }
    @JsonProperty
    public void deviceID(String value) {
        this.deviceID = value;
    }

    public int batteryPercent() {
        return batteryPercent;
    }
    @JsonProperty
    public void batteryPercent(int value) {
        this.batteryPercent = value;
    }

    public String firmware() {
        return firmware;
    }
    @JsonProperty
    public void firmware(String value) {
        this.firmware = value;
    }

    public Iot$colon$SensorReadings iot$colon$SensorReadings() {
        return iot$colon$SensorReadings;
    }
    @JsonProperty
    public void iot$colon$SensorReadings(Iot$colon$SensorReadings value) {
        this.iot$colon$SensorReadings = value;
    }
}
