package api;

import service.LoginService;

import java.util.Map;

/** POST /sessions (login) and DELETE /sessions (logout). */
public class SessionHandler {
    private final LoginService logins;

    public SessionHandler(LoginService logins) {
        this.logins = logins;
    }

    Response login(Request req) {
        String token = logins.login(req.field("username"), req.field("password"));
        return new Response(201, Json.object(Map.of("token", token)));
    }

    Response logout(Request req) {
        logins.logout(Auth.bearer(req));
        return new Response(204, "");
    }
}
