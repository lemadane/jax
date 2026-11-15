import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Log$colon$GeoCoordinates {
    @XmlAttribute("lat") java.math.BigDecimal lat;
    @XmlAttribute("lon") java.math.BigDecimal lon;

    public Log$colon$GeoCoordinates() {
    }

    public Log$colon$GeoCoordinates(@XmlAttribute("lat") java.math.BigDecimal lat, @XmlAttribute("lon") java.math.BigDecimal lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public java.math.BigDecimal lat() {
        return lat;
    }
    @JsonProperty
    public void lat(java.math.BigDecimal value) {
        this.lat = value;
    }

    public java.math.BigDecimal lon() {
        return lon;
    }
    @JsonProperty
    public void lon(java.math.BigDecimal value) {
        this.lon = value;
    }
}
