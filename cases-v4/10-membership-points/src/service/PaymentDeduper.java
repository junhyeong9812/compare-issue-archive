package service;

import domain.Payment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The acquirer resends rows after a timeout; the same payment id must be counted once. */
public final class PaymentDeduper {
    private PaymentDeduper() {}

    public static List<Payment> unique(List<Payment> payments) {
        Set<String> seen = new LinkedHashSet<>();
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments) {
            if (seen.add(p.payId())) out.add(p);
        }
        return out;
    }
}
