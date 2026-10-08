package store;

import domain.EarnEntry;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Our own record of earns already sent, keyed by payment id (a payment earns once). */
public class EarnLedger {
    private final Map<String, EarnEntry> byPayment = new LinkedHashMap<>();

    public synchronized boolean contains(String payId) {
        return byPayment.containsKey(payId);
    }

    public synchronized void record(EarnEntry e) {
        byPayment.put(e.payId(), e);
    }

    public synchronized List<EarnEntry> all() {
        return new ArrayList<>(byPayment.values());
    }

    public synchronized long earnedIn(String memberNo, YearMonth month) {
        return byPayment.values().stream()
                .filter(e -> e.memberNo().equals(memberNo) && YearMonth.from(e.day()).equals(month))
                .mapToLong(EarnEntry::points).sum();
    }
}
