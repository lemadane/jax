import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Vendors {
    String vendorId;
    String name;
    List<Contracts> contracts;

    public String vendorId() {
        return vendorId;
    }
    @JsonProperty
    public void vendorId(String value) {
        this.vendorId = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public List<Contracts> contracts() {
        return contracts;
    }
    @JsonProperty
    public void contracts(List<Contracts> value) {
        this.contracts = value;
    }
}
