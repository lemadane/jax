import com.lemadane.jax.JsonProperty;

public class AssignedTo {
    String employeeId;
    String name;
    boolean onCall;

    public String employeeId() {
        return employeeId;
    }
    @JsonProperty
    public void employeeId(String value) {
        this.employeeId = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public boolean onCall() {
        return onCall;
    }
    @JsonProperty
    public void onCall(boolean value) {
        this.onCall = value;
    }
}
