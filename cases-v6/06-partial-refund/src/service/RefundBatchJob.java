package service;

import domain.RefundLine;
import domain.RunResult;
import infra.PgsimClient;
import infra.RefundFileWriter;
import store.RefundRepository;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nightly refund run: every pending refund line goes to the PG in batches of
 * at most maxLines. Batch id = run id + chunk number; the PG settles and is
 * reconciled per batch id.
 */
public class RefundBatchJob {
    private final RefundRepository refunds;
    private final PgsimClient pg;
    private final String mid;
    private final String prefix;
    private final int maxLines;
    private final Path workDir;

    public RefundBatchJob(RefundRepository refunds, PgsimClient pg, String mid, String prefix, int maxLines,
                          Path workDir) {
        this.refunds = refunds;
        this.pg = pg;
        this.mid = mid;
        this.prefix = prefix;
        this.maxLines = maxLines;
        this.workDir = workDir;
    }

    public RunResult run(LocalDate day) {
        String runId = String.format("%s%s-%02d", prefix, day.format(DateTimeFormatter.BASIC_ISO_DATE),
                refunds.nextRunSeq());
        List<RefundLine> pending = new ArrayList<>();
        for (RefundLine l : uniquePending()) {
            // 결제일 당일분은 이번 회차에서 빼고 다음 날 회차로 (CS-4471)
            // TODO: 이거 아직 필요한가? pgsim 4.x 올린 뒤로는 당일 건도 status·txn 다 정상으로 나옴
            if (l.paidOn() != null && !l.paidOn().isBefore(day)) continue;
            pending.add(l);
        }
        List<String> batchIds = new ArrayList<>();
        int completed = 0;
        for (int start = 0, chunk = 1; start < pending.size(); start += maxLines, chunk++) {
            List<RefundLine> part = pending.subList(start, Math.min(pending.size(), start + maxLines));
            String batchId = runId + "-" + chunk;
            Path file = RefundFileWriter.write(workDir, batchId, part);
            int accepted = pg.refund(mid, batchId, file);
            if (accepted != part.size()) {
                throw new IllegalStateException("PG accepted " + accepted + " of " + part.size() + " in " + batchId);
            }
            for (RefundLine l : part) l.submitted(batchId);
            batchIds.add(batchId);
            completed += RefundResultMatcher.apply(part, pg.status(batchId));
        }
        int awaiting = (int) pending.stream().filter(l -> l.status() == RefundLine.Status.SUBMITTED).count();
        return new RunResult(runId, List.copyOf(batchIds), pending.size(), completed, awaiting);
    }

    /** A line queued twice by a CS double-click must be refunded once. */
    private List<RefundLine> uniquePending() {
        Map<String, RefundLine> byKey = new LinkedHashMap<>();
        for (RefundLine l : refunds.pending()) {
            byKey.putIfAbsent(l.orderNo() + "#" + l.lineNo(), l);
        }
        return new ArrayList<>(byKey.values());
    }
}
