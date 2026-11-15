import com.lemadane.jax.JsonProperty;

public class Log$colon$SecurityAudit {
    String log$colon$DigitalSignatureAlgorithm;
    String log$colon$Checksum;
    String log$colon$ComplianceFramework;

    public Log$colon$SecurityAudit() {
    }

    public Log$colon$SecurityAudit(String log$colon$DigitalSignatureAlgorithm, String log$colon$Checksum, String log$colon$ComplianceFramework) {
        this.log$colon$DigitalSignatureAlgorithm = log$colon$DigitalSignatureAlgorithm;
        this.log$colon$Checksum = log$colon$Checksum;
        this.log$colon$ComplianceFramework = log$colon$ComplianceFramework;
    }

    public String log$colon$DigitalSignatureAlgorithm() {
        return log$colon$DigitalSignatureAlgorithm;
    }
    @JsonProperty
    public void log$colon$DigitalSignatureAlgorithm(String value) {
        this.log$colon$DigitalSignatureAlgorithm = value;
    }

    public String log$colon$Checksum() {
        return log$colon$Checksum;
    }
    @JsonProperty
    public void log$colon$Checksum(String value) {
        this.log$colon$Checksum = value;
    }

    public String log$colon$ComplianceFramework() {
        return log$colon$ComplianceFramework;
    }
    @JsonProperty
    public void log$colon$ComplianceFramework(String value) {
        this.log$colon$ComplianceFramework = value;
    }
}
