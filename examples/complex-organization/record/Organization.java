import java.util.List;

public record Organization(String orgId, String name, boolean isVerified, String taxIdentifier, List<Departments> departments) {
}
