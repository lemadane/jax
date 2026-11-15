import java.util.List;

public record ComplexOrganization(String apiVersion, String requestId, String timestamp, Environment environment, Organization organization, Pagination pagination) {
}
