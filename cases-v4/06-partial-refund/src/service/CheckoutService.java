package service;

import domain.CatalogItem;
import domain.Order;
import domain.OrderLine;
import infra.PgsimClient;
import store.OrderRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckoutService {
    private final Map<String, CatalogItem> catalog;
    private final OrderRepository orders;
    private final PgsimClient pg;
    private final String mid;

    public CheckoutService(Map<String, CatalogItem> catalog, OrderRepository orders, PgsimClient pg, String mid) {
        this.catalog = catalog;
        this.orders = orders;
        this.pg = pg;
        this.mid = mid;
    }

    /** One line per unit: ordering the same SKU twice gives two lines. */
    public Order place(String customerId, List<String> skus, long coupon, LocalDate day) {
        if (skus.isEmpty()) throw new IllegalArgumentException("empty cart");
        List<CatalogItem> items = new ArrayList<>();
        for (String sku : skus) {
            CatalogItem item = catalog.get(sku);
            if (item == null) throw new IllegalArgumentException("unknown sku " + sku);
            items.add(item);
        }
        List<Long> discounts = DiscountAllocator.allocate(items.stream().map(CatalogItem::price).toList(), coupon);
        List<OrderLine> lines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            CatalogItem it = items.get(i);
            lines.add(new OrderLine(i + 1, it.sku(), it.name(), it.price(), discounts.get(i)));
        }
        String orderNo = orders.nextOrderNo(day.format(DateTimeFormatter.BASIC_ISO_DATE));
        long total = lines.stream().mapToLong(OrderLine::paid).sum();
        String tid = pg.pay(mid, total, orderNo);
        Order order = new Order(orderNo, customerId, tid, List.copyOf(lines), coupon);
        orders.save(order);
        return order;
    }
}
