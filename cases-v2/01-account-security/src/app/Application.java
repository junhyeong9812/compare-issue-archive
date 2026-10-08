package app;

import api.AccountHandler;
import api.AdminHandler;
import api.Request;
import api.Response;
import api.Router;
import api.SessionHandler;
import config.Settings;
import crypto.PasswordHasher;
import domain.PasswordPolicy;
import service.AccountService;
import service.LoginService;
import service.PasswordHistoryGuard;
import service.PasswordService;
import store.AccountStore;
import store.CredentialHistoryStore;
import store.SessionStore;

import java.time.Clock;
import java.time.Duration;

/** Wires the account service together. One instance = one running server. */
public final class Application {
    private final Router router;

    private Application(Router router) {
        this.router = router;
    }

    public static Application create(Clock clock, String adminKey) {
        Settings settings = Settings.load();
        AccountStore accounts = new AccountStore();
        SessionStore sessions = new SessionStore();
        PasswordHasher hasher = new PasswordHasher();
        PasswordHistoryGuard history = new PasswordHistoryGuard(new CredentialHistoryStore(), hasher);
        PasswordPolicy policy = PasswordPolicy.DEFAULT;
        Duration ttl = Duration.ofMinutes(settings.intValue("session.ttlMinutes", 30));

        AccountService accountService = new AccountService(accounts, hasher, history, policy, clock, settings);
        PasswordService passwordService = new PasswordService(accounts, sessions, hasher, history, policy, clock);
        LoginService loginService = new LoginService(accounts, sessions, hasher, clock, ttl);

        Router router = new Router(
                new AccountHandler(accountService, passwordService, loginService),
                new SessionHandler(loginService),
                new AdminHandler(adminKey, passwordService, accountService));
        return new Application(router);
    }

    public Response handle(Request request) {
        return router.handle(request);
    }
}
