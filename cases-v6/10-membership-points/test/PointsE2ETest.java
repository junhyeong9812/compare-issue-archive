import api.PointsApi;
import domain.MemberStatement;
import domain.RunResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** End-to-end: the 10/06 acquirer export through the nightly earn run, then what members see. */
public class PointsE2ETest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("e2e");
        PointsApi api = new PointsApi(TestSupport.sandbox());
        RunResult run = api.runDay(DAY);

        t.test("10/06 run earns every approved payment of a known member once", () -> {
            TestSupport.eq(10, run.earned(), "earned payments");
            TestSupport.eq(List.of("999999"), run.unknownMembers(), "unknown members");
        });

        t.test("김서연 (store card 004512) sees the 2,068P she earned", () ->
                TestSupport.eq(2_068L, api.statement("004512", OCT).balance(), "balance of 004512"));

        t.test("박지훈 (app 4512) sees only his own 690P", () ->
                TestSupport.eq(690L, api.statement("4512", OCT).balance(), "balance of 4512"));

        t.test("every member's balance equals the points earned from their own payments", () -> {
            Map<String, Long> expected = new LinkedHashMap<>();
            expected.put("4512", 690L);
            expected.put("004512", 2_068L);
            expected.put("87310", 5_000L);
            expected.put("000873", 239L);
            expected.put("120045", 999L);
            expected.put("C7781", 5_000L);
            expected.put("031207", 1_780L);
            expected.put("31207", 420L);
            expected.put("205", 0L);
            List<String> wrong = new ArrayList<>();
            for (Map.Entry<String, Long> e : expected.entrySet()) {
                MemberStatement s = api.statement(e.getKey(), OCT);
                if (s.balance() != e.getValue()) wrong.add(e.getKey() + " " + s.balance() + "P (expected " + e.getValue() + "P)");
            }
            TestSupport.eq(List.of(), wrong, "balances");
        });

        t.test("VIP and corporate payments are capped at 5,000P", () -> {
            TestSupport.eq(5_000L, api.statement("87310", OCT).balance(), "87310");
            TestSupport.eq(5_000L, api.statement("C7781", OCT).balance(), "C7781");
        });

        t.test("cancelled payments earn nothing", () -> {
            MemberStatement s = api.statement("205", OCT);
            TestSupport.eq(0L, s.balance(), "205 balance");
            TestSupport.eq(0, s.entries(), "205 entries");
        });

        t.test("re-running the same day earns nothing twice", () -> {
            RunResult again = api.runDay(DAY);
            TestSupport.eq(0, again.earned(), "earned on re-run");
            TestSupport.eq(5_000L, api.statement("87310", OCT).balance(), "87310 after re-run");
        });

        t.test("statement text for 이도윤", () -> {
            String text = api.statementText("87310", OCT);
            TestSupport.check(text.contains("이도윤님 (*7310, VIP)"), text);
            TestSupport.check(text.contains("포인트 잔액 5,000P · 이번 달 적립 5,000P · 적립 1건"), text);
        });

        t.test("monthly top earners", () -> {
            String text = api.topEarnersText(OCT, 3);
            TestSupport.check(text.contains(" 1. *7310    이도윤 5,000P"), text);
            TestSupport.check(text.contains(" 2. *7781    한빛상사 5,000P"), text);
            TestSupport.check(text.contains(" 3. **4512   김서연 2,068P"), text);
        });

        t.finish();
    }
}
