import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import com.lemadane.jax.XmlText;

public class Log$colon$OrganizationID {
    @XmlAttribute("qualifier") String qualifier;
    @XmlText String $text;

    public Log$colon$OrganizationID() {
    }

    public Log$colon$OrganizationID(@XmlAttribute("qualifier") String qualifier, @XmlText String $text) {
        this.qualifier = qualifier;
        this.$text = $text;
    }

    public String qualifier() {
        return qualifier;
    }
    @JsonProperty
    public void qualifier(String value) {
        this.qualifier = value;
    }

    public String $text() {
        return $text;
    }
    @JsonProperty
    public void $text(String value) {
        this.$text = value;
    }
}
