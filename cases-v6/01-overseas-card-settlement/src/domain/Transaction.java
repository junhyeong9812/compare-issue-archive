package domain;

import java.math.BigDecimal;
import java.time.Instant;

/** One approved overseas card transaction as received from the acquirer feed. */
public record Transaction(String id, String partnerId, Instant approvedAt, String currency,
                          BigDecimal amount, Type type) {
    public enum Type { PURCHASE, REFUND }

    public BigDecimal signedAmount() {
        return type == Type.REFUND ? amount.negate() : amount;
    }
}
