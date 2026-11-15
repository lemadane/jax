import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Milestones {
    String id;
    String title;
    boolean completed;
    List<Deliverables> deliverables;
    List<Blockers> blockers;

    public Milestones() {
    }

    public Milestones(String id, String title, boolean completed, List<Deliverables> deliverables, List<Blockers> blockers) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.deliverables = deliverables;
        this.blockers = blockers;
    }

    public String id() {
        return id;
    }
    @JsonProperty
    public void id(String value) {
        this.id = value;
    }

    public String title() {
        return title;
    }
    @JsonProperty
    public void title(String value) {
        this.title = value;
    }

    public boolean completed() {
        return completed;
    }
    @JsonProperty
    public void completed(boolean value) {
        this.completed = value;
    }

    public List<Deliverables> deliverables() {
        return deliverables;
    }
    @JsonProperty
    public void deliverables(List<Deliverables> value) {
        this.deliverables = value;
    }

    public List<Blockers> blockers() {
        return blockers;
    }
    @JsonProperty
    public void blockers(List<Blockers> value) {
        this.blockers = value;
    }
}
