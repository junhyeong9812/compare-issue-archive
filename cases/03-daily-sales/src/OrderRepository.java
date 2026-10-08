import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {
    private final List<Order> orders = new ArrayList<>();

    public void add(Order order) {
        orders.add(order);
    }

    public List<Order> findCreatedIn(LocalDateTime start, LocalDateTime end) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (!o.createdAt().isBefore(start) && !o.createdAt().isAfter(end)) {
                result.add(o);
            }
        }
        return result;
    }
}
