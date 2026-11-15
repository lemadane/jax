import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$PortCall {
    @XmlAttribute("sequence") int sequence;
    @XmlAttribute("locationCode") String locationCode;
    @XmlAttribute("operation") String operation;
    String tms$colon$ScheduledArrival;

    public Tms$colon$PortCall() {
    }

    public Tms$colon$PortCall(@XmlAttribute("sequence") int sequence, @XmlAttribute("locationCode") String locationCode, @XmlAttribute("operation") String operation, String tms$colon$ScheduledArrival) {
        this.sequence = sequence;
        this.locationCode = locationCode;
        this.operation = operation;
        this.tms$colon$ScheduledArrival = tms$colon$ScheduledArrival;
    }

    public int sequence() {
        return sequence;
    }
    @JsonProperty
    public void sequence(int value) {
        this.sequence = value;
    }

    public String locationCode() {
        return locationCode;
    }
    @JsonProperty
    public void locationCode(String value) {
        this.locationCode = value;
    }

    public String operation() {
        return operation;
    }
    @JsonProperty
    public void operation(String value) {
        this.operation = value;
    }

    public String tms$colon$ScheduledArrival() {
        return tms$colon$ScheduledArrival;
    }
    @JsonProperty
    public void tms$colon$ScheduledArrival(String value) {
        this.tms$colon$ScheduledArrival = value;
    }
}
