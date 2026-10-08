import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class HiddenSalesCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    public static void main(String[] args) {
        OrderRepository repo = new OrderRepository();
        repo.add(new Order("a", LocalDateTime.of(2026, 3, 31, 0, 0), 1));
        repo.add(new Order("b", LocalDateTime.of(2026, 3, 31, 23, 59, 59, 500_000_000), 10));
        repo.add(new Order("c", LocalDateTime.of(2026, 4, 1, 0, 0), 100));
        repo.add(new Order("d", LocalDateTime.of(2026, 4, 1, 0, 0, 0, 1), 1_000));
        repo.add(new Order("e", LocalDateTime.of(2026, 4, 2, 23, 59, 59, 999_999_999), 10_000));
        repo.add(new Order("f", LocalDateTime.of(2026, 4, 3, 0, 0), 100_000));
        repo.add(new Order("g", LocalDateTime.of(2026, 3, 30, 23, 59, 59, 999_000_000), 1_000_000));
        SalesReportService reports = new SalesReportService(repo);

        Map<LocalDate, Long> daily = reports.dailyTotals(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 3));
        System.out.println("daily = " + daily);
        check(daily.get(LocalDate.of(2026, 3, 31)) == 11, "03-31 includes 00:00 and 23:59:59.5 only");
        check(daily.get(LocalDate.of(2026, 4, 1)) == 1_100, "04-01 starts exactly at midnight");
        check(daily.get(LocalDate.of(2026, 4, 2)) == 10_000, "04-02 includes last nanosecond, excludes next midnight");
        check(daily.get(LocalDate.of(2026, 4, 3)) == 100_000, "04-03");

        check(reports.periodTotal(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 31)) == 11, "single-day period");
        check(reports.periodTotal(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2)) == 11_100, "two-day period");
        long march = reports.periodTotal(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        long april = reports.periodTotal(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        long all = reports.periodTotal(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30));
        check(march == 1_000_011, "March total (got " + march + ")");
        check(april == 111_100, "April total (got " + april + ")");
        check(march + april == all, "adjacent months add up to the combined period");
        long dailySum = daily.values().stream().mapToLong(Long::longValue).sum();
        check(dailySum == reports.periodTotal(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 3)), "daily rows add up to period total");
        check(reports.dailyTotals(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1)).get(LocalDate.of(2026, 5, 1)) == 0, "empty day is 0");

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println("all hidden checks passed");
    }
}
