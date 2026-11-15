import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Tms$colon$Carrier {
    @XmlAttribute("scac") String scac;
    @XmlAttribute("name") String name;
    @XmlAttribute("uicCode") Integer uicCode;

    public String scac() {
        return scac;
    }
    @JsonProperty
    public void scac(String value) {
        this.scac = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public Integer uicCode() {
        return uicCode;
    }
    @JsonProperty
    public void uicCode(Integer value) {
        this.uicCode = value;
    }
}
