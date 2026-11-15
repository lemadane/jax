import com.lemadane.jax.JsonProperty;

public class Thresholds {
    java.math.BigDecimal cpuPercent;
    int memoryPercent;

    public Thresholds() {
    }

    public Thresholds(java.math.BigDecimal cpuPercent, int memoryPercent) {
        this.cpuPercent = cpuPercent;
        this.memoryPercent = memoryPercent;
    }

    public java.math.BigDecimal cpuPercent() {
        return cpuPercent;
    }
    @JsonProperty
    public void cpuPercent(java.math.BigDecimal value) {
        this.cpuPercent = value;
    }

    public int memoryPercent() {
        return memoryPercent;
    }
    @JsonProperty
    public void memoryPercent(int value) {
        this.memoryPercent = value;
    }
}
