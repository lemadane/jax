import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Wms$colon$MaxAllowedTemp {
    @XmlAttribute("unit") String unit;
    @XmlText String $text;

    public String unit() {
        return unit;
    }
    @JsonProperty
    public void unit(String value) {
        this.unit = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
