package api;

import service.ValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps "METHOD /path" to handlers and turns service errors into responses. */
public class Router {
    private final AccountHandler accounts;
    private final SessionHandler sessions;
    private final AdminHandler admin;

    public Router(AccountHandler accounts, SessionHandler sessions, AdminHandler admin) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.admin = admin;
    }

    public Response handle(Request req) {
        try {
            return dispatch(req);
        } catch (ValidationException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", e.problems().get(0));
            body.put("problems", e.problems());
            return new Response(e.status(), Json.object(body));
        } catch (RuntimeException e) {
            return new Response(500, Json.object(Map.of("error", "internal error")));
        }
    }

    private Response dispatch(Request req) {
        String route = req.method() + " " + req.path();
        switch (route) {
            case "POST /accounts":
                return accounts.signUp(req);
            case "POST /sessions":
                return sessions.login(req);
            case "DELETE /sessions":
                return sessions.logout(req);
            case "PUT /me/password":
                return accounts.changePassword(req);
            case "PUT /me/profile":
                return accounts.updateProfile(req);
            default:
                break;
        }
        String[] parts = req.path().split("/");
        // /admin/accounts/{id}/password-reset , /admin/accounts/{id}/disable
        if (parts.length == 5 && parts[1].equals("admin") && parts[2].equals("accounts") && req.method().equals("POST")) {
            if (parts[4].equals("password-reset")) {
                return admin.resetPassword(req, parts[3]);
            }
            if (parts[4].equals("disable")) {
                return admin.disable(req, parts[3]);
            }
        }
        return new Response(404, Json.object(Map.of("error", "no route for " + route)));
    }
}
