import domain.Grade;
import domain.Member;
import domain.Payment;
import service.EarnPolicy;
import service.MemberDirectory;
import service.PaymentDeduper;
import util.Fmt;

import java.util.List;

public class UnitChecks {
    public static void main(String[] args) {
        TestSupport t = new TestSupport("unit");

        t.test("grade rates round down", () -> {
            EarnPolicy p = new EarnPolicy(5000);
            TestSupport.eq(148L, p.points(9_900, Grade.GOLD), "gold 1.5% of 9,900");
            TestSupport.eq(540L, p.points(54_000, Grade.BASIC), "basic 1%");
            TestSupport.eq(1_780L, p.points(89_000, Grade.VIP), "vip 2%");
            TestSupport.eq(0L, p.points(99, Grade.BASIC), "below 1 point");
        });

        t.test("cap per payment", () ->
                TestSupport.eq(5_000L, new EarnPolicy(5000).points(1_250_000, Grade.BASIC), "cap"));

        t.test("member numbers are looked up as issued", () -> {
            MemberDirectory d = new MemberDirectory(List.of(
                    new Member("004512", "A", Grade.GOLD, "CARD"),
                    new Member("4512", "B", Grade.BASIC, "APP"),
                    new Member("C7781", "C", Grade.BASIC, "CORP")));
            TestSupport.eq("A", d.find("004512").orElseThrow().name(), "004512");
            TestSupport.eq("B", d.find("4512").orElseThrow().name(), "4512");
            TestSupport.eq("C", d.find("c7781").orElseThrow().name(), "corporate code case");
            TestSupport.check(d.find("04512").isEmpty(), "04512 is nobody");
        });

        t.test("resent acquirer rows are counted once", () -> {
            Payment a = new Payment("P1", "4512", "m", 1000, "APPROVED");
            Payment b = new Payment("P2", "4512", "m", 1000, "APPROVED");
            TestSupport.eq(List.of(a, b), PaymentDeduper.unique(List.of(a, b, a)), "unique");
        });

        t.test("formatting", () -> {
            TestSupport.eq("2,068P", Fmt.points(2068), "points");
            TestSupport.eq("**4512", Fmt.mask("004512"), "mask 6");
            TestSupport.eq("205", Fmt.mask("205"), "mask short");
        });

        t.finish();
    }
}
