import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Log$colon$InterchangeHeader {
    Log$colon$Sender log$colon$Sender;
    Log$colon$Receiver log$colon$Receiver;
    Log$colon$SecurityAudit log$colon$SecurityAudit;

    public Log$colon$Sender log$colon$Sender() {
        return log$colon$Sender;
    }
    @JsonProperty
    public void log$colon$Sender(Log$colon$Sender value) {
        this.log$colon$Sender = value;
    }

    public Log$colon$Receiver log$colon$Receiver() {
        return log$colon$Receiver;
    }
    @JsonProperty
    public void log$colon$Receiver(Log$colon$Receiver value) {
        this.log$colon$Receiver = value;
    }

    public Log$colon$SecurityAudit log$colon$SecurityAudit() {
        return log$colon$SecurityAudit;
    }
    @JsonProperty
    public void log$colon$SecurityAudit(Log$colon$SecurityAudit value) {
        this.log$colon$SecurityAudit = value;
    }
}
