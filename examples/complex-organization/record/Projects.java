import java.util.List;

public record Projects(String projectId, String status, List<String> tags, List<Milestones> milestones) {
}
