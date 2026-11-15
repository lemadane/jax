import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Cus$colon$RequiredCertificates {
    List<Cus$colon$Certificate> cus$colon$Certificate;

    public Cus$colon$RequiredCertificates() {
    }

    public Cus$colon$RequiredCertificates(List<Cus$colon$Certificate> cus$colon$Certificate) {
        this.cus$colon$Certificate = cus$colon$Certificate;
    }

    public List<Cus$colon$Certificate> cus$colon$Certificate() {
        return cus$colon$Certificate;
    }
    @JsonProperty
    public void cus$colon$Certificate(List<Cus$colon$Certificate> value) {
        this.cus$colon$Certificate = value;
    }
}
