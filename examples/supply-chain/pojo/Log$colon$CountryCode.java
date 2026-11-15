import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Log$colon$CountryCode {
    @XmlAttribute("iso") String iso;
    @XmlText String $text;

    public String iso() {
        return iso;
    }
    @JsonProperty
    public void iso(String value) {
        this.iso = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
