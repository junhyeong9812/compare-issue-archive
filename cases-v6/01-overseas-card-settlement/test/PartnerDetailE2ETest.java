import api.SettlementApi;
import domain.DailyLine;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Per-transaction partner statement (sample data in fixtures/). */
public class PartnerDetailE2ETest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 5);

    public static void main(String[] args) {
        TestSupport t = new TestSupport("detail");
        SettlementApi api = SettlementApi.fromDefaultConfig();
        String p1 = api.partnerDetailText("P-1001", FROM, TO);

        t.test("first line names the partner and the period", () -> {
            String first = p1.lines().findFirst().orElse("");
            TestSupport.check(first.contains("P-1001") && first.contains("트래블메이트"), "header: " + first);
        });

        t.test("one line per transaction of the partner, nothing else", () -> {
            for (String id : List.of("T0001", "T0004", "T0007", "T0008", "T0011", "T0012", "T0016", "T0018")) {
                TestSupport.check(line(p1, id) != null, "missing " + id + "\n" + p1);
            }
            for (String id : List.of("T0002", "T0013", "T0021")) {
                TestSupport.check(line(p1, id) == null, "should not list " + id + "\n" + p1);
            }
        });

        t.test("weekday lines show the rate and the KRW amount", () -> {
            TestSupport.check(line(p1, "T0001").contains("1,377.30"), line(p1, "T0001"));
            TestSupport.check(line(p1, "T0001").contains("165,965"), line(p1, "T0001"));
            TestSupport.check(line(p1, "T0007").contains("1,386.02"), line(p1, "T0007"));
            TestSupport.check(line(p1, "T0007").contains("45,877"), line(p1, "T0007"));
            String p3 = api.partnerDetailText("P-1003", FROM, TO);
            TestSupport.check(line(p3, "T0003").contains("925.86") && line(p3, "T0003").contains("146,286"), line(p3, "T0003"));
        });

        t.test("KRW per line is the settled amount", () -> {
            TestSupport.check(line(p1, "T0011").contains("82,746"), line(p1, "T0011"));
            TestSupport.check(line(p1, "T0016").contains("125,254"), line(p1, "T0016"));
        });

        t.test("total line matches the daily settlement gross", () -> {
            long gross = api.daily(FROM, TO).stream().filter(l -> l.partnerId().equals("P-1001"))
                    .mapToLong(DailyLine::grossKrw).sum();
            String total = p1.lines().filter(l -> l.startsWith("합계")).findFirst().orElse("");
            TestSupport.check(total.contains("8건"), "count: " + total);
            TestSupport.check(total.contains(util.Money.won(gross)), "total " + util.Money.won(gross) + ": " + total);
        });

        t.finish();
    }

    /** The line that lists the given transaction id, or null. */
    static String line(String text, String txnId) {
        return text.lines().filter(l -> Arrays.asList(l.trim().split("\\s+")).contains(txnId)).findFirst().orElse(null);
    }
}
