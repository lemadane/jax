import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Wms$colon$Seal {
    @XmlAttribute("issuer") String issuer;
    @XmlAttribute("type") String type;
    @XmlText String $text;

    public String issuer() {
        return issuer;
    }
    @JsonProperty
    public void issuer(String value) {
        this.issuer = value;
    }

    public String type() {
        return type;
    }
    @JsonProperty
    public void type(String value) {
        this.type = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
