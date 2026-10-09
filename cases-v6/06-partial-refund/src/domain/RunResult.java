package domain;

import java.util.List;

public record RunResult(String runId, List<String> batchIds, int submitted, int completed, int awaiting) {}
