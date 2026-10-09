package service;

import domain.Member;
import domain.MemberStatement;
import infra.PtsctlClient;
import store.EarnLedger;

import java.time.YearMonth;

/** Member-facing points summary. The balance is the vendor's, not ours. */
public class StatementService {
    private final MemberDirectory members;
    private final EarnLedger ledger;
    private final PtsctlClient pts;

    public StatementService(MemberDirectory members, EarnLedger ledger, PtsctlClient pts) {
        this.members = members;
        this.ledger = ledger;
        this.pts = pts;
    }

    public MemberStatement statement(String memberNo, YearMonth month) {
        Member m = members.find(memberNo).orElseThrow(() -> new IllegalArgumentException("unknown member " + memberNo));
        long balance = pts.balance(m.memberNo());
        int entries = pts.history(m.memberNo()).size();
        return new MemberStatement(m.memberNo(), m.name(), m.grade(), balance, entries,
                ledger.earnedIn(m.memberNo(), month));
    }
}
