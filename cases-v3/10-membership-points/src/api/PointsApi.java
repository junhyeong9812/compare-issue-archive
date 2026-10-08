package api;

import config.AppConfig;
import domain.MemberStatement;
import domain.Payment;
import domain.RunResult;
import infra.CsvFiles;
import infra.PtsctlClient;
import service.EarnBatchJob;
import service.EarnPolicy;
import service.MemberDirectory;
import service.StatementService;
import service.TopEarnersReport;
import store.EarnLedger;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Facade used by the nightly scheduler, the member app backend and the tests. */
public class PointsApi {
    private final Path dataDir;
    private final EarnBatchJob earnJob;
    private final StatementService statements;
    private final TopEarnersReport topEarners;

    public PointsApi(AppConfig cfg) {
        this.dataDir = cfg.dataDir();
        MemberDirectory members = new MemberDirectory(CsvFiles.members(dataDir.resolve("members.csv")));
        PtsctlClient pts = new PtsctlClient(cfg.ptsBin(), cfg.ptsHome(), cfg.ptsTimeoutSeconds());
        EarnLedger ledger = new EarnLedger();
        this.earnJob = new EarnBatchJob(members, new EarnPolicy(cfg.capPerPayment()), ledger, pts, cfg.refPrefix());
        this.statements = new StatementService(members, ledger, pts);
        this.topEarners = new TopEarnersReport(members, ledger);
    }

    /** Runs the earn batch for one day's acquirer export (data.dir/payments-YYYY-MM-DD.csv). */
    public RunResult runDay(LocalDate day) {
        List<Payment> payments = CsvFiles.payments(dataDir.resolve("payments-" + day + ".csv"));
        return earnJob.run(day, payments);
    }

    public RunResult run(LocalDate day, List<Payment> payments) {
        return earnJob.run(day, payments);
    }

    public MemberStatement statement(String memberNo, YearMonth month) {
        return statements.statement(memberNo, month);
    }

    public String statementText(String memberNo, YearMonth month) {
        return ReportFormatter.statement(statement(memberNo, month));
    }

    public String topEarnersText(YearMonth month, int n) {
        return ReportFormatter.topEarners(month, topEarners.top(month, n));
    }
}
