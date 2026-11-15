import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Projects {
    String projectId;
    String status;
    List<String> tags;
    List<Milestones> milestones;

    public Projects() {
    }

    public Projects(String projectId, String status, List<String> tags, List<Milestones> milestones) {
        this.projectId = projectId;
        this.status = status;
        this.tags = tags;
        this.milestones = milestones;
    }

    public String projectId() {
        return projectId;
    }
    @JsonProperty
    public void projectId(String value) {
        this.projectId = value;
    }

    public String status() {
        return status;
    }
    @JsonProperty
    public void status(String value) {
        this.status = value;
    }

    public List<String> tags() {
        return tags;
    }
    @JsonProperty
    public void tags(List<String> value) {
        this.tags = value;
    }

    public List<Milestones> milestones() {
        return milestones;
    }
    @JsonProperty
    public void milestones(List<Milestones> value) {
        this.milestones = value;
    }
}
