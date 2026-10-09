package service;

import domain.RefundLine;
import infra.PgsimClient.RefundRow;
import infra.RefundFileWriter;

import java.util.ArrayList;
import java.util.List;

/** Attaches PG refund numbers from a batch status to the lines we sent in that batch. */
public final class RefundResultMatcher {
    private RefundResultMatcher() {}

    /** Returns how many lines got a refund number. Each PG row is used at most once. */
    public static int apply(List<RefundLine> sent, List<RefundRow> rows) {
        List<RefundRow> free = new ArrayList<>(rows);
        int matched = 0;
        for (RefundLine line : sent) {
            String ref = RefundFileWriter.merchantRef(line);
            for (int i = 0; i < free.size(); i++) {
                RefundRow row = free.get(i);
                if (row.tid().equals(line.tid()) && row.amount() == line.amount() && row.aux().equals(ref)) {
                    line.done(row.refundNo());
                    free.remove(i);
                    matched++;
                    break;
                }
            }
        }
        return matched;
    }
}
