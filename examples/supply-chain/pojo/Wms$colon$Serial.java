import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$Serial {
    @XmlAttribute("rangeStart") String rangeStart;
    @XmlAttribute("rangeEnd") String rangeEnd;

    public String rangeStart() {
        return rangeStart;
    }
    @JsonProperty
    public void rangeStart(String value) {
        this.rangeStart = value;
    }

    public String rangeEnd() {
        return rangeEnd;
    }
    @JsonProperty
    public void rangeEnd(String value) {
        this.rangeEnd = value;
    }
}
