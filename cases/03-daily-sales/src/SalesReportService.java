import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesReportService {
    private final OrderRepository repository;

    public SalesReportService(OrderRepository repository) {
        this.repository = repository;
    }

    /** Revenue per calendar day, for every day from {@code from} to {@code to} (both included). */
    public Map<LocalDate, Long> dailyTotals(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            totals.put(day, sum(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
        }
        return totals;
    }

    /** Revenue for the whole period from {@code from} to {@code to} (both days included). */
    public long periodTotal(LocalDate from, LocalDate to) {
        return sum(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private long sum(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = repository.findCreatedIn(start, end);
        long total = 0;
        for (Order o : orders) {
            total += o.amountCents();
        }
        return total;
    }
}
