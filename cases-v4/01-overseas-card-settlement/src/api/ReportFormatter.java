package api;

import domain.DailyLine;
import domain.FxQuote;
import service.ReconcileService.Mismatch;
import infra.DataFiles;
import util.Money;

import java.time.LocalDate;
import java.util.List;

/** Text renderings for the ops console. */
public final class ReportFormatter {
    private ReportFormatter() {}

    public static String daily(List<DailyLine> lines, DataFiles data) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s  %-7s  %-14s %5s %15s %12s %15s%n",
                "DATE", "PARTNER", "NAME", "CNT", "GROSS", "FEE", "NET"));
        long gross = 0, fee = 0;
        for (DailyLine l : lines) {
            String name = data.partner(l.partnerId()).map(p -> p.name()).orElse("?");
            sb.append(String.format("%-10s  %-7s  %-14s %5d %15s %12s %15s%n",
                    l.date(), l.partnerId(), name, l.count(), Money.won(l.grossKrw()),
                    Money.won(l.feeKrw()), Money.won(l.netKrw())));
            gross += l.grossKrw();
            fee += l.feeKrw();
        }
        sb.append(String.format("%-10s  %-7s  %-14s %5s %15s %12s %15s%n",
                "TOTAL", "", "", "", Money.won(gross), Money.won(fee), Money.won(gross - fee)));
        return sb.toString();
    }

    /** CSV export for the accounting team (one row per partner per day). */
    public static String csv(List<DailyLine> lines) {
        StringBuilder sb = new StringBuilder("date,partner_id,count,gross_krw,fee_krw,net_krw\n");
        for (DailyLine l : lines) {
            sb.append(l.date()).append(',').append(l.partnerId()).append(',').append(l.count()).append(',')
              .append(l.grossKrw()).append(',').append(l.feeKrw()).append(',').append(l.netKrw()).append('\n');
        }
        return sb.toString();
    }

    public static String mismatches(List<Mismatch> list) {
        if (list.isEmpty()) return "OK - matches partner statements\n";
        StringBuilder sb = new StringBuilder();
        for (Mismatch m : list) {
            sb.append(String.format("MISMATCH %s %s %-10s ours=%s partner=%s diff=%s%n",
                    m.date(), m.partnerId(), m.field(), Money.won(m.ours()), Money.won(m.theirs()),
                    Money.won(m.diff())));
        }
        return sb.toString();
    }

    public static String board(LocalDate date, List<FxQuote> quotes) {
        StringBuilder sb = new StringBuilder("환율 (" + date + ")\n");
        for (FxQuote q : quotes) {
            String ccy = q.pair().substring(0, 3);
            String unit = "JPY".equals(ccy) ? "100 " + ccy : ccy;
            sb.append(String.format("  %-8s %10s원%n", unit, Money.rate(q.rate())));
        }
        return sb.toString();
    }
}
