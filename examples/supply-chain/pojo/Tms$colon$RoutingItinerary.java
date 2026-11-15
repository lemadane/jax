import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Tms$colon$RoutingItinerary {
    @XmlAttribute("totalDistanceKm") java.math.BigDecimal totalDistanceKm;
    @XmlAttribute("estimatedTotalTransitHours") int estimatedTotalTransitHours;
    List<Tms$colon$TransportLeg> tms$colon$TransportLeg;

    public java.math.BigDecimal totalDistanceKm() {
        return totalDistanceKm;
    }
    @JsonProperty
    public void totalDistanceKm(java.math.BigDecimal value) {
        this.totalDistanceKm = value;
    }

    public int estimatedTotalTransitHours() {
        return estimatedTotalTransitHours;
    }
    @JsonProperty
    public void estimatedTotalTransitHours(int value) {
        this.estimatedTotalTransitHours = value;
    }

    public List<Tms$colon$TransportLeg> tms$colon$TransportLeg() {
        return tms$colon$TransportLeg;
    }
    @JsonProperty
    public void tms$colon$TransportLeg(List<Tms$colon$TransportLeg> value) {
        this.tms$colon$TransportLeg = value;
    }
}
