package app;

import api.AdminMenuHandler;
import api.PublicMenuHandler;
import api.Request;
import api.Response;
import api.Router;
import db.Database;
import orm.Transactions;
import query.MenuQueryService;
import repo.MenuRepository;
import service.CategoryService;
import service.MenuEditService;
import service.MenuOrderingService;
import util.AuditTrail;

import java.util.Map;

/** The menu-board server: staff edit the menu, guests read it. Requests arrive on many threads. */
public final class MenuApp {
    private final Router router;
    private final AuditTrail audit;

    private MenuApp(Router router, AuditTrail audit) {
        this.router = router;
        this.audit = audit;
    }

    public static MenuApp create(Database db) {
        Transactions tx = new Transactions(db);
        MenuRepository repo = new MenuRepository();
        AuditTrail audit = new AuditTrail();
        Router router = new Router(
                new PublicMenuHandler(new MenuQueryService(tx, repo, db)),
                new AdminMenuHandler(new CategoryService(tx, repo),
                        new MenuEditService(tx, repo, audit),
                        new MenuOrderingService(tx, repo, audit)));
        return new MenuApp(router, audit);
    }

    public Response handle(Request request) {
        return router.handle(request);
    }

    public Response handle(String method, String path, String staff, Map<String, String> params) {
        return router.handle(new Request(method, path, staff == null ? Map.of() : Map.of("X-Staff", staff), params));
    }

    public AuditTrail audit() {
        return audit;
    }
}
