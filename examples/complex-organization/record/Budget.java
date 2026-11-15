import java.util.List;

public record Budget(String currency, int allocated, java.math.BigDecimal spent, List<CostCenters> costCenters) {
}
