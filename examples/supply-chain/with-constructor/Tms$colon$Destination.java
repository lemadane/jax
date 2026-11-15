import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$Destination {
    @XmlAttribute("locationCode") String locationCode;
    @XmlAttribute("facilityType") String facilityType;
    String tms$colon$TerminalName;
    String tms$colon$ActualArrival;
    String tms$colon$EstimatedArrival;

    public Tms$colon$Destination() {
    }

    public Tms$colon$Destination(@XmlAttribute("locationCode") String locationCode, @XmlAttribute("facilityType") String facilityType, String tms$colon$TerminalName, String tms$colon$ActualArrival, String tms$colon$EstimatedArrival) {
        this.locationCode = locationCode;
        this.facilityType = facilityType;
        this.tms$colon$TerminalName = tms$colon$TerminalName;
        this.tms$colon$ActualArrival = tms$colon$ActualArrival;
        this.tms$colon$EstimatedArrival = tms$colon$EstimatedArrival;
    }

    public String locationCode() {
        return locationCode;
    }
    @JsonProperty
    public void locationCode(String value) {
        this.locationCode = value;
    }

    public String facilityType() {
        return facilityType;
    }
    @JsonProperty
    public void facilityType(String value) {
        this.facilityType = value;
    }

    public String tms$colon$TerminalName() {
        return tms$colon$TerminalName;
    }
    @JsonProperty
    public void tms$colon$TerminalName(String value) {
        this.tms$colon$TerminalName = value;
    }

    public String tms$colon$ActualArrival() {
        return tms$colon$ActualArrival;
    }
    @JsonProperty
    public void tms$colon$ActualArrival(String value) {
        this.tms$colon$ActualArrival = value;
    }

    public String tms$colon$EstimatedArrival() {
        return tms$colon$EstimatedArrival;
    }
    @JsonProperty
    public void tms$colon$EstimatedArrival(String value) {
        this.tms$colon$EstimatedArrival = value;
    }
}
