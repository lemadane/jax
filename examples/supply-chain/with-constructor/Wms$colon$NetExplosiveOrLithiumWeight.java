import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Wms$colon$NetExplosiveOrLithiumWeight {
    @XmlAttribute("unit") String unit;
    @XmlText String $text;

    public Wms$colon$NetExplosiveOrLithiumWeight() {
    }

    public Wms$colon$NetExplosiveOrLithiumWeight(@XmlAttribute("unit") String unit, @XmlText String $text) {
        this.unit = unit;
        this.$text = $text;
    }

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
