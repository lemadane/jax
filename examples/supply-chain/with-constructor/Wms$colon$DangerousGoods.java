import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$DangerousGoods {
    @XmlAttribute("regulated") boolean regulated;
    @XmlAttribute("regulationCode") String regulationCode;
    String wms$colon$UNNumber;
    String wms$colon$ProperShippingName;
    int wms$colon$HazardClass;
    String wms$colon$PackingGroup;
    Wms$colon$NetExplosiveOrLithiumWeight wms$colon$NetExplosiveOrLithiumWeight;
    String wms$colon$WattHourRating;
    Wms$colon$EmergencyContact wms$colon$EmergencyContact;
    String wms$colon$EMSCode;

    public Wms$colon$DangerousGoods() {
    }

    public Wms$colon$DangerousGoods(@XmlAttribute("regulated") boolean regulated, @XmlAttribute("regulationCode") String regulationCode, String wms$colon$UNNumber, String wms$colon$ProperShippingName, int wms$colon$HazardClass, String wms$colon$PackingGroup, Wms$colon$NetExplosiveOrLithiumWeight wms$colon$NetExplosiveOrLithiumWeight, String wms$colon$WattHourRating, Wms$colon$EmergencyContact wms$colon$EmergencyContact, String wms$colon$EMSCode) {
        this.regulated = regulated;
        this.regulationCode = regulationCode;
        this.wms$colon$UNNumber = wms$colon$UNNumber;
        this.wms$colon$ProperShippingName = wms$colon$ProperShippingName;
        this.wms$colon$HazardClass = wms$colon$HazardClass;
        this.wms$colon$PackingGroup = wms$colon$PackingGroup;
        this.wms$colon$NetExplosiveOrLithiumWeight = wms$colon$NetExplosiveOrLithiumWeight;
        this.wms$colon$WattHourRating = wms$colon$WattHourRating;
        this.wms$colon$EmergencyContact = wms$colon$EmergencyContact;
        this.wms$colon$EMSCode = wms$colon$EMSCode;
    }

    public boolean regulated() {
        return regulated;
    }
    @JsonProperty
    public void regulated(boolean value) {
        this.regulated = value;
    }

    public String regulationCode() {
        return regulationCode;
    }
    @JsonProperty
    public void regulationCode(String value) {
        this.regulationCode = value;
    }

    public String wms$colon$UNNumber() {
        return wms$colon$UNNumber;
    }
    @JsonProperty
    public void wms$colon$UNNumber(String value) {
        this.wms$colon$UNNumber = value;
    }

    public String wms$colon$ProperShippingName() {
        return wms$colon$ProperShippingName;
    }
    @JsonProperty
    public void wms$colon$ProperShippingName(String value) {
        this.wms$colon$ProperShippingName = value;
    }

    public int wms$colon$HazardClass() {
        return wms$colon$HazardClass;
    }
    @JsonProperty
    public void wms$colon$HazardClass(int value) {
        this.wms$colon$HazardClass = value;
    }

    public String wms$colon$PackingGroup() {
        return wms$colon$PackingGroup;
    }
    @JsonProperty
    public void wms$colon$PackingGroup(String value) {
        this.wms$colon$PackingGroup = value;
    }

    public Wms$colon$NetExplosiveOrLithiumWeight wms$colon$NetExplosiveOrLithiumWeight() {
        return wms$colon$NetExplosiveOrLithiumWeight;
    }
    @JsonProperty
    public void wms$colon$NetExplosiveOrLithiumWeight(Wms$colon$NetExplosiveOrLithiumWeight value) {
        this.wms$colon$NetExplosiveOrLithiumWeight = value;
    }

    public String wms$colon$WattHourRating() {
        return wms$colon$WattHourRating;
    }
    @JsonProperty
    public void wms$colon$WattHourRating(String value) {
        this.wms$colon$WattHourRating = value;
    }

    public Wms$colon$EmergencyContact wms$colon$EmergencyContact() {
        return wms$colon$EmergencyContact;
    }
    @JsonProperty
    public void wms$colon$EmergencyContact(Wms$colon$EmergencyContact value) {
        this.wms$colon$EmergencyContact = value;
    }

    public String wms$colon$EMSCode() {
        return wms$colon$EMSCode;
    }
    @JsonProperty
    public void wms$colon$EMSCode(String value) {
        this.wms$colon$EMSCode = value;
    }
}
