package util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** KRW arithmetic shared by settlement and reporting. */
public final class Money {
    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    private Money() {}

    /** foreign amount x rate / unit, rounded to whole won (acquirer contract: half-up). */
    public static long toKrw(BigDecimal amount, BigDecimal rate, int unit) {
        return amount.multiply(rate)
                .divide(BigDecimal.valueOf(unit), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Commission on gross KRW; partner contract truncates fractional won. */
    public static long fee(long grossKrw, int feeBps) {
        if (grossKrw <= 0 || feeBps <= 0) return 0;
        return BigDecimal.valueOf(grossKrw)
                .multiply(BigDecimal.valueOf(feeBps))
                .divide(TEN_THOUSAND, 0, RoundingMode.DOWN)
                .longValueExact();
    }

    public static String won(long krw) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return f.format(krw);
    }

    public static String rate(BigDecimal rate) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return f.format(rate);
    }
}
