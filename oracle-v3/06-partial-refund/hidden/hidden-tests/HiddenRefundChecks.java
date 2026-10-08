import api.RefundDeskApi;
import config.AppConfig;
import domain.CustomerStatement;
import domain.Order;
import domain.RunResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hidden judge: refunds must really be paid by the PG (checked straight through
 * the vendor CLI), one PG refund per returned item, one PG batch per chunk,
 * across other orders, coupons, several runs and small chunk sizes.
 */
public class HiddenRefundChecks {
    static final LocalDate D1 = LocalDate.of(2026, 11, 2);
    static final LocalDate D2 = LocalDate.of(2026, 11, 3);
    static final LocalDate D3 = LocalDate.of(2026, 11, 4);

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("hidden");

        t.test("November returns: PG pays every returned item", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order three = api.placeOrder("C-5003", List.of("SKU-PB10", "SKU-PB10", "SKU-PB10"), 0, D1);
            Order coupon = api.placeOrder("C-5004", List.of("SKU-CB2M", "SKU-CB2M"), 2000, D1);
            Order cases = api.placeOrder("C-5005", List.of("SKU-CASE", "SKU-CASE", "SKU-CASE", "SKU-FILM"), 0, D1);
            Order stand = api.placeOrder("C-5006", List.of("SKU-STND"), 0, D1);
            Order twoReq = api.placeOrder("C-5007", List.of("SKU-PB10", "SKU-PB10", "SKU-AD20"), 0, D1);

            api.requestRefund(three.orderNo(), List.of(1, 2, 3), D2);
            api.requestRefund(coupon.orderNo(), List.of(1, 2), D2);
            api.requestRefund(cases.orderNo(), List.of(1, 3), D2);
            api.requestRefund(stand.orderNo(), List.of(1), D2);
            api.requestRefund(twoReq.orderNo(), List.of(1), D2);
            api.requestRefund(twoReq.orderNo(), List.of(2), D2);
            RunResult r1 = api.runRefunds(D2);
            checkRun(cfg, r1, 10, 1);

            api.requestRefund(cases.orderNo(), List.of(2), D3);
            RunResult r2 = api.runRefunds(D3);
            checkRun(cfg, r2, 1, 1);

            expectPg(cfg, api, three, 47_700, 0);
            expectPg(cfg, api, coupon, 15_800, 0);
            expectPg(cfg, api, cases, 29_700, 7_900);
            expectPg(cfg, api, stand, 11_900, 0);
            expectPg(cfg, api, twoReq, 31_800, 12_900);
            TestSupport.eq(List.of(), api.awaitingPg().stream().map(Object::toString).toList(), "awaiting");
            for (Order o : List.of(three, coupon, cases, stand, twoReq)) checkLines(cfg, api, o);
        });

        t.test("small chunks: one PG call per chunk, items spread over chunks are all paid", () -> {
            AppConfig cfg = TestSupport.sandbox().with("refund.maxLinesPerBatch", "3");
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order a = api.placeOrder("C-6001", List.of("SKU-FILM", "SKU-FILM", "SKU-FILM", "SKU-FILM"), 0, D1);
            Order b = api.placeOrder("C-6002", List.of("SKU-CB1M", "SKU-CB1M", "SKU-CB1M"), 900, D1);
            api.requestRefund(a.orderNo(), List.of(1, 2, 3, 4), D2);
            api.requestRefund(b.orderNo(), List.of(1, 2, 3), D2);
            RunResult r = api.runRefunds(D2);
            checkRun(cfg, r, 7, 3);
            expectPg(cfg, api, a, 31_600, 0);
            expectPg(cfg, api, b, 19_800, 0);
            checkLines(cfg, api, a);
            checkLines(cfg, api, b);
        });

        t.test("sample week: C-2051 power banks and the other customers", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            LocalDate od = LocalDate.of(2026, 10, 7), rd = LocalDate.of(2026, 10, 8);
            Order chargers = api.placeOrder("C-2051", List.of("SKU-PB10", "SKU-PB10", "SKU-CB1M"), 0, od);
            Order caseOrder = api.placeOrder("C-3310", List.of("SKU-AD20", "SKU-CASE"), 0, od);
            Order films = api.placeOrder("C-1188", List.of("SKU-FILM", "SKU-FILM", "SKU-STND"), 1000, od);
            api.requestRefund(chargers.orderNo(), List.of(1, 2), rd);
            api.requestRefund(caseOrder.orderNo(), List.of(2), rd);
            api.requestRefund(films.orderNo(), List.of(1), rd);
            RunResult r = api.runRefunds(rd);
            checkRun(cfg, r, 4, 1);
            expectPg(cfg, api, chargers, 31_800, 6_900);
            expectPg(cfg, api, caseOrder, 9_900, 12_900);
            expectPg(cfg, api, films, 7_615, 19_085);
            for (Order o : List.of(chargers, caseOrder, films)) checkLines(cfg, api, o);
        });

        t.test("items that were not returned are not refunded", () -> {
            AppConfig cfg = TestSupport.sandbox();
            RefundDeskApi api = new RefundDeskApi(cfg);
            Order o = api.placeOrder("C-7001", List.of("SKU-EARB", "SKU-EARB"), 0, D1);
            api.requestRefund(o.orderNo(), List.of(2), D2);
            checkRun(cfg, api.runRefunds(D2), 1, 1);
            expectPg(cfg, api, o, 49_000, 49_000);
            CustomerStatement s = api.statement(o.orderNo());
            TestSupport.eq("구매", s.lines().get(0).state(), "line 1 kept");
        });

        t.finish();
    }

    /** The run went out as `chunks` PG batches whose PG status holds exactly `items` refunds, all distinct. */
    static void checkRun(AppConfig cfg, RunResult r, int items, int chunks) throws Exception {
        TestSupport.eq(items, r.submitted(), r.runId() + " submitted");
        TestSupport.eq(chunks, r.batchIds().size(), r.runId() + " PG calls");
        Set<String> refundNos = new HashSet<>();
        int rows = 0;
        for (String b : r.batchIds()) {
            for (String line : pg(cfg, "status", "--batch", b).split("\n")) {
                if (line.isBlank()) continue;
                rows++;
                refundNos.add(line.split("\t")[0]);
            }
        }
        TestSupport.eq(items, rows, r.runId() + " refunds the PG actually made");
        TestSupport.eq(items, refundNos.size(), r.runId() + " distinct PG refund numbers");
        TestSupport.eq(r.submitted(), r.completed(), r.runId() + " completed");
    }

    static void expectPg(AppConfig cfg, RefundDeskApi api, Order o, long refunded, long balance) throws Exception {
        Map<String, String> kv = new HashMap<>();
        for (String line : pg(cfg, "txn", o.tid()).split("\n")) {
            int sp = line.indexOf(' ');
            if (sp > 0) kv.put(line.substring(0, sp), line.substring(sp + 1).strip());
        }
        TestSupport.eq(Long.toString(refunded), kv.get("refunded"), o.customerId() + " refunded at the PG");
        TestSupport.eq(Long.toString(balance), kv.get("bal"), o.customerId() + " balance at the PG");
        CustomerStatement s = api.statement(o.orderNo());
        TestSupport.eq(refunded, s.refunded(), o.customerId() + " statement refunded");
        TestSupport.eq(balance, s.balance(), o.customerId() + " statement balance");
    }

    /** Every returned line shows a PG refund number that really exists at the PG for this TID. */
    static void checkLines(AppConfig cfg, RefundDeskApi api, Order o) throws Exception {
        Set<String> seen = new HashSet<>();
        for (CustomerStatement.Line l : api.statement(o.orderNo()).lines()) {
            if (l.state().equals("구매")) continue;
            TestSupport.eq("환불 완료", l.state(), o.orderNo() + " line " + l.lineNo());
            TestSupport.check(l.refundNo() != null && seen.add(l.refundNo()),
                    o.orderNo() + " line " + l.lineNo() + " refund number missing or shared: " + l.refundNo());
        }
        List<String> pgRows = new ArrayList<>();
        for (String b : allBatches(cfg)) {
            for (String line : pg(cfg, "status", "--batch", b).split("\n")) {
                if (!line.isBlank() && line.split("\t")[1].equals(o.tid())) pgRows.add(line.split("\t")[0]);
            }
        }
        TestSupport.check(pgRows.containsAll(seen), o.orderNo() + " refund numbers not found at the PG: " + seen);
        TestSupport.eq(seen.size(), pgRows.size(), o.orderNo() + " PG refunds for this payment");
    }

    static List<String> allBatches(AppConfig cfg) throws IOException {
        List<String> ids = new ArrayList<>();
        Path f = cfg.pgHome().resolve("bat.dat");
        if (!java.nio.file.Files.exists(f)) return ids;
        for (String line : java.nio.file.Files.readAllLines(f)) if (!line.isBlank()) ids.add(line.split("\\|")[0]);
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
