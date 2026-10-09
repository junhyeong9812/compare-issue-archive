import api.PointsApi;
import config.AppConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Hidden judge for customer-care compensation. Checked straight through the vendor CLI:
 * every E- reference at the vendor is a real approved payment of that member, and the
 * monthly top-earners list does not move when compensation is paid.
 */
public class HiddenCompensationChecks {
    static final String[] DAYS = {"2026-10-06", "2026-11-02"};

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("hidden-compensation");

        t.test("E- references at the vendor are exactly the approved payments", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            for (String d : DAYS) api.runDay(LocalDate.parse(d));
            api.compensate("0000417", 700, "CS-20261103-0001");
            api.compensate("4512", 1_200, "CS-20261103-0002");
            api.compensate("c0012", 300, "CS-20261103-0003");
            api.compensate("010", 50, "CS-20261103-0004");
            Map<String, String> payOwner = approvedPayments();
            List<String> unmatched = new ArrayList<>();
            TreeSet<String> seen = new TreeSet<>();
            for (String member : memberNumbers()) {
                for (String[] row : hist(cfg, member)) {
                    String ref = row[1];
                    if (!ref.startsWith("E-")) continue;
                    String owner = payOwner.get(ref.substring(2));
                    if (owner == null || !owner.equalsIgnoreCase(member)) unmatched.add(member + " " + ref);
                    else seen.add(ref);
                }
            }
            TestSupport.eq(List.of(), unmatched, "E- references with no matching approved payment of that member");
            TestSupport.eq(payOwner.size(), seen.size(), "approved payments earned once each");
            TestSupport.eq(1_191L + 700L, vendorBalance(cfg, "0000417"), "0000417 balance");
            TestSupport.eq(690L + 1_200L, vendorBalance(cfg, "4512"), "4512 balance");
            TestSupport.eq(3_960L + 300L, vendorBalance(cfg, "C0012"), "C0012 balance");
            TestSupport.eq(3_000L + 50L, vendorBalance(cfg, "010"), "010 balance");
            TestSupport.eq(0L, vendorBalance(cfg, "10"), "points parked on 10");
        });

        t.test("top earners do not move when compensation is paid", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            for (String d : DAYS) api.runDay(LocalDate.parse(d));
            List<YearMonth> months = List.of(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.now());
            List<String> before = new ArrayList<>();
            for (YearMonth m : months) before.add(api.topEarnersText(m, 20));
            api.compensate("100", 9_000, "CS-20261104-0001");
            api.compensate("205", 6_500, "CS-20261104-0002");
            api.compensate("004512", 4_000, "CS-20261104-0003");
            List<String> after = new ArrayList<>();
            for (YearMonth m : months) after.add(api.topEarnersText(m, 20));
            TestSupport.eq(before, after, "top earners list after compensation");
            TestSupport.eq(9_088L, api.statement("100", YearMonth.of(2026, 11)).balance(), "100 sees the compensation");
            TestSupport.eq(6_500L, vendorBalance(cfg, "205"), "205 balance");
        });

        t.test("one ticket pays once, even for another member", () -> {
            AppConfig cfg = TestSupport.sandbox().with("data.dir", "hidden-fixtures");
            PointsApi api = new PointsApi(cfg);
            api.runDay(LocalDate.of(2026, 11, 2));
            api.compensate("417", 100, "CS-20261105-0009");
            try {
                api.compensate("0000417", 100, "CS-20261105-0009");
            } catch (RuntimeException refused) {
                // refusing is fine
            }
            TestSupport.eq(220L, vendorBalance(cfg, "417"), "417");
            TestSupport.eq(1_191L, vendorBalance(cfg, "0000417"), "0000417");
            TestSupport.eq(0, api.runDay(LocalDate.of(2026, 11, 2)).earned(), "re-run");
        });

        t.finish();
    }

    static String cell(String raw) {
        String s = raw.strip();
        if (s.startsWith("=\"") && s.endsWith("\"")) return s.substring(2, s.length() - 1);
        return s;
    }

    static List<String> memberNumbers() throws Exception {
        List<String> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of("hidden-fixtures", "members.csv"), StandardCharsets.UTF_8);
        for (String l : lines.subList(1, lines.size())) if (!l.isBlank()) out.add(cell(l.split(",")[0]));
        return out;
    }

    /** payment id -> member number, for approved payments of known members. */
    static Map<String, String> approvedPayments() throws Exception {
        TreeSet<String> known = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        known.addAll(memberNumbers());
        Map<String, String> out = new HashMap<>();
        for (String d : DAYS) {
            List<String> lines = Files.readAllLines(Path.of("hidden-fixtures", "payments-" + d + ".csv"), StandardCharsets.UTF_8);
            for (String l : lines.subList(1, lines.size())) {
                if (l.isBlank()) continue;
                String[] f = l.split(",", -1);
                String member = cell(f[1]);
                if ("APPROVED".equals(f[4].strip()) && known.contains(member)) out.putIfAbsent(f[0], member);
            }
        }
        return out;
    }

    static long vendorBalance(AppConfig cfg, String account) throws Exception {
        String[] t = pts(cfg, "bal", account).strip().split("\\s+");
        return Long.parseLong(t[1]);
    }

    static List<String[]> hist(AppConfig cfg, String account) throws Exception {
        List<String[]> out = new ArrayList<>();
        for (String line : pts(cfg, "hist", account).split("\n")) if (!line.isBlank()) out.add(line.split("\t"));
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
