import api.SettlementApi;
import domain.DailyLine;
import domain.FxQuote;
import service.ReconcileService.Mismatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** End-to-end checks through the public facade, against the sample data in fixtures/. */
public class SettlementE2ETest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 5);

    public static void main(String[] args) {
        TestSupport t = new TestSupport("e2e");
        SettlementApi api = SettlementApi.fromDefaultConfig();

        t.test("settlement for 10/01-10/05 matches every partner statement", () -> {
            List<Mismatch> m = api.reconcile(FROM, TO);
            TestSupport.check(m.isEmpty(), "settlement differs from partner statements:\n"
                    + api.reconcileReport(FROM, TO));
        });

        t.test("partner P-1001 is paid the statement amount for 10/03", () -> {
            DailyLine l = line(api.daily(FROM, TO), LocalDate.of(2026, 10, 3), "P-1001");
            TestSupport.eq(3, l.count(), "transaction count");
            TestSupport.eq(524_145L, l.grossKrw(), "gross KRW");
            TestSupport.eq(517_856L, l.netKrw(), "net KRW after fee");
        });

        t.test("weekday settlement is unchanged", () -> {
            DailyLine l = line(api.daily(FROM, TO), LocalDate.of(2026, 10, 2), "P-1002");
            TestSupport.eq(397_234L, l.grossKrw(), "gross KRW 10/02 P-1002");
        });

        t.test("inactive partners are not settled", () -> {
            TestSupport.check(api.daily(FROM, TO).stream().noneMatch(l -> l.partnerId().equals("P-1004")),
                    "P-1004 should be excluded");
        });

        t.test("accounting export has one row per partner per day", () -> {
            String csv = api.exportCsv(FROM, TO);
            TestSupport.eq(16L, csv.lines().count(), "csv lines incl. header");
        });

        t.test("app rate board shows the published quotes", () -> {
            List<FxQuote> b = api.rateBoard(LocalDate.of(2026, 10, 3));
            TestSupport.eq(4, b.size(), "board size");
            TestSupport.eq(new BigDecimal("1386.02"), b.get(0).rate(), "USD on board");
            TestSupport.eq(new BigDecimal("923.98"), b.get(2).rate(), "JPY on board");
            TestSupport.check(api.rateBoardText(LocalDate.of(2026, 10, 3)).contains("1,386.02원"), "board text");
        });

        t.finish();
    }

    private static DailyLine line(List<DailyLine> lines, LocalDate d, String partner) {
        return lines.stream().filter(l -> l.date().equals(d) && l.partnerId().equals(partner)).findFirst()
                .orElseThrow(() -> new AssertionError("no settlement line for " + d + " " + partner));
    }
}
