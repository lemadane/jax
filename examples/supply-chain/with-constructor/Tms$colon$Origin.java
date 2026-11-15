import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$Origin {
    @XmlAttribute("locationCode") String locationCode;
    @XmlAttribute("facilityType") String facilityType;
    String tms$colon$ActualDeparture;
    String tms$colon$EstimatedDeparture;

    public Tms$colon$Origin() {
    }

    public Tms$colon$Origin(@XmlAttribute("locationCode") String locationCode, @XmlAttribute("facilityType") String facilityType, String tms$colon$ActualDeparture, String tms$colon$EstimatedDeparture) {
        this.locationCode = locationCode;
        this.facilityType = facilityType;
        this.tms$colon$ActualDeparture = tms$colon$ActualDeparture;
        this.tms$colon$EstimatedDeparture = tms$colon$EstimatedDeparture;
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

    public String tms$colon$ActualDeparture() {
        return tms$colon$ActualDeparture;
    }
    @JsonProperty
    public void tms$colon$ActualDeparture(String value) {
        this.tms$colon$ActualDeparture = value;
    }

    public String tms$colon$EstimatedDeparture() {
        return tms$colon$EstimatedDeparture;
    }
    @JsonProperty
    public void tms$colon$EstimatedDeparture(String value) {
        this.tms$colon$EstimatedDeparture = value;
    }
}
