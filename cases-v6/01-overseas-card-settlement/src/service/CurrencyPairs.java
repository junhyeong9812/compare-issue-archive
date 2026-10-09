package service;

import java.util.Map;

/** Maps a card currency to the vendor pair code and its quotation unit. */
public final class CurrencyPairs {
    private static final Map<String, Integer> UNITS = Map.of("USD", 1, "EUR", 1, "JPY", 100, "CNY", 1);

    private CurrencyPairs() {}

    public static String pairOf(String currency) {
        requireSupported(currency);
        return currency + "KRW";
    }

    /** JPY is quoted per 100 yen. */
    public static int unitOf(String currency) {
        requireSupported(currency);
        return UNITS.get(currency);
    }

    public static boolean supported(String currency) {
        return UNITS.containsKey(currency);
    }

    private static void requireSupported(String currency) {
        if (!supported(currency)) throw new IllegalArgumentException("unsupported currency " + currency);
    }
}
