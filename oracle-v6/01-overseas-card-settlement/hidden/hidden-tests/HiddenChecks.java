import api.SettlementApi;
import config.AppConfig;
import domain.DailyLine;
import domain.FxQuote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Hidden judge: another week of data (other currencies, other dates) plus regression of the sample week. */
public class HiddenChecks {
    public static void main(String[] args) {
        TestSupport t = new TestSupport("hidden");

        System.setProperty("settle.data.dir", "hidden-fixtures");
        SettlementApi wk2 = new SettlementApi(AppConfig.load());
        LocalDate from = LocalDate.of(2026, 10, 6), to = LocalDate.of(2026, 10, 12);

        t.test("week of 10/06-10/12 matches partner statements", () ->
                TestSupport.check(wk2.reconcile(from, to).isEmpty(), "mismatches:\n" + wk2.reconcileReport(from, to)));

        List<DailyLine> d = wk2.daily(from, to);
        t.test("10/09 JPY partner gross", () -> TestSupport.eq(1_091_299L, line(d, "2026-10-09", "P-1003").grossKrw(), "gross"));
        t.test("10/09 USD partner gross", () -> TestSupport.eq(1_027_686L, line(d, "2026-10-09", "P-1001").grossKrw(), "gross"));
        t.test("10/09 CNY partner gross", () -> TestSupport.eq(637_103L, line(d, "2026-10-09", "P-1005").grossKrw(), "gross"));
        t.test("10/10 EUR partner gross", () -> TestSupport.eq(3_011_344L, line(d, "2026-10-10", "P-1002").grossKrw(), "gross"));
        t.test("10/11 JPY+USD partner gross", () -> TestSupport.eq(2_312_100L, line(d, "2026-10-11", "P-1002").grossKrw(), "gross"));
        t.test("10/11 net after fee", () -> TestSupport.eq(1_273_234L, line(d, "2026-10-11", "P-1001").netKrw(), "net"));
        t.test("10/12 weekday unchanged", () -> TestSupport.eq(213_502L, line(d, "2026-10-12", "P-1001").grossKrw(), "gross"));
        t.test("10/08 late-night approval stays on 10/08", () -> TestSupport.eq(159_342L, line(d, "2026-10-08", "P-1001").grossKrw(), "gross"));

        for (String day : List.of("2026-10-09", "2026-10-10", "2026-10-11")) {
            t.test("rate board " + day + " shows published quotes", () -> {
                List<FxQuote> b = wk2.rateBoard(LocalDate.parse(day));
                TestSupport.eq(new BigDecimal("1377.48"), b.get(0).rate(), "USD");
                TestSupport.eq(new BigDecimal("1609.52"), b.get(1).rate(), "EUR");
                TestSupport.eq(new BigDecimal("931.58"), b.get(2).rate(), "JPY");
                TestSupport.eq(new BigDecimal("190.26"), b.get(3).rate(), "CNY");
            });
        }

        // same feed before the partners have sent their statements
        System.setProperty("settle.data.dir", "hidden-fixtures-open");
        List<DailyLine> open = new SettlementApi(AppConfig.load()).daily(from, to);
        t.test("pre-statement 10/09 EUR+USD partner gross", () -> TestSupport.eq(3_044_386L, line(open, "2026-10-09", "P-1002").grossKrw(), "gross"));
        t.test("pre-statement 10/11 CNY partner gross", () -> TestSupport.eq(1_409_132L, line(open, "2026-10-11", "P-1005").grossKrw(), "gross"));
        t.test("pre-statement 10/09 JPY partner gross", () -> TestSupport.eq(1_091_299L, line(open, "2026-10-09", "P-1003").grossKrw(), "gross"));

        System.setProperty("settle.data.dir", "fixtures");
        SettlementApi wk1 = new SettlementApi(AppConfig.load());
        LocalDate f1 = LocalDate.of(2026, 10, 1), t1 = LocalDate.of(2026, 10, 5);
        t.test("sample week still matches", () ->
                TestSupport.check(wk1.reconcile(f1, t1).isEmpty(), "mismatches:\n" + wk1.reconcileReport(f1, t1)));
        t.test("sample week board 10/04", () -> TestSupport.eq(new BigDecimal("1386.02"),
                wk1.rateBoard(LocalDate.of(2026, 10, 4)).get(0).rate(), "USD"));

        t.finish();
    }

    private static DailyLine line(List<DailyLine> lines, String date, String partner) {
        LocalDate d = LocalDate.parse(date);
        return lines.stream().filter(l -> l.date().equals(d) && l.partnerId().equals(partner)).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + date + " " + partner));
    }
}
