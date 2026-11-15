import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Organization {
    String orgId;
    String name;
    boolean isVerified;
    String taxIdentifier;
    List<Departments> departments;

    public Organization() {
    }

    public Organization(String orgId, String name, boolean isVerified, String taxIdentifier, List<Departments> departments) {
        this.orgId = orgId;
        this.name = name;
        this.isVerified = isVerified;
        this.taxIdentifier = taxIdentifier;
        this.departments = departments;
    }

    public String orgId() {
        return orgId;
    }
    @JsonProperty
    public void orgId(String value) {
        this.orgId = value;
    }

    public String name() {
        return name;
    }
    @JsonProperty
    public void name(String value) {
        this.name = value;
    }

    public boolean isVerified() {
        return isVerified;
    }
    @JsonProperty
    public void isVerified(boolean value) {
        this.isVerified = value;
    }

    public String taxIdentifier() {
        return taxIdentifier;
    }
    @JsonProperty
    public void taxIdentifier(String value) {
        this.taxIdentifier = value;
    }

    public List<Departments> departments() {
        return departments;
    }
    @JsonProperty
    public void departments(List<Departments> value) {
        this.departments = value;
    }
}
