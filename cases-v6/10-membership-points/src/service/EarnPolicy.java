package service;

import domain.Grade;

/** Points for one payment: grade rate in basis points, rounded down, capped per payment. */
public final class EarnPolicy {
    private final long capPerPayment;

    public EarnPolicy(long capPerPayment) {
        this.capPerPayment = capPerPayment;
    }

    public long points(long amount, Grade grade) {
        if (amount <= 0) return 0;
        long raw = Math.floorDiv(Math.multiplyExact(amount, (long) grade.bps()), 10_000L);
        return Math.min(raw, capPerPayment);
    }
}
