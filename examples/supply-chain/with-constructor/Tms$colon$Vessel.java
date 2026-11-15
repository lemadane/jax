import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$Vessel {
    @XmlAttribute("imoNumber") int imoNumber;
    @XmlAttribute("mmsi") int mmsi;
    @XmlAttribute("flag") String flag;
    String tms$colon$VesselName;
    String tms$colon$VoyageNumber;

    public Tms$colon$Vessel() {
    }

    public Tms$colon$Vessel(@XmlAttribute("imoNumber") int imoNumber, @XmlAttribute("mmsi") int mmsi, @XmlAttribute("flag") String flag, String tms$colon$VesselName, String tms$colon$VoyageNumber) {
        this.imoNumber = imoNumber;
        this.mmsi = mmsi;
        this.flag = flag;
        this.tms$colon$VesselName = tms$colon$VesselName;
        this.tms$colon$VoyageNumber = tms$colon$VoyageNumber;
    }

    public int imoNumber() {
        return imoNumber;
    }
    @JsonProperty
    public void imoNumber(int value) {
        this.imoNumber = value;
    }

    public int mmsi() {
        return mmsi;
    }
    @JsonProperty
    public void mmsi(int value) {
        this.mmsi = value;
    }

    public String flag() {
        return flag;
    }
    @JsonProperty
    public void flag(String value) {
        this.flag = value;
    }

    public String tms$colon$VesselName() {
        return tms$colon$VesselName;
    }
    @JsonProperty
    public void tms$colon$VesselName(String value) {
        this.tms$colon$VesselName = value;
    }

    public String tms$colon$VoyageNumber() {
        return tms$colon$VoyageNumber;
    }
    @JsonProperty
    public void tms$colon$VoyageNumber(String value) {
        this.tms$colon$VoyageNumber = value;
    }
}
