package service;

import domain.DailyLine;
import domain.Partner;
import domain.Transaction;
import infra.DataFiles;
import util.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds per-partner daily settlement lines from the transaction feed. */
public class SettlementService {
    private final DataFiles data;
    private final RateService rates;
    private final BusinessDateResolver dates;

    public SettlementService(DataFiles data, RateService rates, BusinessDateResolver dates) {
        this.data = data;
        this.rates = rates;
        this.dates = dates;
    }

    public List<DailyLine> settle(LocalDate from, LocalDate to) {
        Map<String, long[]> acc = new TreeMap<>(); // key "date|partner" -> {count, grossKrw}
        for (Transaction t : data.transactions()) {
            LocalDate d = dates.dateOf(t.approvedAt());
            if (d.isBefore(from) || d.isAfter(to)) continue;
            Partner p = data.partner(t.partnerId()).orElse(null);
            if (p == null || !p.active()) continue;
            long krw = convert(t, d);
            long[] a = acc.computeIfAbsent(d + "|" + p.id(), k -> new long[2]);
            a[0]++;
            a[1] += krw;
        }
        List<DailyLine> out = new ArrayList<>();
        for (var e : acc.entrySet()) {
            String[] k = e.getKey().split("\\|");
            Partner p = data.partner(k[1]).orElseThrow();
            long gross = e.getValue()[1];
            out.add(new DailyLine(LocalDate.parse(k[0]), p.id(), (int) e.getValue()[0], gross,
                    Money.fee(gross, p.feeBps())));
        }
        return out;
    }

    private long convert(Transaction t, LocalDate businessDate) {
        var rate = rates.settlementRate(t.currency(), businessDate);
        return Money.toKrw(t.signedAmount(), rate, CurrencyPairs.unitOf(t.currency()));
    }
}
