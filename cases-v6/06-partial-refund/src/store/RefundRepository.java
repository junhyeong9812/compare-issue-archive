package store;

import domain.RefundLine;

import java.util.ArrayList;
import java.util.List;

public class RefundRepository {
    private final List<RefundLine> lines = new ArrayList<>();
    private int requestSeq;
    private int runSeq;

    public synchronized String nextRequestId() {
        return String.format("RQ%05d", ++requestSeq);
    }

    public synchronized int nextRunSeq() {
        return ++runSeq;
    }

    public synchronized void add(RefundLine line) {
        lines.add(line);
    }

    public synchronized List<RefundLine> pending() {
        return lines.stream().filter(l -> l.status() == RefundLine.Status.REQUESTED).toList();
    }

    public synchronized List<RefundLine> byOrder(String orderNo) {
        return lines.stream().filter(l -> l.orderNo().equals(orderNo)).toList();
    }

    public synchronized List<RefundLine> all() {
        return new ArrayList<>(lines);
    }
}
