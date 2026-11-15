import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Log$colon$ServiceLevel {
    @XmlAttribute("incoterms") String incoterms;
    @XmlAttribute("incotermsVersion") int incotermsVersion;
    @XmlAttribute("serviceType") String serviceType;
    @XmlAttribute("paymentTerms") String paymentTerms;

    public Log$colon$ServiceLevel() {
    }

    public Log$colon$ServiceLevel(@XmlAttribute("incoterms") String incoterms, @XmlAttribute("incotermsVersion") int incotermsVersion, @XmlAttribute("serviceType") String serviceType, @XmlAttribute("paymentTerms") String paymentTerms) {
        this.incoterms = incoterms;
        this.incotermsVersion = incotermsVersion;
        this.serviceType = serviceType;
        this.paymentTerms = paymentTerms;
    }

    public String incoterms() {
        return incoterms;
    }
    @JsonProperty
    public void incoterms(String value) {
        this.incoterms = value;
    }

    public int incotermsVersion() {
        return incotermsVersion;
    }
    @JsonProperty
    public void incotermsVersion(int value) {
        this.incotermsVersion = value;
    }

    public String serviceType() {
        return serviceType;
    }
    @JsonProperty
    public void serviceType(String value) {
        this.serviceType = value;
    }

    public String paymentTerms() {
        return paymentTerms;
    }
    @JsonProperty
    public void paymentTerms(String value) {
        this.paymentTerms = value;
    }
}
