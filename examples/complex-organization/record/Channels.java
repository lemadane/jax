import java.util.List;

public record Channels(String email, String slackWebhook, List<String> pagerDutyIds) {
}
