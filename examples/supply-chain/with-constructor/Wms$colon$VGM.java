import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$VGM {
    @XmlAttribute("verifiedBy") String verifiedBy;
    @XmlAttribute("method") String method;
    @XmlAttribute("timestamp") String timestamp;

    public Wms$colon$VGM() {
    }

    public Wms$colon$VGM(@XmlAttribute("verifiedBy") String verifiedBy, @XmlAttribute("method") String method, @XmlAttribute("timestamp") String timestamp) {
        this.verifiedBy = verifiedBy;
        this.method = method;
        this.timestamp = timestamp;
    }

    public String verifiedBy() {
        return verifiedBy;
    }
    @JsonProperty
    public void verifiedBy(String value) {
        this.verifiedBy = value;
    }

    public String method() {
        return method;
    }
    @JsonProperty
    public void method(String value) {
        this.method = value;
    }

    public String timestamp() {
        return timestamp;
    }
    @JsonProperty
    public void timestamp(String value) {
        this.timestamp = value;
    }
}
