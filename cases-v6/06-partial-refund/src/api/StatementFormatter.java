package api;

import domain.CustomerStatement;
import util.Money;

public final class StatementFormatter {
    private StatementFormatter() {}

    public static String format(CustomerStatement s) {
        StringBuilder sb = new StringBuilder();
        sb.append("주문 ").append(s.orderNo()).append(" (").append(s.customerId()).append(")\n");
        for (CustomerStatement.Line l : s.lines()) {
            sb.append(String.format("  %d. %s  %s  %s%s%n", l.lineNo(), l.name(), Money.won(l.amount()), l.state(),
                    l.refundNo() == null ? "" : " [" + l.refundNo() + "]"));
        }
        sb.append("결제 ").append(Money.won(s.paid()))
          .append(" / 환불 ").append(Money.won(s.refunded()))
          .append(" / 잔액 ").append(Money.won(s.balance())).append('\n');
        return sb.toString();
    }
}
