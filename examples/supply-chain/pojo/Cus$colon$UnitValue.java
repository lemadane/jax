import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Cus$colon$UnitValue {
    @XmlAttribute("currency") String currency;
    @XmlText String $text;

    public String currency() {
        return currency;
    }
    @JsonProperty
    public void currency(String value) {
        this.currency = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
