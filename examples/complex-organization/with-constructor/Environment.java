import com.lemadane.jax.JsonProperty;

public class Environment {
    String region;
    Cluster cluster;

    public Environment() {
    }

    public Environment(String region, Cluster cluster) {
        this.region = region;
        this.cluster = cluster;
    }

    public String region() {
        return region;
    }
    @JsonProperty
    public void region(String value) {
        this.region = value;
    }

    public Cluster cluster() {
        return cluster;
    }
    @JsonProperty
    public void cluster(Cluster value) {
        this.cluster = value;
    }
}
