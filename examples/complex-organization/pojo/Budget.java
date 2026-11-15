import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Budget {
    String currency;
    int allocated;
    java.math.BigDecimal spent;
    List<CostCenters> costCenters;

    public String currency() {
        return currency;
    }
    @JsonProperty
    public void currency(String value) {
        this.currency = value;
    }

    public int allocated() {
        return allocated;
    }
    @JsonProperty
    public void allocated(int value) {
        this.allocated = value;
    }

    public java.math.BigDecimal spent() {
        return spent;
    }
    @JsonProperty
    public void spent(java.math.BigDecimal value) {
        this.spent = value;
    }

    public List<CostCenters> costCenters() {
        return costCenters;
    }
    @JsonProperty
    public void costCenters(List<CostCenters> value) {
        this.costCenters = value;
    }
}
