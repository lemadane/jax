import com.lemadane.jax.JsonProperty;

public class Cluster {
    String id;
    int nodes;
    AutoScaling autoScaling;

    public String id() {
        return id;
    }
    @JsonProperty
    public void id(String value) {
        this.id = value;
    }

    public int nodes() {
        return nodes;
    }
    @JsonProperty
    public void nodes(int value) {
        this.nodes = value;
    }

    public AutoScaling autoScaling() {
        return autoScaling;
    }
    @JsonProperty
    public void autoScaling(AutoScaling value) {
        this.autoScaling = value;
    }
}
