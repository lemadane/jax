import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class log$colon$GlobalSupplyChainManifest {
    @XmlAttribute("xmlns:log") String xmlns$colon$log;
    @XmlAttribute("xmlns:tms") String xmlns$colon$tms;
    @XmlAttribute("xmlns:wms") String xmlns$colon$wms;
    @XmlAttribute("xmlns:cus") String xmlns$colon$cus;
    @XmlAttribute("xmlns:iot") String xmlns$colon$iot;
    @XmlAttribute("xmlns:xsi") String xmlns$colon$xsi;
    @XmlAttribute("xsi:schemaLocation") String xsi$colon$schemaLocation;
    @XmlAttribute("manifestID") String manifestID;
    @XmlAttribute("schemaVersion") String schemaVersion;
    @XmlAttribute("environment") String environment;
    @XmlAttribute("generatedTimestamp") String generatedTimestamp;
    Log$colon$InterchangeHeader log$colon$InterchangeHeader;
    Log$colon$ShipmentMaster log$colon$ShipmentMaster;

    public log$colon$GlobalSupplyChainManifest() {
    }

    public log$colon$GlobalSupplyChainManifest(@XmlAttribute("xmlns:log") String xmlns$colon$log, @XmlAttribute("xmlns:tms") String xmlns$colon$tms, @XmlAttribute("xmlns:wms") String xmlns$colon$wms, @XmlAttribute("xmlns:cus") String xmlns$colon$cus, @XmlAttribute("xmlns:iot") String xmlns$colon$iot, @XmlAttribute("xmlns:xsi") String xmlns$colon$xsi, @XmlAttribute("xsi:schemaLocation") String xsi$colon$schemaLocation, @XmlAttribute("manifestID") String manifestID, @XmlAttribute("schemaVersion") String schemaVersion, @XmlAttribute("environment") String environment, @XmlAttribute("generatedTimestamp") String generatedTimestamp, Log$colon$InterchangeHeader log$colon$InterchangeHeader, Log$colon$ShipmentMaster log$colon$ShipmentMaster) {
        this.xmlns$colon$log = xmlns$colon$log;
        this.xmlns$colon$tms = xmlns$colon$tms;
        this.xmlns$colon$wms = xmlns$colon$wms;
        this.xmlns$colon$cus = xmlns$colon$cus;
        this.xmlns$colon$iot = xmlns$colon$iot;
        this.xmlns$colon$xsi = xmlns$colon$xsi;
        this.xsi$colon$schemaLocation = xsi$colon$schemaLocation;
        this.manifestID = manifestID;
        this.schemaVersion = schemaVersion;
        this.environment = environment;
        this.generatedTimestamp = generatedTimestamp;
        this.log$colon$InterchangeHeader = log$colon$InterchangeHeader;
        this.log$colon$ShipmentMaster = log$colon$ShipmentMaster;
    }

    public String xmlns$colon$log() {
        return xmlns$colon$log;
    }
    @JsonProperty
    public void xmlns$colon$log(String value) {
        this.xmlns$colon$log = value;
    }

    public String xmlns$colon$tms() {
        return xmlns$colon$tms;
    }
    @JsonProperty
    public void xmlns$colon$tms(String value) {
        this.xmlns$colon$tms = value;
    }

    public String xmlns$colon$wms() {
        return xmlns$colon$wms;
    }
    @JsonProperty
    public void xmlns$colon$wms(String value) {
        this.xmlns$colon$wms = value;
    }

    public String xmlns$colon$cus() {
        return xmlns$colon$cus;
    }
    @JsonProperty
    public void xmlns$colon$cus(String value) {
        this.xmlns$colon$cus = value;
    }

    public String xmlns$colon$iot() {
        return xmlns$colon$iot;
    }
    @JsonProperty
    public void xmlns$colon$iot(String value) {
        this.xmlns$colon$iot = value;
    }

    public String xmlns$colon$xsi() {
        return xmlns$colon$xsi;
    }
    @JsonProperty
    public void xmlns$colon$xsi(String value) {
        this.xmlns$colon$xsi = value;
    }

    public String xsi$colon$schemaLocation() {
        return xsi$colon$schemaLocation;
    }
    @JsonProperty
    public void xsi$colon$schemaLocation(String value) {
        this.xsi$colon$schemaLocation = value;
    }

    public String manifestID() {
        return manifestID;
    }
    @JsonProperty
    public void manifestID(String value) {
        this.manifestID = value;
    }

    public String schemaVersion() {
        return schemaVersion;
    }
    @JsonProperty
    public void schemaVersion(String value) {
        this.schemaVersion = value;
    }

    public String environment() {
        return environment;
    }
    @JsonProperty
    public void environment(String value) {
        this.environment = value;
    }

    public String generatedTimestamp() {
        return generatedTimestamp;
    }
    @JsonProperty
    public void generatedTimestamp(String value) {
        this.generatedTimestamp = value;
    }

    public Log$colon$InterchangeHeader log$colon$InterchangeHeader() {
        return log$colon$InterchangeHeader;
    }
    @JsonProperty
    public void log$colon$InterchangeHeader(Log$colon$InterchangeHeader value) {
        this.log$colon$InterchangeHeader = value;
    }

    public Log$colon$ShipmentMaster log$colon$ShipmentMaster() {
        return log$colon$ShipmentMaster;
    }
    @JsonProperty
    public void log$colon$ShipmentMaster(Log$colon$ShipmentMaster value) {
        this.log$colon$ShipmentMaster = value;
    }
}
