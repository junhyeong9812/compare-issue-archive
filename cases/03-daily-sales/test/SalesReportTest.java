import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class SalesReportTest {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    public static void main(String[] args) {
        OrderRepository repo = new OrderRepository();
        repo.add(new Order("o1", LocalDateTime.of(2026, 3, 1, 10, 0), 100_00));
        repo.add(new Order("o2", LocalDateTime.of(2026, 3, 1, 23, 30), 50_00));
        repo.add(new Order("o3", LocalDateTime.of(2026, 3, 2, 0, 0), 70_00));
        repo.add(new Order("o4", LocalDateTime.of(2026, 3, 2, 15, 0), 30_00));
        repo.add(new Order("o5", LocalDateTime.of(2026, 3, 3, 0, 0), 20_00));
        SalesReportService reports = new SalesReportService(repo);

        Map<LocalDate, Long> daily = reports.dailyTotals(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        System.out.println("daily = " + daily);
        check(daily.get(LocalDate.of(2026, 3, 1)) == 150_00, "2026-03-01 revenue is 150.00");
        check(daily.get(LocalDate.of(2026, 3, 2)) == 100_00, "2026-03-02 revenue is 100.00");

        long period = reports.periodTotal(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        check(period == 250_00, "period 03-01..03-02 revenue is 250.00 (got " + period + ")");
        long dailySum = daily.values().stream().mapToLong(Long::longValue).sum();
        check(dailySum == period, "sum of daily rows equals period total (" + dailySum + " vs " + period + ")");

        if (failed > 0) {
            System.out.println(failed + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
