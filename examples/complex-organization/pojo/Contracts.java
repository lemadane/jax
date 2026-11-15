import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Contracts {
    String contractId;
    boolean active;
    Sla sla;

    public String contractId() {
        return contractId;
    }
    @JsonProperty
    public void contractId(String value) {
        this.contractId = value;
    }

    public boolean active() {
        return active;
    }
    @JsonProperty
    public void active(boolean value) {
        this.active = value;
    }

    public Sla sla() {
        return sla;
    }
    @JsonProperty
    public void sla(Sla value) {
        this.sla = value;
    }
}
