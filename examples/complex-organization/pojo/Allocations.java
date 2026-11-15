import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Allocations {
    String category;
    List<Integer> monthlyLimits;
    List<Vendors> vendors;

    public String category() {
        return category;
    }
    @JsonProperty
    public void category(String value) {
        this.category = value;
    }

    public List<Integer> monthlyLimits() {
        return monthlyLimits;
    }
    @JsonProperty
    public void monthlyLimits(List<Integer> value) {
        this.monthlyLimits = value;
    }

    public List<Vendors> vendors() {
        return vendors;
    }
    @JsonProperty
    public void vendors(List<Vendors> value) {
        this.vendors = value;
    }
}
