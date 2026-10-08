package util;

import java.text.DecimalFormat;

public final class Money {
    private Money() {}

    public static String won(long amount) {
        return new DecimalFormat("#,##0").format(amount) + "원";
    }

    /** floor(amount * part / whole) without overflow for realistic order sizes. */
    public static long share(long amount, long part, long whole) {
        if (whole <= 0) throw new IllegalArgumentException("whole must be positive");
        return Math.floorDiv(Math.multiplyExact(amount, part), whole);
    }
}
