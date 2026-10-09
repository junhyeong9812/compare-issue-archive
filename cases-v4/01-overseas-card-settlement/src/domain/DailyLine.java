package domain;

import java.time.LocalDate;

/** Settlement result for one partner on one KST date (KRW, whole won). */
public record DailyLine(LocalDate date, String partnerId, int count, long grossKrw, long feeKrw) {
    public long netKrw() { return grossKrw - feeKrw; }
}
