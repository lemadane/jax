import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Log$colon$Contact {
    @XmlAttribute("department") String department;
    String log$colon$FullName;
    String log$colon$Phone;
    String log$colon$Email;

    public Log$colon$Contact() {
    }

    public Log$colon$Contact(@XmlAttribute("department") String department, String log$colon$FullName, String log$colon$Phone, String log$colon$Email) {
        this.department = department;
        this.log$colon$FullName = log$colon$FullName;
        this.log$colon$Phone = log$colon$Phone;
        this.log$colon$Email = log$colon$Email;
    }

    public String department() {
        return department;
    }
    @JsonProperty
    public void department(String value) {
        this.department = value;
    }

    public String log$colon$FullName() {
        return log$colon$FullName;
    }
    @JsonProperty
    public void log$colon$FullName(String value) {
        this.log$colon$FullName = value;
    }

    public String log$colon$Phone() {
        return log$colon$Phone;
    }
    @JsonProperty
    public void log$colon$Phone(String value) {
        this.log$colon$Phone = value;
    }

    public String log$colon$Email() {
        return log$colon$Email;
    }
    @JsonProperty
    public void log$colon$Email(String value) {
        this.log$colon$Email = value;
    }
}
