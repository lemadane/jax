import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Cus$colon$TotalCustomsStatisticalValue {
    @XmlAttribute("currency") String currency;
    @XmlText String $text;

    public Cus$colon$TotalCustomsStatisticalValue() {
    }

    public Cus$colon$TotalCustomsStatisticalValue(@XmlAttribute("currency") String currency, @XmlText String $text) {
        this.currency = currency;
        this.$text = $text;
    }

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
