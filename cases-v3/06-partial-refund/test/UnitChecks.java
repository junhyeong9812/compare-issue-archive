import domain.RefundLine;
import infra.PgsimClient.RefundRow;
import infra.RefundFileWriter;
import service.DiscountAllocator;
import service.RefundResultMatcher;
import util.Money;
import util.Refs;

import java.time.LocalDate;
import java.util.List;

public class UnitChecks {
    public static void main(String[] args) {
        TestSupport t = new TestSupport("unit");
        LocalDate d = LocalDate.of(2026, 10, 8);

        t.test("coupon is spread by price and adds up", () -> {
            List<Long> a = DiscountAllocator.allocate(List.of(7900L, 7900L, 11900L), 1000);
            TestSupport.eq(List.of(285L, 285L, 430L), a, "allocation");
            TestSupport.eq(1000L, a.stream().mapToLong(Long::longValue).sum(), "sum");
        });

        t.test("no coupon, no discount", () ->
                TestSupport.eq(List.of(0L, 0L), DiscountAllocator.allocate(List.of(15900L, 6900L), 0), "allocation"));

        t.test("won formatting", () -> {
            TestSupport.eq("31,800원", Money.won(31800), "won");
            TestSupport.eq("0원", Money.won(0), "zero");
        });

        t.test("PG reference keeps only allowed characters", () -> {
            TestSupport.eq("HB20261007-0001", Refs.sanitize("HB20261007-0001"), "order no");
            TestSupport.eq("A-B-C", Refs.sanitize("A_B C"), "separators");
            TestSupport.check(Refs.sanitize("X".repeat(40)).length() <= Refs.MAX, "length");
        });

        t.test("refund file has TID, amount and reference per line", () -> {
            RefundLine l = new RefundLine("RQ00001", "HB20261007-0002", 2, "T00000002", 9900, d);
            String[] cols = RefundFileWriter.format(List.of(l)).strip().split("\t");
            TestSupport.eq(3, cols.length, "columns");
            TestSupport.eq("T00000002", cols[0], "tid");
            TestSupport.eq("9900", cols[1], "amount");
            TestSupport.eq(RefundFileWriter.merchantRef(l), cols[2], "reference");
        });

        t.test("status rows are matched to the lines that were sent", () -> {
            RefundLine a = new RefundLine("RQ00001", "HB20261007-0003", 1, "T00000003", 49000, d);
            RefundLine b = new RefundLine("RQ00002", "HB20261007-0002", 2, "T00000002", 9900, d);
            a.submitted("RB20261008-01-1");
            b.submitted("RB20261008-01-1");
            List<RefundRow> rows = List.of(
                    new RefundRow("RF00000007", "T00000002", 9900, RefundFileWriter.merchantRef(b)),
                    new RefundRow("RF00000008", "T00000003", 49000, RefundFileWriter.merchantRef(a)));
            TestSupport.eq(2, RefundResultMatcher.apply(List.of(a, b), rows), "matched");
            TestSupport.eq("RF00000008", a.refundNo(), "a");
            TestSupport.eq("RF00000007", b.refundNo(), "b");
            TestSupport.eq(RefundLine.Status.DONE, b.status(), "status");
        });

        t.finish();
    }
}
