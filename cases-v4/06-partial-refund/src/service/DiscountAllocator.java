package service;

import util.Money;

import java.util.ArrayList;
import java.util.List;

/**
 * Spreads an order-level coupon over the lines in proportion to price (floor),
 * the leftover won goes to the first most expensive line. A partial refund
 * returns what the customer actually paid for that line.
 */
public final class DiscountAllocator {
    private DiscountAllocator() {}

    public static List<Long> allocate(List<Long> prices, long coupon) {
        List<Long> out = new ArrayList<>();
        if (prices.isEmpty()) return out;
        long total = prices.stream().mapToLong(Long::longValue).sum();
        if (coupon < 0 || coupon > total) throw new IllegalArgumentException("coupon out of range: " + coupon);
        long given = 0;
        int top = 0;
        for (int i = 0; i < prices.size(); i++) {
            long d = coupon == 0 ? 0 : Money.share(coupon, prices.get(i), total);
            out.add(d);
            given += d;
            if (prices.get(i) > prices.get(top)) top = i;
        }
        out.set(top, out.get(top) + (coupon - given));
        return out;
    }
}
