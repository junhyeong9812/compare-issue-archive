package domain;

import java.time.LocalDate;

/** One row of a partner's own settlement statement. */
public record StatementRow(LocalDate date, String partnerId, int count, long grossKrw) {}
