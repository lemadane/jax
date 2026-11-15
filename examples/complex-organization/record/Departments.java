import java.util.List;

public record Departments(String deptId, String name, Budget budget, List<Projects> projects) {
}
