package service;

import domain.DailyLine;
import domain.StatementRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compares our daily lines with the partners' statements. */
public class ReconcileService {

    /** Difference between our settlement and the partner's statement for a date/partner. */
    public record Mismatch(LocalDate date, String partnerId, String field, long ours, long theirs) {
        public long diff() { return ours - theirs; }
    }

    public List<Mismatch> compare(List<DailyLine> ours, List<StatementRow> theirs) {
        Map<String, DailyLine> mine = new HashMap<>();
        for (DailyLine l : ours) mine.put(key(l.date(), l.partnerId()), l);
        List<Mismatch> out = new ArrayList<>();
        for (StatementRow r : theirs) {
            DailyLine l = mine.remove(key(r.date(), r.partnerId()));
            if (l == null) {
                out.add(new Mismatch(r.date(), r.partnerId(), "missing", 0, r.grossKrw()));
                continue;
            }
            if (l.count() != r.count()) out.add(new Mismatch(r.date(), r.partnerId(), "count", l.count(), r.count()));
            if (l.grossKrw() != r.grossKrw()) out.add(new Mismatch(r.date(), r.partnerId(), "gross", l.grossKrw(), r.grossKrw()));
        }
        for (DailyLine l : mine.values()) {
            out.add(new Mismatch(l.date(), l.partnerId(), "unexpected", l.grossKrw(), 0));
        }
        out.sort((a, b) -> a.date().equals(b.date()) ? a.partnerId().compareTo(b.partnerId()) : a.date().compareTo(b.date()));
        return out;
    }

    private static String key(LocalDate d, String partner) {
        return d + "|" + partner;
    }
}
