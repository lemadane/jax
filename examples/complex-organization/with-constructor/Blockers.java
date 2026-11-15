import com.lemadane.jax.JsonProperty;

public class Blockers {
    String issueId;
    String severity;
    AssignedTo assignedTo;

    public Blockers() {
    }

    public Blockers(String issueId, String severity, AssignedTo assignedTo) {
        this.issueId = issueId;
        this.severity = severity;
        this.assignedTo = assignedTo;
    }

    public String issueId() {
        return issueId;
    }
    @JsonProperty
    public void issueId(String value) {
        this.issueId = value;
    }

    public String severity() {
        return severity;
    }
    @JsonProperty
    public void severity(String value) {
        this.severity = value;
    }

    public AssignedTo assignedTo() {
        return assignedTo;
    }
    @JsonProperty
    public void assignedTo(AssignedTo value) {
        this.assignedTo = value;
    }
}
