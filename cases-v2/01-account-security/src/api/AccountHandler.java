package api;

import domain.Account;
import service.AccountService;
import service.LoginService;
import service.PasswordService;

import java.util.LinkedHashMap;
import java.util.Map;

/** /accounts and /me endpoints. */
public class AccountHandler {
    private final AccountService accounts;
    private final PasswordService passwords;
    private final LoginService logins;

    public AccountHandler(AccountService accounts, PasswordService passwords, LoginService logins) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.logins = logins;
    }

    Response signUp(Request req) {
        Account a = accounts.signUp(req.field("username"), req.field("email"), req.field("password"));
        return new Response(201, Json.object(view(a)));
    }

    Response changePassword(Request req) {
        String accountId = logins.accountFor(Auth.bearer(req));
        passwords.change(accountId, req.field("currentPassword"), req.field("newPassword"));
        return Response.ok(Json.object(Map.of("status", "password changed")));
    }

    Response updateProfile(Request req) {
        String accountId = logins.accountFor(Auth.bearer(req));
        Account a = accounts.rename(accountId, req.field("displayName"));
        return Response.ok(Json.object(view(a)));
    }

    private static Map<String, Object> view(Account a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("username", a.username());
        m.put("email", a.email());
        m.put("displayName", a.displayName());
        return m;
    }
}
