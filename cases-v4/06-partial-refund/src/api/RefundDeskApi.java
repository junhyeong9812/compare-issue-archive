package api;

import config.AppConfig;
import domain.CatalogItem;
import domain.CustomerStatement;
import domain.Order;
import domain.RefundLine;
import domain.RunResult;
import infra.CatalogLoader;
import infra.PgsimClient;
import service.CheckoutService;
import service.CustomerStatementService;
import service.RefundBatchJob;
import service.RefundService;
import store.OrderRepository;
import store.RefundRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Facade used by the CS console, the nightly scheduler and the tests. */
public class RefundDeskApi {
    private final CheckoutService checkout;
    private final RefundService refundService;
    private final RefundBatchJob batchJob;
    private final CustomerStatementService statements;
    private final RefundRepository refunds;

    public RefundDeskApi(AppConfig cfg) {
        Map<String, CatalogItem> catalog = CatalogLoader.load(cfg.catalogFile());
        PgsimClient pg = new PgsimClient(cfg.pgBin(), cfg.pgHome(), cfg.pgTimeoutSeconds());
        OrderRepository orders = new OrderRepository();
        this.refunds = new RefundRepository();
        this.checkout = new CheckoutService(catalog, orders, pg, cfg.merchantId());
        this.refundService = new RefundService(orders, refunds);
        this.batchJob = new RefundBatchJob(refunds, pg, cfg.merchantId(), cfg.batchPrefix(),
                cfg.maxLinesPerBatch(), cfg.workDir());
        this.statements = new CustomerStatementService(orders, refunds, pg);
    }

    public Order placeOrder(String customerId, List<String> skus, long coupon, LocalDate day) {
        return checkout.place(customerId, skus, coupon, day);
    }

    public String requestRefund(String orderNo, List<Integer> lineNos, LocalDate day) {
        return refundService.request(orderNo, lineNos, day);
    }

    public RunResult runRefunds(LocalDate day) {
        return batchJob.run(day);
    }

    public CustomerStatement statement(String orderNo) {
        return statements.statement(orderNo);
    }

    public String statementText(String orderNo) {
        return StatementFormatter.format(statement(orderNo));
    }

    /** Lines sent to the PG that still have no PG refund number. */
    public List<RefundLine> awaitingPg() {
        return refunds.all().stream().filter(l -> l.status() == RefundLine.Status.SUBMITTED).toList();
    }
}
