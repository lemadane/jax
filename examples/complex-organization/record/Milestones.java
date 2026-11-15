import java.util.List;

public record Milestones(String id, String title, boolean completed, List<Deliverables> deliverables, List<Blockers> blockers) {
}
