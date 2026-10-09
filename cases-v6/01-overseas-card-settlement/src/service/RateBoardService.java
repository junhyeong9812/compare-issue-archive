package service;

import domain.FxQuote;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Customer-facing exchange rate board for the app's travel wallet screen. */
public class RateBoardService {
    private final RateService rates;
    private final List<String> currencies;

    public RateBoardService(RateService rates, List<String> currencies) {
        this.rates = rates;
        this.currencies = currencies;
    }

    public List<FxQuote> board(LocalDate date) {
        List<FxQuote> out = new ArrayList<>();
        for (String c : currencies) {
            if (CurrencyPairs.supported(c)) out.add(rates.quote(c, date));
        }
        return out;
    }
}
