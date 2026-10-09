import api.RefundDeskApi;
import config.AppConfig;
import domain.CustomerStatement;
import domain.Order;
import domain.RunResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hidden judge for "즉시 환불": the request (items of an already captured payment are refunded by the PG at once,
 * other requests wait) and the decision invariant: no refund reaches the PG on the KST date the payment was made,
 * whether it came through the instant path or the nightly run; such items are still refunded by a later run.
 * Money is read straight from the vendor CLI.
 */
public class HiddenNowChecks {
    static final LocalDate D1 = LocalDate.of(2026, 11, 2);
    static final LocalDate D2 = LocalDate.of(2026, 11, 3);
    static final LocalDate D3 = LocalDate.of(2026, 11, 4);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("hidden-now");

        t.test("instant refund of a payment made the same day does not reach the PG that day", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order quick = api.placeOrder("C-8101", List.of("SKU-PB10", "SKU-PB10", "SKU-CB1M"), 0, D1);
            Order quick2 = api.placeOrder("C-8102", List.of("SKU-EARB"), 0, D1);
            instant(api, quick, List.of(1), D1);
            instant(api, quick2, List.of(1), D1);
            pgRefunded(cfg, quick, 0, "right after refundNow on the payment day");
            pgRefunded(cfg, quick2, 0, "right after refundNow on the payment day");
            api.runRefunds(D1);
            pgRefunded(cfg, quick, 0, "after the nightly run of the payment day");
            pgRefunded(cfg, quick2, 0, "after the nightly run of the payment day");
            RunResult next = api.runRefunds(D2);
            pgRefunded(cfg, quick, 15_900, "after the next day's run");
            pgRefunded(cfg, quick2, 49_000, "after the next day's run");
            TestSupport.eq(next.submitted(), next.completed(), "next run completed");
            done(api, quick, 1);
            done(api, quick2, 1);
            distinctPgRefunds(cfg, quick, 1);
        });

        t.test("instant refund of an earlier payment is paid at once, ordinary requests wait", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order a = api.placeOrder("C-8201", List.of("SKU-CASE", "SKU-CASE", "SKU-FILM"), 0, D1);
            Order b = api.placeOrder("C-8202", List.of("SKU-CB2M", "SKU-CB2M"), 2000, D1);
            Order c = api.placeOrder("C-8203", List.of("SKU-STND"), 0, D1);
            api.requestRefund(c.orderNo(), List.of(1), D2);
            instant(api, a, List.of(1, 2), D2);
            instant(api, b, List.of(2), D2);
            pgRefunded(cfg, a, 19_800, "instant, paid the day before");
            pgRefunded(cfg, b, 7_900, "instant with coupon");
            pgRefunded(cfg, c, 0, "ordinary request before the nightly run");
            done(api, a, 1);
            done(api, a, 2);
            done(api, b, 2);
            distinctPgRefunds(cfg, a, 2);
            api.runRefunds(D2);
            pgRefunded(cfg, c, 11_900, "ordinary request after the nightly run");
            pgRefunded(cfg, a, 19_800, "instant refund not repeated by the run");
            batchIdsUnique(cfg);
        });

        t.test("nightly run holds payments of its own day and sends them the next day", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order old = api.placeOrder("C-8301", List.of("SKU-AD20", "SKU-AD20"), 0, D1);
            Order fresh = api.placeOrder("C-8302", List.of("SKU-AD20", "SKU-AD20"), 0, D2);
            api.requestRefund(old.orderNo(), List.of(1, 2), D2);
            api.requestRefund(fresh.orderNo(), List.of(2), D2);
            api.runRefunds(D2);
            pgRefunded(cfg, old, 25_800, "earlier payment, run of D2");
            pgRefunded(cfg, fresh, 0, "same-day payment, run of D2");
            api.runRefunds(D3);
            pgRefunded(cfg, fresh, 12_900, "same-day payment, run of D3");
            done(api, fresh, 2);
            batchIdsUnique(cfg);
        });

        t.test("same customer, two orders: only the captured one goes out at once", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order yesterday = api.placeOrder("C-8401", List.of("SKU-FILM", "SKU-FILM"), 0, D1);
            Order today = api.placeOrder("C-8401", List.of("SKU-FILM", "SKU-FILM"), 0, D2);
            instant(api, today, List.of(1, 2), D2);
            instant(api, yesterday, List.of(1, 2), D2);
            pgRefunded(cfg, yesterday, 15_800, "yesterday's order");
            pgRefunded(cfg, today, 0, "today's order");
            api.runRefunds(D2);
            pgRefunded(cfg, today, 0, "today's order after tonight's run");
            api.runRefunds(D3);
            pgRefunded(cfg, today, 15_800, "today's order next day");
            distinctPgRefunds(cfg, today, 2);
            done(api, today, 1);
            done(api, today, 2);
        });

        t.finish();
    }

    /** refundNow; an implementation may refuse a same-day payment, then the item is queued the ordinary way. */
    static void instant(RefundDeskApi api, Order o, List<Integer> lines, LocalDate day) {
        try {
            api.refundNow(o.orderNo(), lines, day);
        } catch (RuntimeException e) {
            boolean queued = api.statement(o.orderNo()).lines().stream()
                    .anyMatch(l -> lines.contains(l.lineNo()) && !l.state().equals("구매"));
            if (!queued) api.requestRefund(o.orderNo(), lines, day);
        }
    }

    static void pgRefunded(AppConfig cfg, Order o, long expected, String when) throws Exception {
        Map<String, String> kv = new HashMap<>();
        for (String line : pg(cfg, "txn", o.tid()).split("\n")) {
            int sp = line.indexOf(' ');
            if (sp > 0) kv.put(line.substring(0, sp), line.substring(sp + 1).strip());
        }
        TestSupport.eq(Long.toString(expected), kv.get("refunded"), o.customerId() + " " + o.orderNo() + " refunded at the PG " + when);
    }

    static void done(RefundDeskApi api, Order o, int lineNo) {
        CustomerStatement.Line l = api.statement(o.orderNo()).lines().stream().filter(x -> x.lineNo() == lineNo)
                .findFirst().orElseThrow();
        TestSupport.eq("환불 완료", l.state(), o.orderNo() + " line " + lineNo);
        TestSupport.check(l.refundNo() != null, o.orderNo() + " line " + lineNo + " has no refund number");
    }

    /** The PG holds exactly n refunds for this payment, and the statement shows exactly those numbers. */
    static void distinctPgRefunds(AppConfig cfg, Order o, int n) throws Exception {
        List<String> pgRows = new ArrayList<>();
        for (String b : allBatches(cfg)) {
            for (String line : pg(cfg, "status", "--batch", b).split("\n")) {
                if (!line.isBlank() && line.split("\t")[1].equals(o.tid())) pgRows.add(line.split("\t")[0]);
            }
        }
        TestSupport.eq(n, pgRows.size(), o.orderNo() + " PG refunds");
        TestSupport.eq(n, new HashSet<>(pgRows).size(), o.orderNo() + " distinct PG refund numbers");
    }

    static void batchIdsUnique(AppConfig cfg) throws Exception {
        List<String> ids = allBatches(cfg);
        Set<String> uniq = new HashSet<>(ids);
        TestSupport.eq(ids.size(), uniq.size(), "PG batch ids reused: " + ids);
    }

    static List<String> allBatches(AppConfig cfg) throws Exception {
        List<String> ids = new ArrayList<>();
        Path f = cfg.pgHome().resolve("bat.dat");
        if (!Files.exists(f)) return ids;
        for (String line : Files.readAllLines(f)) if (!line.isBlank()) ids.add(line.split("\\|")[0]);
        return ids;
    }

    static String pg(AppConfig cfg, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.pgBin().toString());
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PGSIM_HOME", cfg.pgHome().toAbsolutePath().toString());
        Process p = pb.start();
        String out;
        String err;
        try (InputStream o = p.getInputStream(); InputStream e = p.getErrorStream()) {
            out = new String(o.readAllBytes(), StandardCharsets.UTF_8);
            err = new String(e.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (p.waitFor() != 0) throw new AssertionError("pgsim " + String.join(" ", args) + " failed: " + err.strip());
        return out;
    }
}
