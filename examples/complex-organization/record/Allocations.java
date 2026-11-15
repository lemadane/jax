import java.util.List;

public record Allocations(String category, List<Integer> monthlyLimits, List<Vendors> vendors) {
}
