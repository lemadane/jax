import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Tms$colon$IntermediateStops {
    List<Tms$colon$PortCall> tms$colon$PortCall;

    public Tms$colon$IntermediateStops() {
    }

    public Tms$colon$IntermediateStops(List<Tms$colon$PortCall> tms$colon$PortCall) {
        this.tms$colon$PortCall = tms$colon$PortCall;
    }

    public List<Tms$colon$PortCall> tms$colon$PortCall() {
        return tms$colon$PortCall;
    }
    @JsonProperty
    public void tms$colon$PortCall(List<Tms$colon$PortCall> value) {
        this.tms$colon$PortCall = value;
    }
}
