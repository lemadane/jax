import com.lemadane.jax.JsonProperty;

public class Deliverables {
    String artifact;
    String checksum;
    Metrics metrics;

    public String artifact() {
        return artifact;
    }
    @JsonProperty
    public void artifact(String value) {
        this.artifact = value;
    }

    public String checksum() {
        return checksum;
    }
    @JsonProperty
    public void checksum(String value) {
        this.checksum = value;
    }

    public Metrics metrics() {
        return metrics;
    }
    @JsonProperty
    public void metrics(Metrics value) {
        this.metrics = value;
    }
}
