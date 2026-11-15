import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Cus$colon$Certificate {
    @XmlAttribute("type") String type;
    @XmlAttribute("documentID") String documentID;
    @XmlAttribute("verified") boolean verified;

    public Cus$colon$Certificate() {
    }

    public Cus$colon$Certificate(@XmlAttribute("type") String type, @XmlAttribute("documentID") String documentID, @XmlAttribute("verified") boolean verified) {
        this.type = type;
        this.documentID = documentID;
        this.verified = verified;
    }

    public String type() {
        return type;
    }
    @JsonProperty
    public void type(String value) {
        this.type = value;
    }

    public String documentID() {
        return documentID;
    }
    @JsonProperty
    public void documentID(String value) {
        this.documentID = value;
    }

    public boolean verified() {
        return verified;
    }
    @JsonProperty
    public void verified(boolean value) {
        this.verified = value;
    }
}
