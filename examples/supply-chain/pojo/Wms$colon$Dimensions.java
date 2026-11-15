import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Wms$colon$Dimensions {
    @XmlAttribute("unit") String unit;
    @XmlAttribute("length") int length;
    @XmlAttribute("width") int width;
    @XmlAttribute("height") int height;

    public String unit() {
        return unit;
    }
    @JsonProperty
    public void unit(String value) {
        this.unit = value;
    }

    public int length() {
        return length;
    }
    @JsonProperty
    public void length(int value) {
        this.length = value;
    }

    public int width() {
        return width;
    }
    @JsonProperty
    public void width(int value) {
        this.width = value;
    }

    public int height() {
        return height;
    }
    @JsonProperty
    public void height(int value) {
        this.height = value;
    }
}
