package service;

import domain.CustomerStatement;
import domain.Order;
import domain.OrderLine;
import domain.RefundLine;
import infra.PgsimClient;
import store.OrderRepository;
import store.RefundRepository;

import java.util.ArrayList;
import java.util.List;

/** Customer-facing refund statement. Money figures come from the PG, not from our own bookkeeping. */
public class CustomerStatementService {
    private final OrderRepository orders;
    private final RefundRepository refunds;
    private final PgsimClient pg;

    public CustomerStatementService(OrderRepository orders, RefundRepository refunds, PgsimClient pg) {
        this.orders = orders;
        this.refunds = refunds;
        this.pg = pg;
    }

    public CustomerStatement statement(String orderNo) {
        Order order = orders.find(orderNo).orElseThrow(() -> new IllegalArgumentException("no order " + orderNo));
        PgsimClient.Txn txn = pg.txn(order.tid());
        List<RefundLine> mine = refunds.byOrder(orderNo);
        List<CustomerStatement.Line> lines = new ArrayList<>();
        for (OrderLine ol : order.lines()) {
            RefundLine r = mine.stream().filter(x -> x.lineNo() == ol.lineNo()).findFirst().orElse(null);
            String state = r == null ? "구매" : switch (r.status()) {
                case REQUESTED -> "환불 접수";
                case SUBMITTED -> "환불 처리 중";
                case DONE -> "환불 완료";
            };
            lines.add(new CustomerStatement.Line(ol.lineNo(), ol.name(), ol.paid(), state,
                    r == null ? null : r.refundNo()));
        }
        return new CustomerStatement(orderNo, order.customerId(), txn.amount(), txn.refunded(), txn.balance(),
                List.copyOf(lines));
    }
}
