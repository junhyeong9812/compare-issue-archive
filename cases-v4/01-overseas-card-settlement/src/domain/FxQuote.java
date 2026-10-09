package domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single quote returned by the FX vendor for one pair and one value date. */
public record FxQuote(String pair, LocalDate date, BigDecimal rate) {}
