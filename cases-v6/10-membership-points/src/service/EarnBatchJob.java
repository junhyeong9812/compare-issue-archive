package service;

import domain.EarnEntry;
import domain.Member;
import domain.Payment;
import domain.RunResult;
import infra.PtsctlClient;
import store.EarnLedger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Daily earn run: every approved payment of a known member earns grade-rate
 * points at the points vendor once. The vendor reference is prefix + payment id,
 * so a re-run never earns twice.
 */
public class EarnBatchJob {
    private final MemberDirectory members;
    private final EarnPolicy policy;
    private final EarnLedger ledger;
    private final PtsctlClient pts;
    private final String refPrefix;

    public EarnBatchJob(MemberDirectory members, EarnPolicy policy, EarnLedger ledger, PtsctlClient pts,
                        String refPrefix) {
        this.members = members;
        this.policy = policy;
        this.ledger = ledger;
        this.pts = pts;
        this.refPrefix = refPrefix;
    }

    public RunResult run(LocalDate day, List<Payment> payments) {
        int earned = 0;
        int skipped = 0;
        long total = 0;
        List<String> unknown = new ArrayList<>();
        for (Payment p : PaymentDeduper.unique(payments)) {
            if (!p.approved() || ledger.contains(p.payId())) {
                skipped++;
                continue;
            }
            Optional<Member> m = members.find(p.memberNo());
            if (m.isEmpty()) {
                unknown.add(p.memberNo());
                continue;
            }
            long pts = policy.points(p.amount(), m.get().grade());
            if (pts == 0) {
                skipped++;
                continue;
            }
            String entryNo = this.pts.earn(m.get().memberNo(), pts, refPrefix + "-" + p.payId());
            ledger.record(new EarnEntry(p.payId(), m.get().memberNo(), pts, day, entryNo));
            earned++;
            total += pts;
        }
        return new RunResult(earned, total, List.copyOf(unknown), skipped);
    }
}
