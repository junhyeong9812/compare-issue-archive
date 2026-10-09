package service;

import domain.FxQuote;
import infra.FxqClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** FX rates for settlement and display, backed by the vendor CLI with a per-run cache. */
public class RateService {
    private final FxqClient client;
    private final Map<String, FxQuote> cache = new HashMap<>();
    private final Map<String, FxQuote> settledQuotes = new HashMap<>();
    private int vendorCalls;

    public RateService(FxqClient client) {
        this.client = client;
    }

    public FxQuote quote(String currency, LocalDate date) {
        String pair = CurrencyPairs.pairOf(currency);
        return cache.computeIfAbsent(pair + "@" + date, k -> {
            vendorCalls++;
            return FxqClient.parse(client.rate(pair, date));
        });
    }

    /**
     * Rate applied to card transactions of the given KST settlement date.
     * A lagged quote (trailing ~N) repeats an earlier day's value and must not be settled with;
     * the vendor's source-3 value for that date is used instead.
     */
    public BigDecimal settlementRate(String currency, LocalDate date) {
        FxQuote q = quote(currency, date);
        if (!q.lagged()) return q.rate();
        String pair = CurrencyPairs.pairOf(currency);
        return settledQuotes.computeIfAbsent(pair + "@" + date, k -> {
            vendorCalls++;
            return FxqClient.parse(client.settlementQuote(pair, date));
        }).rate();
    }

    public int vendorCalls() {
        return vendorCalls;
    }
}
