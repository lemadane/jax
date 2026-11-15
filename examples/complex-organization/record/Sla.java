import java.util.List;

public record Sla(java.math.BigDecimal uptimeGuarantee, String supportTier, List<EscalationContacts> escalationContacts) {
}
