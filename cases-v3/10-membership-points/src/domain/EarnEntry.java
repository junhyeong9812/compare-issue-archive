package domain;

import java.time.LocalDate;

/** One earn we sent to the points vendor, with the vendor entry number. */
public record EarnEntry(String payId, String memberNo, long points, LocalDate day, String entryNo) {}
