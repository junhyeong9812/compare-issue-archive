package store;

import domain.Order;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class OrderRepository {
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private int seq;

    public synchronized String nextOrderNo(String yyyymmdd) {
        return String.format("HB%s-%04d", yyyymmdd, ++seq);
    }

    public synchronized void save(Order order) {
        orders.put(order.orderNo(), order);
    }

    public synchronized Optional<Order> find(String orderNo) {
        return Optional.ofNullable(orders.get(orderNo));
    }
}
