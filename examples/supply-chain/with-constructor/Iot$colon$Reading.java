import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Iot$colon$Reading {
    @XmlAttribute("timestamp") String timestamp;
    @XmlAttribute("tempC") java.math.BigDecimal tempC;
    @XmlAttribute("humidity") java.math.BigDecimal humidity;
    @XmlAttribute("shockG") java.math.BigDecimal shockG;
    @XmlAttribute("doorOpen") boolean doorOpen;
    Iot$colon$GPS iot$colon$GPS;

    public Iot$colon$Reading() {
    }

    public Iot$colon$Reading(@XmlAttribute("timestamp") String timestamp, @XmlAttribute("tempC") java.math.BigDecimal tempC, @XmlAttribute("humidity") java.math.BigDecimal humidity, @XmlAttribute("shockG") java.math.BigDecimal shockG, @XmlAttribute("doorOpen") boolean doorOpen, Iot$colon$GPS iot$colon$GPS) {
        this.timestamp = timestamp;
        this.tempC = tempC;
        this.humidity = humidity;
        this.shockG = shockG;
        this.doorOpen = doorOpen;
        this.iot$colon$GPS = iot$colon$GPS;
    }

    public String timestamp() {
        return timestamp;
    }
    @JsonProperty
    public void timestamp(String value) {
        this.timestamp = value;
    }

    public java.math.BigDecimal tempC() {
        return tempC;
    }
    @JsonProperty
    public void tempC(java.math.BigDecimal value) {
        this.tempC = value;
    }

    public java.math.BigDecimal humidity() {
        return humidity;
    }
    @JsonProperty
    public void humidity(java.math.BigDecimal value) {
        this.humidity = value;
    }

    public java.math.BigDecimal shockG() {
        return shockG;
    }
    @JsonProperty
    public void shockG(java.math.BigDecimal value) {
        this.shockG = value;
    }

    public boolean doorOpen() {
        return doorOpen;
    }
    @JsonProperty
    public void doorOpen(boolean value) {
        this.doorOpen = value;
    }

    public Iot$colon$GPS iot$colon$GPS() {
        return iot$colon$GPS;
    }
    @JsonProperty
    public void iot$colon$GPS(Iot$colon$GPS value) {
        this.iot$colon$GPS = value;
    }
}
