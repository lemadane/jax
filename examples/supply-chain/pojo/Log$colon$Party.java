import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Log$colon$Party {
    @XmlAttribute("role") String role;
    @XmlAttribute("taxID") String taxID;
    String log$colon$Name;
    Log$colon$Address log$colon$Address;
    Log$colon$Contact log$colon$Contact;
    @XmlAttribute("licenseNumber") String licenseNumber;

    public String role() {
        return role;
    }
    @JsonProperty
    public void role(String value) {
        this.role = value;
    }

    public String taxID() {
        return taxID;
    }
    @JsonProperty
    public void taxID(String value) {
        this.taxID = value;
    }

    public String log$colon$Name() {
        return log$colon$Name;
    }
    @JsonProperty
    public void log$colon$Name(String value) {
        this.log$colon$Name = value;
    }

    public Log$colon$Address log$colon$Address() {
        return log$colon$Address;
    }
    @JsonProperty
    public void log$colon$Address(Log$colon$Address value) {
        this.log$colon$Address = value;
    }

    public Log$colon$Contact log$colon$Contact() {
        return log$colon$Contact;
    }
    @JsonProperty
    public void log$colon$Contact(Log$colon$Contact value) {
        this.log$colon$Contact = value;
    }

    public String licenseNumber() {
        return licenseNumber;
    }
    @JsonProperty
    public void licenseNumber(String value) {
        this.licenseNumber = value;
    }
}
