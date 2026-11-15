import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Tms$colon$TransportLeg {
    @XmlAttribute("sequence") int sequence;
    @XmlAttribute("mode") String mode;
    @XmlAttribute("status") String status;
    Tms$colon$Carrier tms$colon$Carrier;
    Tms$colon$Equipment tms$colon$Equipment;
    Tms$colon$Origin tms$colon$Origin;
    Tms$colon$Destination tms$colon$Destination;
    Tms$colon$Vessel tms$colon$Vessel;
    Tms$colon$IntermediateStops tms$colon$IntermediateStops;
    String tms$colon$TrainID;

    public Tms$colon$TransportLeg() {
    }

    public Tms$colon$TransportLeg(@XmlAttribute("sequence") int sequence, @XmlAttribute("mode") String mode, @XmlAttribute("status") String status, Tms$colon$Carrier tms$colon$Carrier, Tms$colon$Equipment tms$colon$Equipment, Tms$colon$Origin tms$colon$Origin, Tms$colon$Destination tms$colon$Destination, Tms$colon$Vessel tms$colon$Vessel, Tms$colon$IntermediateStops tms$colon$IntermediateStops, String tms$colon$TrainID) {
        this.sequence = sequence;
        this.mode = mode;
        this.status = status;
        this.tms$colon$Carrier = tms$colon$Carrier;
        this.tms$colon$Equipment = tms$colon$Equipment;
        this.tms$colon$Origin = tms$colon$Origin;
        this.tms$colon$Destination = tms$colon$Destination;
        this.tms$colon$Vessel = tms$colon$Vessel;
        this.tms$colon$IntermediateStops = tms$colon$IntermediateStops;
        this.tms$colon$TrainID = tms$colon$TrainID;
    }

    public int sequence() {
        return sequence;
    }
    @JsonProperty
    public void sequence(int value) {
        this.sequence = value;
    }

    public String mode() {
        return mode;
    }
    @JsonProperty
    public void mode(String value) {
        this.mode = value;
    }

    public String status() {
        return status;
    }
    @JsonProperty
    public void status(String value) {
        this.status = value;
    }

    public Tms$colon$Carrier tms$colon$Carrier() {
        return tms$colon$Carrier;
    }
    @JsonProperty
    public void tms$colon$Carrier(Tms$colon$Carrier value) {
        this.tms$colon$Carrier = value;
    }

    public Tms$colon$Equipment tms$colon$Equipment() {
        return tms$colon$Equipment;
    }
    @JsonProperty
    public void tms$colon$Equipment(Tms$colon$Equipment value) {
        this.tms$colon$Equipment = value;
    }

    public Tms$colon$Origin tms$colon$Origin() {
        return tms$colon$Origin;
    }
    @JsonProperty
    public void tms$colon$Origin(Tms$colon$Origin value) {
        this.tms$colon$Origin = value;
    }

    public Tms$colon$Destination tms$colon$Destination() {
        return tms$colon$Destination;
    }
    @JsonProperty
    public void tms$colon$Destination(Tms$colon$Destination value) {
        this.tms$colon$Destination = value;
    }

    public Tms$colon$Vessel tms$colon$Vessel() {
        return tms$colon$Vessel;
    }
    @JsonProperty
    public void tms$colon$Vessel(Tms$colon$Vessel value) {
        this.tms$colon$Vessel = value;
    }

    public Tms$colon$IntermediateStops tms$colon$IntermediateStops() {
        return tms$colon$IntermediateStops;
    }
    @JsonProperty
    public void tms$colon$IntermediateStops(Tms$colon$IntermediateStops value) {
        this.tms$colon$IntermediateStops = value;
    }

    public String tms$colon$TrainID() {
        return tms$colon$TrainID;
    }
    @JsonProperty
    public void tms$colon$TrainID(String value) {
        this.tms$colon$TrainID = value;
    }
}
