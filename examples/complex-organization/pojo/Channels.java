import com.lemadane.jax.JsonProperty;

import java.util.List;

public class Channels {
    String email;
    String slackWebhook;
    List<String> pagerDutyIds;

    public String email() {
        return email;
    }
    @JsonProperty
    public void email(String value) {
        this.email = value;
    }

    public String slackWebhook() {
        return slackWebhook;
    }
    @JsonProperty
    public void slackWebhook(String value) {
        this.slackWebhook = value;
    }

    public List<String> pagerDutyIds() {
        return pagerDutyIds;
    }
    @JsonProperty
    public void pagerDutyIds(List<String> value) {
        this.pagerDutyIds = value;
    }
}
