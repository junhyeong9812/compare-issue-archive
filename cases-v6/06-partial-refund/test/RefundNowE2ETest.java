import api.RefundDeskApi;
import domain.CustomerStatement;
import domain.Order;
import domain.RunResult;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** CS console "즉시 환불": returned items go to the PG right away, other requests wait for the nightly run. */
public class RefundNowE2ETest {
    private static final LocalDate ORDER_DAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("refund-now");
        RefundDeskApi api = new RefundDeskApi(TestSupport.sandbox());

        Order chargers = api.placeOrder("C-2051", List.of("SKU-PB10", "SKU-PB10", "SKU-CB1M"), 0, ORDER_DAY);
        Order caseOrder = api.placeOrder("C-3310", List.of("SKU-AD20", "SKU-CASE"), 0, ORDER_DAY);
        Order films = api.placeOrder("C-1188", List.of("SKU-FILM", "SKU-FILM", "SKU-STND"), 1000, ORDER_DAY);

        api.requestRefund(caseOrder.orderNo(), List.of(2), DAY);
        String id = api.refundNow(chargers.orderNo(), List.of(1, 2), DAY);

        t.test("refundNow returns a request id", () -> TestSupport.check(id != null && id.startsWith("RQ"), "id " + id));

        t.test("returned power banks are refunded by the PG right away", () -> {
            CustomerStatement s = api.statement(chargers.orderNo());
            TestSupport.eq(31_800L, s.refunded(), "refunded by the PG");
            TestSupport.eq(6_900L, s.balance(), "balance");
            Set<String> nos = new HashSet<>();
            for (CustomerStatement.Line l : s.lines()) {
                if (l.lineNo() == 3) {
                    TestSupport.eq("구매", l.state(), "cable kept");
                    continue;
                }
                TestSupport.eq("환불 완료", l.state(), "line " + l.lineNo());
                TestSupport.check(l.refundNo() != null && nos.add(l.refundNo()), "refund number " + l.refundNo());
            }
        });

        t.test("an ordinary request still waits for the nightly run", () -> {
            CustomerStatement s = api.statement(caseOrder.orderNo());
            TestSupport.eq(0L, s.refunded(), "not refunded yet");
            TestSupport.eq("환불 접수", s.lines().get(1).state(), "state");
        });

        t.test("coupon order: the line's paid amount is refunded at once", () -> {
            api.refundNow(films.orderNo(), List.of(1), DAY);
            TestSupport.eq(7_615L, api.statement(films.orderNo()).refunded(), "refunded");
        });

        t.test("an item cannot be refunded twice", () -> {
            try {
                api.refundNow(chargers.orderNo(), List.of(1), DAY);
                throw new AssertionError("second refund of the same item was accepted");
            } catch (IllegalStateException expected) {
                // ok
            }
        });

        t.test("the nightly run sends only what is left", () -> {
            RunResult run = api.runRefunds(DAY);
            TestSupport.eq(1, run.submitted(), "submitted");
            TestSupport.eq(9_900L, api.statement(caseOrder.orderNo()).refunded(), "C-3310 refunded");
            TestSupport.eq(31_800L, api.statement(chargers.orderNo()).refunded(), "C-2051 unchanged");
            TestSupport.eq(List.of(), api.awaitingPg().stream().map(Object::toString).toList(), "awaiting");
        });

        t.finish();
    }
}
