import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Departments {
    String deptId;
    String name;
    Budget budget;
    List<Projects> projects;

    public String deptId() {
        return deptId;
    }
    @JsonProperty
    public void deptId(String value) {
        this.deptId = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public Budget budget() {
        return budget;
    }
    @JsonProperty
    public void budget(Budget value) {
        this.budget = value;
    }

    public List<Projects> projects() {
        return projects;
    }
    @JsonProperty
    public void projects(List<Projects> value) {
        this.projects = value;
    }
}
