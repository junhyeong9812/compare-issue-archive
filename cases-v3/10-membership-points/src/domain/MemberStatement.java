package domain;

/** Member-facing points summary: balance and history come from the points vendor. */
public record MemberStatement(String memberNo, String name, Grade grade, long balance, int entries,
                              long earnedThisMonth) {}
