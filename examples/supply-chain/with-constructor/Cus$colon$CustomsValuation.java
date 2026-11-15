import com.lemadane.jax.JsonProperty;

import com.lemadane.jax.XmlAttribute;

public class Cus$colon$CustomsValuation {
    @XmlAttribute("hsCode") String hsCode;
    @XmlAttribute("countryOfOrigin") String countryOfOrigin;
    Cus$colon$UnitValue cus$colon$UnitValue;
    Cus$colon$TotalLineValue cus$colon$TotalLineValue;
    java.math.BigDecimal cus$colon$DutyRatePercent;
    String cus$colon$PreferentialTariffCode;

    public Cus$colon$CustomsValuation() {
    }

    public Cus$colon$CustomsValuation(@XmlAttribute("hsCode") String hsCode, @XmlAttribute("countryOfOrigin") String countryOfOrigin, Cus$colon$UnitValue cus$colon$UnitValue, Cus$colon$TotalLineValue cus$colon$TotalLineValue, java.math.BigDecimal cus$colon$DutyRatePercent, String cus$colon$PreferentialTariffCode) {
        this.hsCode = hsCode;
        this.countryOfOrigin = countryOfOrigin;
        this.cus$colon$UnitValue = cus$colon$UnitValue;
        this.cus$colon$TotalLineValue = cus$colon$TotalLineValue;
        this.cus$colon$DutyRatePercent = cus$colon$DutyRatePercent;
        this.cus$colon$PreferentialTariffCode = cus$colon$PreferentialTariffCode;
    }

    public String hsCode() {
        return hsCode;
    }
    @JsonProperty
    public void hsCode(String value) {
        this.hsCode = value;
    }

    public String countryOfOrigin() {
        return countryOfOrigin;
    }
    @JsonProperty
    public void countryOfOrigin(String value) {
        this.countryOfOrigin = value;
    }

    public Cus$colon$UnitValue cus$colon$UnitValue() {
        return cus$colon$UnitValue;
    }
    @JsonProperty
    public void cus$colon$UnitValue(Cus$colon$UnitValue value) {
        this.cus$colon$UnitValue = value;
    }

    public Cus$colon$TotalLineValue cus$colon$TotalLineValue() {
        return cus$colon$TotalLineValue;
    }
    @JsonProperty
    public void cus$colon$TotalLineValue(Cus$colon$TotalLineValue value) {
        this.cus$colon$TotalLineValue = value;
    }

    public java.math.BigDecimal cus$colon$DutyRatePercent() {
        return cus$colon$DutyRatePercent;
    }
    @JsonProperty
    public void cus$colon$DutyRatePercent(java.math.BigDecimal value) {
        this.cus$colon$DutyRatePercent = value;
    }

    public String cus$colon$PreferentialTariffCode() {
        return cus$colon$PreferentialTariffCode;
    }
    @JsonProperty
    public void cus$colon$PreferentialTariffCode(String value) {
        this.cus$colon$PreferentialTariffCode = value;
    }
}
