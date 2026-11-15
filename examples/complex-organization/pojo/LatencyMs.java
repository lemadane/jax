import com.lemadane.jax.JsonProperty;

public class LatencyMs {
    java.math.BigDecimal p50;
    java.math.BigDecimal p95;
    java.math.BigDecimal p99;

    public java.math.BigDecimal p50() {
        return p50;
    }
    @JsonProperty
    public void p50(java.math.BigDecimal value) {
        this.p50 = value;
    }

    public java.math.BigDecimal p95() {
        return p95;
    }
    @JsonProperty
    public void p95(java.math.BigDecimal value) {
        this.p95 = value;
    }

    public java.math.BigDecimal p99() {
        return p99;
    }
    @JsonProperty
    public void p99(java.math.BigDecimal value) {
        this.p99 = value;
    }
}
