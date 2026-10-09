import api.SettlementApi;
import config.AppConfig;
import domain.DailyLine;
import util.Money;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hidden judge for the partner statement: the request (lines, KRW, rates on published days, total)
 * and the decision invariant: a value that only the settlement source has (vendor holiday, src 3)
 * never appears in a text that goes to a partner. Checked against the vendor CLI itself.
 */
public class HiddenDetailChecks {
    static final List<String> PAIRS = List.of("USDKRW", "EURKRW", "JPYKRW", "CNYKRW");

    public static void main(String[] args) throws Exception {
        TestSupport t = new TestSupport("hidden-detail");

        System.setProperty("settle.data.dir", "fixtures");
        SettlementApi wk1 = new SettlementApi(AppConfig.load());
        LocalDate f1 = LocalDate.of(2026, 10, 1), t1 = LocalDate.of(2026, 10, 5);
        System.setProperty("settle.data.dir", "hidden-fixtures");
        SettlementApi wk2 = new SettlementApi(AppConfig.load());
        LocalDate f2 = LocalDate.of(2026, 10, 6), t2 = LocalDate.of(2026, 10, 12);

        List<String> secret1 = settlementOnlyValues(f1, t1);
        List<String> secret2 = settlementOnlyValues(f2, t2);
        t.test("probe: holiday settlement values exist", () -> {
            TestSupport.check(secret1.size() >= 8, "sample week " + secret1);
            TestSupport.check(secret2.size() >= 8, "hidden week " + secret2);
        });

        for (String p : List.of("P-1001", "P-1002", "P-1003")) {
            t.test("sample week " + p + ": no settlement-only rate in the partner statement", () ->
                    noSecret(wk1.partnerDetailText(p, f1, t1), secret1));
        }
        for (String p : List.of("P-1001", "P-1002", "P-1003", "P-1005")) {
            t.test("week 10/06-10/12 " + p + ": no settlement-only rate in the partner statement", () ->
                    noSecret(wk2.partnerDetailText(p, f2, t2), secret2));
        }

        t.test("week 10/06-10/12 P-1001: weekday rate + KRW, holiday KRW", () -> {
            String s = wk2.partnerDetailText("P-1001", f2, t2);
            has(s, "H0101", "1,373.04", "562,946");
            has(s, "H0105", "1,609.52", "159,342");
            has(s, "H0106", "1,027,686");
            has(s, "H0113", "1,288,698");
            has(s, "H0116", "1,377.43", "213,502");
            TestSupport.check(line(s, "H0102") == null && line(s, "H0109") == null, "other partners listed\n" + s);
        });
        t.test("week 10/06-10/12 P-1003: JPY lines and refund", () -> {
            String s = wk2.partnerDetailText("P-1003", f2, t2);
            has(s, "H0103", "920.84", "589,338");
            has(s, "H0107", "1,091,299");
            TestSupport.check(line(s, "H0111") != null && line(s, "H0112") != null, "purchase+refund lines\n" + s);
        });
        t.test("week 10/06-10/12 P-1005: CNY lines", () -> {
            String s = wk2.partnerDetailText("P-1005", f2, t2);
            has(s, "H0102", "190.31", "243,597");
            has(s, "H0108", "637,103");
            has(s, "H0114", "1,409,132");
        });
        for (String p : List.of("P-1001", "P-1002", "P-1003", "P-1005")) {
            t.test("week 10/06-10/12 " + p + ": total = daily gross", () -> total(wk2, p, f2, t2));
        }
        for (String p : List.of("P-1001", "P-1002", "P-1003")) {
            t.test("sample week " + p + ": total = daily gross", () -> total(wk1, p, f1, t1));
        }
        t.test("sample week P-1002 weekday rates", () -> {
            String s = wk1.partnerDetailText("P-1002", f1, t1);
            has(s, "T0002", "1,602.98", "144,108");
            has(s, "T0022", "189.59", "106,170");
            has(s, "T0014", "1,724,388");
        });

        t.test("accounting export unchanged and carries no settlement-only value", () -> {
            String csv = wk2.exportCsv(f2, t2);
            TestSupport.eq("date,partner_id,count,gross_krw,fee_krw,net_krw", csv.lines().findFirst().orElse(""), "header");
            noSecret(csv, secret2);
        });

        t.finish();
    }

    /** src-3 values on vendor holidays (lagged default quote) that differ from the published value. */
    static List<String> settlementOnlyValues(LocalDate from, LocalDate to) throws Exception {
        List<String> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            for (String pair : PAIRS) {
                String[] pub = fxq("rate", pair, d.toString()).trim().split("\\s+");
                if (pub.length < 4 || !pub[3].startsWith("~")) continue;
                String[] src3 = fxq("rate", pair, d.toString(), "--src", "3").trim().split("\\s+");
                if (!src3[2].equals(pub[2])) out.add(src3[2]);
            }
        }
        return out;
    }

    static void noSecret(String text, List<String> secret) {
        String plain = text.replace(",", "");
        for (String v : secret) {
            TestSupport.check(!plain.contains(v), "settlement-only rate " + v + " appears in partner-facing text:\n" + text);
        }
    }

    static void has(String text, String txn, String... parts) {
        String l = line(text, txn);
        TestSupport.check(l != null, "no line for " + txn + "\n" + text);
        for (String p : parts) TestSupport.check(l.contains(p), txn + " line lacks " + p + ": " + l);
    }

    static void total(SettlementApi api, String partner, LocalDate from, LocalDate to) {
        List<DailyLine> d = api.daily(from, to).stream().filter(l -> l.partnerId().equals(partner)).toList();
        long gross = d.stream().mapToLong(DailyLine::grossKrw).sum();
        int count = d.stream().mapToInt(DailyLine::count).sum();
        String s = api.partnerDetailText(partner, from, to);
        String tot = s.lines().filter(l -> l.trim().startsWith("합계")).findFirst().orElse("");
        TestSupport.check(tot.contains(count + "건") && tot.contains(Money.won(gross)),
                partner + " expected " + count + "건 " + Money.won(gross) + ": " + tot);
    }

    static String line(String text, String txnId) {
        return text.lines().filter(l -> Arrays.asList(l.trim().split("\\s+")).contains(txnId)).findFirst().orElse(null);
    }

    static String fxq(String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(AppConfig.load().fxqBin().toString());
        cmd.addAll(Arrays.asList(args));
        Process p = new ProcessBuilder(cmd).start();
        String out;
        try (InputStream o = p.getInputStream()) {
            out = new String(o.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (p.waitFor() != 0) throw new AssertionError("fxq " + String.join(" ", args) + " failed");
        return out;
    }
}
