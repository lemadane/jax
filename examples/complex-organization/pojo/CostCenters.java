import com.lemadane.jax.JsonProperty;

import java.util.List;

public class CostCenters {
    String code;
    List<Allocations> allocations;

    public String code() {
        return code;
    }
    @JsonProperty
    public void code(String value) {
        this.code = value;
    }

    public List<Allocations> allocations() {
        return allocations;
    }
    @JsonProperty
    public void allocations(List<Allocations> value) {
        this.allocations = value;
    }
}
