package domain;

/** Earn rate in basis points of the payment amount. */
public enum Grade {
    BASIC(100),
    GOLD(150),
    VIP(200);

    private final int bps;

    Grade(int bps) {
        this.bps = bps;
    }

    public int bps() {
        return bps;
    }
}
