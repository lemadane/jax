import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Iot$colon$GPS {
    @XmlAttribute("lat") java.math.BigDecimal lat;
    @XmlAttribute("lon") java.math.BigDecimal lon;
    @XmlAttribute("speedKnots") java.math.BigDecimal speedKnots;
    @XmlAttribute("heading") int heading;

    public Iot$colon$GPS() {
    }

    public Iot$colon$GPS(@XmlAttribute("lat") java.math.BigDecimal lat, @XmlAttribute("lon") java.math.BigDecimal lon, @XmlAttribute("speedKnots") java.math.BigDecimal speedKnots, @XmlAttribute("heading") int heading) {
        this.lat = lat;
        this.lon = lon;
        this.speedKnots = speedKnots;
        this.heading = heading;
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

    public java.math.BigDecimal speedKnots() {
        return speedKnots;
    }
    @JsonProperty
    public void speedKnots(java.math.BigDecimal value) {
        this.speedKnots = value;
    }

    public int heading() {
        return heading;
    }
    @JsonProperty
    public void heading(int value) {
        this.heading = value;
    }
}
