package domain;

/** One unit of a product in an order. Quantity is always 1: two of the same product are two lines. */
public record OrderLine(int lineNo, String sku, String name, long price, long discount) {
    public long paid() {
        return price - discount;
    }
}
