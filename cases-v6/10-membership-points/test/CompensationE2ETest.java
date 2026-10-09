import api.PointsApi;
import config.AppConfig;

import java.time.LocalDate;
import java.time.YearMonth;

/** Customer-care compensation points: credited once per ticket, visible in the member app. */
public class CompensationE2ETest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("compensation");
        AppConfig cfg = TestSupport.sandbox();
        PointsApi api = new PointsApi(cfg);
        api.runDay(DAY);

        t.test("compensation shows up in the member's balance and entry count", () -> {
            api.compensate("004512", 3_000, "CS-20261007-0012");
            TestSupport.eq(5_068L, api.statement("004512", OCT).balance(), "balance of 004512");
            TestSupport.eq(3, api.statement("004512", OCT).entries(), "entries of 004512");
        });

        t.test("store-card numbers keep their leading zeros", () -> {
            api.compensate("000873", 500, "CS-20261007-0013");
            TestSupport.eq(739L, api.statement("000873", OCT).balance(), "balance of 000873");
        });

        t.test("the same ticket pays only once", () -> {
            try {
                api.compensate("004512", 3_000, "CS-20261007-0012");
            } catch (RuntimeException expected) {
                // refusing the second request is fine
            }
            TestSupport.eq(5_068L, api.statement("004512", OCT).balance(), "balance of 004512 after a repeat");
        });

        t.test("unknown member and non-positive points are refused", () -> {
            boolean unknown = false;
            try {
                api.compensate("999999", 1_000, "CS-20261007-0014");
            } catch (RuntimeException e) {
                unknown = true;
            }
            TestSupport.check(unknown, "unknown member must be refused");
            boolean zero = false;
            try {
                api.compensate("4512", 0, "CS-20261007-0015");
            } catch (RuntimeException e) {
                zero = true;
            }
            TestSupport.check(zero, "0 points must be refused");
            TestSupport.eq(690L, api.statement("4512", OCT).balance(), "balance of 4512");
        });

        t.test("other members are untouched", () -> {
            TestSupport.eq(5_000L, api.statement("87310", OCT).balance(), "87310");
            TestSupport.eq(1_780L, api.statement("031207", OCT).balance(), "031207");
        });

        t.test("the earn run still earns nothing twice", () ->
                TestSupport.eq(0, api.runDay(DAY).earned(), "earned on re-run"));

        t.finish();
    }
}
