import api.RefundDeskApi;
import domain.CustomerStatement;
import domain.Order;
import domain.RunResult;

import java.time.LocalDate;
import java.util.List;

/** End-to-end: orders paid through the PG sandbox, CS refund requests, nightly refund run, customer statement. */
public class RefundE2ETest {
    private static final LocalDate ORDER_DAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate REFUND_DAY = LocalDate.of(2026, 10, 8);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("e2e");
        RefundDeskApi api = new RefundDeskApi(TestSupport.sandbox());

        Order chargers = api.placeOrder("C-2051", List.of("SKU-PB10", "SKU-PB10", "SKU-CB1M"), 0, ORDER_DAY);
        Order caseOrder = api.placeOrder("C-3310", List.of("SKU-AD20", "SKU-CASE"), 0, ORDER_DAY);
        Order earbuds = api.placeOrder("C-4127", List.of("SKU-EARB"), 0, ORDER_DAY);
        Order films = api.placeOrder("C-1188", List.of("SKU-FILM", "SKU-FILM", "SKU-STND"), 1000, ORDER_DAY);

        api.requestRefund(chargers.orderNo(), List.of(1, 2), REFUND_DAY);
        api.requestRefund(caseOrder.orderNo(), List.of(2), REFUND_DAY);
        api.requestRefund(earbuds.orderNo(), List.of(1), REFUND_DAY);
        api.requestRefund(films.orderNo(), List.of(1), REFUND_DAY);
        RunResult run = api.runRefunds(REFUND_DAY);

        t.test("nightly refund run submits every requested item", () -> {
            TestSupport.eq(5, run.submitted(), "items submitted");
            TestSupport.eq(1, run.batchIds().size(), "PG batches");
        });

        t.test("customer C-2051 gets both returned power banks back (31,800원)", () -> {
            CustomerStatement s = api.statement(chargers.orderNo());
            TestSupport.eq(38_700L, s.paid(), "paid");
            TestSupport.eq(31_800L, s.refunded(), "refunded to the customer by the PG");
            TestSupport.eq(6_900L, s.balance(), "remaining payment");
        });

        t.test("every returned item shows 환불 완료 with a PG refund number", () -> {
            for (Order o : List.of(chargers, caseOrder, earbuds, films)) {
                for (CustomerStatement.Line l : api.statement(o.orderNo()).lines()) {
                    if (l.state().equals("구매")) continue;
                    TestSupport.eq("환불 완료", l.state(), o.orderNo() + " line " + l.lineNo());
                    TestSupport.check(l.refundNo() != null, o.orderNo() + " line " + l.lineNo() + " has no refund number");
                }
            }
        });

        t.test("nothing is left waiting at the PG after the nightly run", () ->
                TestSupport.eq(List.of(), api.awaitingPg().stream().map(Object::toString).toList(), "awaiting"));

        t.test("other customers' partial refunds are paid", () -> {
            TestSupport.eq(9_900L, api.statement(caseOrder.orderNo()).refunded(), "C-3310 case");
            TestSupport.eq(49_000L, api.statement(earbuds.orderNo()).refunded(), "C-4127 earbuds");
            TestSupport.eq(7_615L, api.statement(films.orderNo()).refunded(), "C-1188 one film after coupon");
            TestSupport.eq(0L, api.statement(earbuds.orderNo()).balance(), "C-4127 balance");
        });

        t.test("statement text for C-3310", () -> {
            String text = api.statementText(caseOrder.orderNo());
            TestSupport.check(text.contains("투명 젤리 케이스  9,900원  환불 완료 [RF"), text);
            TestSupport.check(text.contains("결제 22,800원 / 환불 9,900원 / 잔액 12,900원"), text);
        });

        t.test("an item cannot be refunded twice", () -> {
            try {
                api.requestRefund(caseOrder.orderNo(), List.of(2), REFUND_DAY.plusDays(1));
                throw new AssertionError("second refund of the same item was accepted");
            } catch (IllegalStateException expected) {
                // ok
            }
        });

        t.test("a run with nothing pending sends nothing", () -> {
            RunResult again = api.runRefunds(REFUND_DAY.plusDays(1));
            TestSupport.eq(0, again.submitted(), "submitted");
            TestSupport.eq(List.of(), again.batchIds(), "batches");
        });

        t.finish();
    }
}
