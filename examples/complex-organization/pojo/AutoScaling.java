import com.lemadane.jax.JsonProperty;

public class AutoScaling {
    boolean enabled;
    Thresholds thresholds;

    public boolean enabled() {
        return enabled;
    }
    @JsonProperty
    public void enabled(boolean value) {
        this.enabled = value;
    }

    public Thresholds thresholds() {
        return thresholds;
    }
    @JsonProperty
    public void thresholds(Thresholds value) {
        this.thresholds = value;
    }
}
