import com.lemadane.jax.JsonProperty;

import java.util.List;

public class ComplexOrganization {
    String apiVersion;
    String requestId;
    String timestamp;
    Environment environment;
    Organization organization;
    Pagination pagination;

    public String apiVersion() {
        return apiVersion;
    }
    @JsonProperty
    public void apiVersion(String value) {
        this.apiVersion = value;
    }

    public String requestId() {
        return requestId;
    }
    @JsonProperty
    public void requestId(String value) {
        this.requestId = value;
    }

    public String timestamp() {
        return timestamp;
    }
    @JsonProperty
    public void timestamp(String value) {
        this.timestamp = value;
    }

    public Environment environment() {
        return environment;
    }
    @JsonProperty
    public void environment(Environment value) {
        this.environment = value;
    }

    public Organization organization() {
        return organization;
    }
    @JsonProperty
    public void organization(Organization value) {
        this.organization = value;
    }

    public Pagination pagination() {
        return pagination;
    }
    @JsonProperty
    public void pagination(Pagination value) {
        this.pagination = value;
    }
}
