package service;

import domain.EarnEntry;
import domain.Member;
import store.EarnLedger;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Marketing's monthly "top earners" list, from what the earn run sent to the vendor this
 * month (our own earn ledger), not from the vendor balance.
 */
// TODO: the member app already shows the vendor balance -- do we still need our own count here?
public class TopEarnersReport {
    public record Row(String memberNo, String name, long points) {}

    private final MemberDirectory members;
    private final EarnLedger ledger;

    public TopEarnersReport(MemberDirectory members, EarnLedger ledger) {
        this.members = members;
        this.ledger = ledger;
    }

    public List<Row> top(YearMonth month, int n) {
        Map<String, Long> sums = new LinkedHashMap<>();
        for (EarnEntry e : ledger.all()) {
            if (YearMonth.from(e.day()).equals(month)) sums.merge(e.memberNo(), e.points(), Long::sum);
        }
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, Long> s : sums.entrySet()) {
            String name = members.find(s.getKey()).map(Member::name).orElse("?");
            rows.add(new Row(s.getKey(), name, s.getValue()));
        }
        rows.sort(Comparator.comparingLong(Row::points).reversed().thenComparing(Row::memberNo));
        return rows.size() > n ? rows.subList(0, n) : rows;
    }
}
