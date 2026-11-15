import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$Equipment {
    @XmlAttribute("equipmentType") String equipmentType;
    @XmlAttribute("licensePlate") String licensePlate;
    @XmlAttribute("vin") String vin;

    public String equipmentType() {
        return equipmentType;
    }
    @JsonProperty
    public void equipmentType(String value) {
        this.equipmentType = value;
    }

    public String licensePlate() {
        return licensePlate;
    }
    @JsonProperty
    public void licensePlate(String value) {
        this.licensePlate = value;
    }

    public String vin() {
        return vin;
    }
    @JsonProperty
    public void vin(String value) {
        this.vin = value;
    }
}
