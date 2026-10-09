import config.AppConfig;
import domain.DailyLine;
import domain.FxQuote;
import service.ReconcileService.Mismatch;
import domain.StatementRow;
import infra.FxqClient;
import service.BusinessDateResolver;
import service.CurrencyPairs;
import service.ReconcileService;
import util.Csv;
import util.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class UnitChecks {
    public static void main(String[] args) {
        TestSupport t = new TestSupport("unit");

        t.test("krw conversion rounds half-up to whole won", () -> {
            TestSupport.eq(16663L, Money.toKrw(new BigDecimal("12.05"), new BigDecimal("1382.80"), 1), "12.05 USD");
            TestSupport.eq(-16663L, Money.toKrw(new BigDecimal("-12.05"), new BigDecimal("1382.80"), 1), "refund");
        });

        t.test("yen is quoted per 100", () -> {
            TestSupport.eq(100, CurrencyPairs.unitOf("JPY"), "unit");
            TestSupport.eq(9250L, Money.toKrw(new BigDecimal("1000"), new BigDecimal("925.00"), 100), "1000 JPY");
        });

        t.test("fee truncates fractional won", () -> {
            TestSupport.eq(1234L, Money.fee(102_850, 120), "1.2% of 102,850");
            TestSupport.eq(0L, Money.fee(-5000, 120), "no fee on net refunds");
        });

        t.test("won formatting", () -> TestSupport.eq("1,234,567", Money.won(1_234_567), "format"));

        t.test("csv split handles quotes", () -> {
            TestSupport.eq(List.of("a", "b,c", "d\"e"), Csv.split("a,\"b,c\",\"d\"\"e\""), "split");
        });

        t.test("approval just after KST midnight belongs to next date", () -> {
            BusinessDateResolver r = new BusinessDateResolver(ZoneId.of("Asia/Seoul"));
            TestSupport.eq(LocalDate.of(2026, 10, 3), r.dateOf(Instant.parse("2026-10-02T15:20:00Z")), "KST date");
            TestSupport.eq(LocalDate.of(2026, 10, 2), r.dateOf(Instant.parse("2026-10-02T14:59:59Z")), "KST date");
        });

        t.test("quote line parsing", () -> {
            FxQuote q = FxqClient.parse("EURKRW 2026-10-01 1598.40");
            TestSupport.eq("EURKRW", q.pair(), "pair");
            TestSupport.eq(LocalDate.of(2026, 10, 1), q.date(), "date");
            TestSupport.eq(new BigDecimal("1598.40"), q.rate(), "rate");
        });

        t.test("reconcile flags gross differences and missing rows", () -> {
            LocalDate d = LocalDate.of(2026, 9, 30);
            List<Mismatch> m = new ReconcileService().compare(
                    List.of(new DailyLine(d, "P-1", 2, 1000, 12)),
                    List.of(new StatementRow(d, "P-1", 2, 1001), new StatementRow(d, "P-2", 1, 50)));
            TestSupport.eq(2, m.size(), "mismatch count");
            TestSupport.eq("gross", m.get(0).field(), "first");
            TestSupport.eq("missing", m.get(1).field(), "second");
        });

        t.test("config reads board currencies", () -> {
            TestSupport.eq(List.of("USD", "EUR", "JPY", "CNY"), AppConfig.load().boardCurrencies(), "currencies");
        });

        t.finish();
    }
}
