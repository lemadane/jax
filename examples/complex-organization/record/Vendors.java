import java.util.List;

public record Vendors(String vendorId, String name, List<Contracts> contracts) {
}
