package api;

import java.time.LocalDate;

/**
 * Ops CLI.
 *   report    FROM TO   daily settlement per partner
 *   reconcile FROM TO   compare with partner statements
 *   export    FROM TO   CSV for accounting
 *   board     DATE      exchange rate board
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: report|reconcile|export FROM TO | board DATE");
            System.exit(64);
        }
        SettlementApi api = SettlementApi.fromDefaultConfig();
        LocalDate a = LocalDate.parse(args[1]);
        LocalDate b = args.length > 2 ? LocalDate.parse(args[2]) : a;
        switch (args[0]) {
            case "report" -> System.out.print(api.dailyReport(a, b));
            case "reconcile" -> System.out.print(api.reconcileReport(a, b));
            case "export" -> System.out.print(api.exportCsv(a, b));
            case "board" -> System.out.print(api.rateBoardText(a));
            default -> {
                System.err.println("unknown command " + args[0]);
                System.exit(64);
            }
        }
    }
}
