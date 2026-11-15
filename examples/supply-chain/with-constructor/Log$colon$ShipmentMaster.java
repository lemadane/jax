import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Log$colon$ShipmentMaster {
    @XmlAttribute("masterBillOfLading") String masterBillOfLading;
    @XmlAttribute("houseBillOfLading") String houseBillOfLading;
    Log$colon$ServiceLevel log$colon$ServiceLevel;
    Log$colon$Parties log$colon$Parties;
    Tms$colon$RoutingItinerary tms$colon$RoutingItinerary;
    Wms$colon$CargoManifest wms$colon$CargoManifest;
    Cus$colon$CustomsSummary cus$colon$CustomsSummary;

    public Log$colon$ShipmentMaster() {
    }

    public Log$colon$ShipmentMaster(@XmlAttribute("masterBillOfLading") String masterBillOfLading, @XmlAttribute("houseBillOfLading") String houseBillOfLading, Log$colon$ServiceLevel log$colon$ServiceLevel, Log$colon$Parties log$colon$Parties, Tms$colon$RoutingItinerary tms$colon$RoutingItinerary, Wms$colon$CargoManifest wms$colon$CargoManifest, Cus$colon$CustomsSummary cus$colon$CustomsSummary) {
        this.masterBillOfLading = masterBillOfLading;
        this.houseBillOfLading = houseBillOfLading;
        this.log$colon$ServiceLevel = log$colon$ServiceLevel;
        this.log$colon$Parties = log$colon$Parties;
        this.tms$colon$RoutingItinerary = tms$colon$RoutingItinerary;
        this.wms$colon$CargoManifest = wms$colon$CargoManifest;
        this.cus$colon$CustomsSummary = cus$colon$CustomsSummary;
    }

    public String masterBillOfLading() {
        return masterBillOfLading;
    }
    @JsonProperty
    public void masterBillOfLading(String value) {
        this.masterBillOfLading = value;
    }

    public String houseBillOfLading() {
        return houseBillOfLading;
    }
    @JsonProperty
    public void houseBillOfLading(String value) {
        this.houseBillOfLading = value;
    }

    public Log$colon$ServiceLevel log$colon$ServiceLevel() {
        return log$colon$ServiceLevel;
    }
    @JsonProperty
    public void log$colon$ServiceLevel(Log$colon$ServiceLevel value) {
        this.log$colon$ServiceLevel = value;
    }

    public Log$colon$Parties log$colon$Parties() {
        return log$colon$Parties;
    }
    @JsonProperty
    public void log$colon$Parties(Log$colon$Parties value) {
        this.log$colon$Parties = value;
    }

    public Tms$colon$RoutingItinerary tms$colon$RoutingItinerary() {
        return tms$colon$RoutingItinerary;
    }
    @JsonProperty
    public void tms$colon$RoutingItinerary(Tms$colon$RoutingItinerary value) {
        this.tms$colon$RoutingItinerary = value;
    }

    public Wms$colon$CargoManifest wms$colon$CargoManifest() {
        return wms$colon$CargoManifest;
    }
    @JsonProperty
    public void wms$colon$CargoManifest(Wms$colon$CargoManifest value) {
        this.wms$colon$CargoManifest = value;
    }

    public Cus$colon$CustomsSummary cus$colon$CustomsSummary() {
        return cus$colon$CustomsSummary;
    }
    @JsonProperty
    public void cus$colon$CustomsSummary(Cus$colon$CustomsSummary value) {
        this.cus$colon$CustomsSummary = value;
    }
}
