package domain;

import java.time.LocalDate;

/** Refund of one order line. Mutable status, owned by the refund repository. */
public final class RefundLine {
    public enum Status {
        /** accepted by CS, waiting for the nightly refund run */
        REQUESTED,
        /** sent to the PG in a refund batch, no PG refund number yet */
        SUBMITTED,
        /** PG refund number received */
        DONE
    }

    private final String requestId;
    private final String orderNo;
    private final int lineNo;
    private final String tid;
    private final long amount;
    private final LocalDate requestedOn;
    private final LocalDate paidOn;
    private Status status = Status.REQUESTED;
    private String batchId;
    private String refundNo;

    public RefundLine(String requestId, String orderNo, int lineNo, String tid, long amount, LocalDate requestedOn) {
        this(requestId, orderNo, lineNo, tid, amount, requestedOn, null);
    }

    /** @param paidOn KST date the original payment was made (null if unknown) */
    public RefundLine(String requestId, String orderNo, int lineNo, String tid, long amount, LocalDate requestedOn,
                      LocalDate paidOn) {
        this.requestId = requestId;
        this.orderNo = orderNo;
        this.lineNo = lineNo;
        this.tid = tid;
        this.amount = amount;
        this.requestedOn = requestedOn;
        this.paidOn = paidOn;
    }

    public String requestId() { return requestId; }
    public String orderNo() { return orderNo; }
    public int lineNo() { return lineNo; }
    public String tid() { return tid; }
    public long amount() { return amount; }
    public LocalDate requestedOn() { return requestedOn; }
    public LocalDate paidOn() { return paidOn; }
    public Status status() { return status; }
    public String batchId() { return batchId; }
    public String refundNo() { return refundNo; }

    public void submitted(String batchId) {
        this.status = Status.SUBMITTED;
        this.batchId = batchId;
    }

    public void done(String refundNo) {
        this.status = Status.DONE;
        this.refundNo = refundNo;
    }

    @Override
    public String toString() {
        return requestId + "/" + orderNo + "#" + lineNo + " " + amount + " " + status;
    }
}
