import com.lemadane.jax.JsonProperty;

public class Metrics {
    LatencyMs latencyMs;
    int throughputRps;

    public Metrics() {
    }

    public Metrics(LatencyMs latencyMs, int throughputRps) {
        this.latencyMs = latencyMs;
        this.throughputRps = throughputRps;
    }

    public LatencyMs latencyMs() {
        return latencyMs;
    }
    @JsonProperty
    public void latencyMs(LatencyMs value) {
        this.latencyMs = value;
    }

    public int throughputRps() {
        return throughputRps;
    }
    @JsonProperty
    public void throughputRps(int value) {
        this.throughputRps = value;
    }
}
