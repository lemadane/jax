import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Sla {
    java.math.BigDecimal uptimeGuarantee;
    String supportTier;
    List<EscalationContacts> escalationContacts;

    public java.math.BigDecimal uptimeGuarantee() {
        return uptimeGuarantee;
    }
    @JsonProperty
    public void uptimeGuarantee(java.math.BigDecimal value) {
        this.uptimeGuarantee = value;
    }

    public String supportTier() {
        return supportTier;
    }
    @JsonProperty
    public void supportTier(String value) {
        this.supportTier = value;
    }

    public List<EscalationContacts> escalationContacts() {
        return escalationContacts;
    }
    @JsonProperty
    public void escalationContacts(List<EscalationContacts> value) {
        this.escalationContacts = value;
    }
}
