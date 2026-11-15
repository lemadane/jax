import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$EmergencyContact {
    @XmlAttribute("provider") String provider;
    @XmlAttribute("phone") String phone;
    @XmlAttribute("contractID") String contractID;

    public Wms$colon$EmergencyContact() {
    }

    public Wms$colon$EmergencyContact(@XmlAttribute("provider") String provider, @XmlAttribute("phone") String phone, @XmlAttribute("contractID") String contractID) {
        this.provider = provider;
        this.phone = phone;
        this.contractID = contractID;
    }

    public String provider() {
        return provider;
    }
    @JsonProperty
    public void provider(String value) {
        this.provider = value;
    }

    public String phone() {
        return phone;
    }
    @JsonProperty
    public void phone(String value) {
        this.phone = value;
    }

    public String contractID() {
        return contractID;
    }
    @JsonProperty
    public void contractID(String value) {
        this.contractID = value;
    }
}
