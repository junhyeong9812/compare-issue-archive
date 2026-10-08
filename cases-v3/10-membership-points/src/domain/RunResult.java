package domain;

import java.util.List;

public record RunResult(int earned, long points, List<String> unknownMembers, int skipped) {}
