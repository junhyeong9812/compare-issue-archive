package api;

import config.AppConfig;
import domain.DailyLine;
import domain.FxQuote;
import service.ReconcileService.Mismatch;
import infra.FxqClient;
import infra.DataFiles;
import service.BusinessDateResolver;
import service.RateBoardService;
import service.RateService;
import service.ReconcileService;
import service.SettlementService;

import java.time.LocalDate;
import java.util.List;

/** Facade used by the batch CLI, the ops console and the app gateway. */
public class SettlementApi {
    private final SettlementService settlement;
    private final ReconcileService reconcile = new ReconcileService();
    private final RateBoardService board;
    private final DataFiles data;

    public SettlementApi(AppConfig cfg) {
        FxqClient fxq = new FxqClient(cfg.fxqBin(), cfg.fxqTimeoutSeconds());
        RateService rates = new RateService(fxq);
        this.data = new DataFiles(cfg.dataDir());
        this.settlement = new SettlementService(data, rates, new BusinessDateResolver(cfg.businessZone()));
        this.board = new RateBoardService(rates, cfg.boardCurrencies());
    }

    public static SettlementApi fromDefaultConfig() {
        return new SettlementApi(AppConfig.load());
    }

    public List<DailyLine> daily(LocalDate from, LocalDate to) {
        return settlement.settle(from, to);
    }

    public String dailyReport(LocalDate from, LocalDate to) {
        return ReportFormatter.daily(daily(from, to), data);
    }

    public List<Mismatch> reconcile(LocalDate from, LocalDate to) {
        return reconcile.compare(daily(from, to), data.statements(from, to));
    }

    public String reconcileReport(LocalDate from, LocalDate to) {
        return ReportFormatter.mismatches(reconcile(from, to));
    }

    public String exportCsv(LocalDate from, LocalDate to) {
        return ReportFormatter.csv(daily(from, to));
    }

    public List<FxQuote> rateBoard(LocalDate date) {
        return board.board(date);
    }

    public String rateBoardText(LocalDate date) {
        return ReportFormatter.board(date, rateBoard(date));
    }
}
