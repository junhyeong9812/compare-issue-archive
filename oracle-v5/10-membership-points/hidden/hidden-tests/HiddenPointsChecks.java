import api.PointsApi;
import config.AppConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hidden judge: points must sit on the member's own account at the points vendor
 * (checked straight through the vendor CLI), for other members and another day,
 * with no points parked on accounts nobody owns.
 */
public class HiddenPointsChecks {
    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("hidden");

        t.test("November day: every member's vendor balance is their own points", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            api.runDay(LocalDate.of(2026, 11, 2));
            Map<String, Long> want = new LinkedHashMap<>();
            want.put("0000417", 1_095L + 96L);
            want.put("417", 120L);
            want.put("010", 3_000L);
            want.put("000000", 450L);
            want.put("100", 88L);
            want.put("C0012", 3_960L);
            want.put("0A9917", 300L);
            want.put("004512", 300L);
            want.put("4512", 0L);
            checkVendor(cfg, api, want, YearMonth.of(2026, 11));
            for (String nobody : List.of("10", "0", "0417", "4512X")) {
                TestSupport.eq(0L, vendorBalance(cfg, nobody), "points parked on non-member account " + nobody);
            }
            TestSupport.eq(List.of("E-PAY-1102-0001", "E-PAY-1102-0008"), refs(cfg, "0000417"), "0000417 history");
            TestSupport.eq(List.of("E-PAY-1102-0002"), refs(cfg, "417"), "417 history");
        });

        t.test("sample day 10/06: vendor balances per member", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            api.runDay(LocalDate.of(2026, 10, 6));
            Map<String, Long> want = new LinkedHashMap<>();
            want.put("4512", 690L);
            want.put("004512", 2_068L);
            want.put("87310", 5_000L);
            want.put("000873", 239L);
            want.put("120045", 999L);
            want.put("C7781", 5_000L);
            want.put("031207", 1_780L);
            want.put("31207", 420L);
            want.put("205", 0L);
            checkVendor(cfg, api, want, YearMonth.of(2026, 10));
            for (String nobody : List.of("873", "31207X")) {
                TestSupport.eq(0L, vendorBalance(cfg, nobody), "points parked on non-member account " + nobody);
            }
        });

        t.test("two days in a row add up on the right accounts", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            api.runDay(LocalDate.of(2026, 10, 6));
            api.runDay(LocalDate.of(2026, 11, 2));
            api.runDay(LocalDate.of(2026, 11, 2));
            TestSupport.eq(2_368L, vendorBalance(cfg, "004512"), "004512 over two days");
            TestSupport.eq(690L, vendorBalance(cfg, "4512"), "4512 over two days");
            TestSupport.eq(2_368L, api.statement("004512", YearMonth.of(2026, 11)).balance(), "statement 004512");
            TestSupport.eq(3, api.statement("004512", YearMonth.of(2026, 11)).entries(), "entries 004512");
        });

        t.finish();
    }

    static void checkVendor(AppConfig cfg, PointsApi api, Map<String, Long> want, YearMonth month) throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Long> e : want.entrySet()) {
            long vendor = vendorBalance(cfg, e.getKey());
            long shown = api.statement(e.getKey(), month).balance();
            if (vendor != e.getValue() || shown != vendor) {
                wrong.add(e.getKey() + " vendor=" + vendor + " shown=" + shown + " want=" + e.getValue());
            }
        }
        TestSupport.eq(List.of(), wrong, "balances");
    }

    static long vendorBalance(AppConfig cfg, String account) throws Exception {
        String[] t = pts(cfg, "bal", account).strip().split("\\s+");
        TestSupport.eq(account, t[0], "bal echoes the account");
        return Long.parseLong(t[1]);
    }

    static List<String> refs(AppConfig cfg, String account) throws Exception {
        List<String> out = new ArrayList<>();
        for (String line : pts(cfg, "hist", account).split("\n")) {
            if (!line.isBlank()) out.add(line.split("\t")[1]);
        }
        return out;
    }

    static String pts(AppConfig cfg, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.ptsBin().toString());
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PTSCTL_HOME", cfg.ptsHome().toAbsolutePath().toString());
        Process p = pb.start();
        String out;
        String err;
        try (InputStream o = p.getInputStream(); InputStream e = p.getErrorStream()) {
            out = new String(o.readAllBytes(), StandardCharsets.UTF_8);
            err = new String(e.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (p.waitFor() != 0) throw new AssertionError("ptsctl " + String.join(" ", args) + " failed: " + err.strip());
        return out;
    }
}
