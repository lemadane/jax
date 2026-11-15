import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Wms$colon$Quantity {
    @XmlAttribute("unitOfMeasure") String unitOfMeasure;
    @XmlText String $text;

    public String unitOfMeasure() {
        return unitOfMeasure;
    }
    @JsonProperty
    public void unitOfMeasure(String value) {
        this.unitOfMeasure = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
