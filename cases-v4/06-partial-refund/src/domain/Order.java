package domain;

import java.util.List;

public record Order(String orderNo, String customerId, String tid, List<OrderLine> lines, long coupon) {
    public long paidTotal() {
        return lines.stream().mapToLong(OrderLine::paid).sum();
    }

    public OrderLine line(int lineNo) {
        return lines.stream().filter(l -> l.lineNo() == lineNo).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no line " + lineNo + " in " + orderNo));
    }
}
