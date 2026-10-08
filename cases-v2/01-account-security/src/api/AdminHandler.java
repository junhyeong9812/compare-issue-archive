package api;

import service.AccountService;
import service.PasswordService;
import service.ValidationException;

import java.util.Map;

/** Back-office endpoints, guarded by a static admin key. */
public class AdminHandler {
    private final String adminKey;
    private final PasswordService passwords;
    private final AccountService accounts;

    public AdminHandler(String adminKey, PasswordService passwords, AccountService accounts) {
        this.adminKey = adminKey;
        this.passwords = passwords;
        this.accounts = accounts;
    }

    Response resetPassword(Request req, String accountId) {
        requireAdmin(req);
        passwords.adminReset(accountId, req.field("newPassword"));
        return Response.ok(Json.object(Map.of("status", "password reset")));
    }

    Response disable(Request req, String accountId) {
        requireAdmin(req);
        accounts.disable(accountId);
        return Response.ok(Json.object(Map.of("status", "disabled")));
    }

    private void requireAdmin(Request req) {
        String key = req.header("X-Admin-Key");
        if (key == null || !key.equals(adminKey)) {
            throw ValidationException.of(403, "admin key required");
        }
    }
}
