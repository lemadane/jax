import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Iot$colon$SensorReadings {
    List<Iot$colon$Reading> iot$colon$Reading;

    public Iot$colon$SensorReadings() {
    }

    public Iot$colon$SensorReadings(List<Iot$colon$Reading> iot$colon$Reading) {
        this.iot$colon$Reading = iot$colon$Reading;
    }

    public List<Iot$colon$Reading> iot$colon$Reading() {
        return iot$colon$Reading;
    }
    @JsonProperty
    public void iot$colon$Reading(List<Iot$colon$Reading> value) {
        this.iot$colon$Reading = value;
    }
}
