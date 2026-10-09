package service;

import domain.Order;
import domain.OrderLine;
import domain.RefundLine;
import store.OrderRepository;
import store.RefundRepository;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** CS side: accepts a (partial) refund request. Money moves in the nightly refund run. */
public class RefundService {
    private final OrderRepository orders;
    private final RefundRepository refunds;

    public RefundService(OrderRepository orders, RefundRepository refunds) {
        this.orders = orders;
        this.refunds = refunds;
    }

    public String request(String orderNo, List<Integer> lineNos, LocalDate day) {
        Order order = orders.find(orderNo).orElseThrow(() -> new IllegalArgumentException("no order " + orderNo));
        if (lineNos.isEmpty()) throw new IllegalArgumentException("no lines to refund");
        Set<Integer> wanted = new LinkedHashSet<>(lineNos);
        if (wanted.size() != lineNos.size()) throw new IllegalArgumentException("line listed twice");
        Set<Integer> taken = new LinkedHashSet<>();
        for (RefundLine r : refunds.byOrder(orderNo)) taken.add(r.lineNo());
        for (int n : wanted) {
            order.line(n);
            if (taken.contains(n)) throw new IllegalStateException("line " + n + " of " + orderNo + " already refunded");
        }
        String id = refunds.nextRequestId();
        for (int n : wanted) {
            OrderLine line = order.line(n);
            refunds.add(new RefundLine(id, orderNo, n, order.tid(), line.paid(), day));
        }
        return id;
    }
}
