import com.lemadane.jax.JsonProperty;

import java.util.List;

public class EscalationContacts {
    String role;
    String name;
    Channels channels;

    public String role() {
        return role;
    }
    @JsonProperty
    public void role(String value) {
        this.role = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public Channels channels() {
        return channels;
    }
    @JsonProperty
    public void channels(Channels value) {
        this.channels = value;
    }
}
