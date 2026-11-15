import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

import java.util.List;

public class Cus$colon$CustomsSummary {
    @XmlAttribute("declarationType") String declarationType;
    @XmlAttribute("entryPointCode") String entryPointCode;
    Cus$colon$TotalInvoiceValue cus$colon$TotalInvoiceValue;
    Cus$colon$FreightCharges cus$colon$FreightCharges;
    Cus$colon$InsuranceCharges cus$colon$InsuranceCharges;
    Cus$colon$TotalCustomsStatisticalValue cus$colon$TotalCustomsStatisticalValue;
    Cus$colon$RequiredCertificates cus$colon$RequiredCertificates;

    public String declarationType() {
        return declarationType;
    }
    @JsonProperty
    public void declarationType(String value) {
        this.declarationType = value;
    }

    public String entryPointCode() {
        return entryPointCode;
    }
    @JsonProperty
    public void entryPointCode(String value) {
        this.entryPointCode = value;
    }

    public Cus$colon$TotalInvoiceValue cus$colon$TotalInvoiceValue() {
        return cus$colon$TotalInvoiceValue;
    }
    @JsonProperty
    public void cus$colon$TotalInvoiceValue(Cus$colon$TotalInvoiceValue value) {
        this.cus$colon$TotalInvoiceValue = value;
    }

    public Cus$colon$FreightCharges cus$colon$FreightCharges() {
        return cus$colon$FreightCharges;
    }
    @JsonProperty
    public void cus$colon$FreightCharges(Cus$colon$FreightCharges value) {
        this.cus$colon$FreightCharges = value;
    }

    public Cus$colon$InsuranceCharges cus$colon$InsuranceCharges() {
        return cus$colon$InsuranceCharges;
    }
    @JsonProperty
    public void cus$colon$InsuranceCharges(Cus$colon$InsuranceCharges value) {
        this.cus$colon$InsuranceCharges = value;
    }

    public Cus$colon$TotalCustomsStatisticalValue cus$colon$TotalCustomsStatisticalValue() {
        return cus$colon$TotalCustomsStatisticalValue;
    }
    @JsonProperty
    public void cus$colon$TotalCustomsStatisticalValue(Cus$colon$TotalCustomsStatisticalValue value) {
        this.cus$colon$TotalCustomsStatisticalValue = value;
    }

    public Cus$colon$RequiredCertificates cus$colon$RequiredCertificates() {
        return cus$colon$RequiredCertificates;
    }
    @JsonProperty
    public void cus$colon$RequiredCertificates(Cus$colon$RequiredCertificates value) {
        this.cus$colon$RequiredCertificates = value;
    }
}
