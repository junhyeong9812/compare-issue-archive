package domain;

import java.util.List;

/** What CS shows the customer: payment, money actually returned by the PG, and per-line refund state. */
public record CustomerStatement(String orderNo, String customerId, long paid, long refunded, long balance,
                                List<Line> lines) {
    public record Line(int lineNo, String name, long amount, String state, String refundNo) {}
}
