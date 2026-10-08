import api.Request;
import api.Response;
import app.Application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HiddenAccountCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    static Response call(Application app, String method, String path, Map<String, String> headers, Map<String, String> body) {
        return app.handle(new Request(method, path, headers, body));
    }

    static String token(Response r) {
        Matcher m = Pattern.compile("\"token\":\"([^\"]*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }

    static String id(Response r) {
        Matcher m = Pattern.compile("\"id\":\"([^\"]*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }

    static String pw(String user, int i) {
        return "Pw-" + (char) ('a' + i) + "-" + (1000 + i) + "-zz";
    }

    public static void main(String[] args) {
        int window = Integer.parseInt(System.getProperty("password.reuseWindow", "5"));
        String tag = "[window=" + window + "] ";
        Application app = Application.create(Clock.fixed(Instant.parse("2026-05-02T03:00:00Z"), ZoneOffset.UTC), "k-admin");

        // user path: P0 at sign-up, then P1..P(window)
        Response signed = call(app, "POST", "/accounts", Map.of(), Map.of("username", "heidi", "email", "heidi@example.org", "password", pw("heidi", 0)));
        check(signed.status() == 201, tag + "sign-up");
        String tok = token(call(app, "POST", "/sessions", Map.of(), Map.of("username", "heidi", "password", pw("heidi", 0))));
        Map<String, String> auth = Map.of("Authorization", "Bearer " + tok);
        for (int i = 1; i <= window; i++) {
            Response r = call(app, "PUT", "/me/password", auth, Map.of("currentPassword", pw("heidi", i - 1), "newPassword", pw("heidi", i)));
            check(r.status() == 200, tag + "fresh password P" + i + " accepted (got " + r.status() + ")");
        }
        String current = pw("heidi", window);
        for (int k = window; k >= 1; k--) {
            Response r = call(app, "PUT", "/me/password", auth, Map.of("currentPassword", current, "newPassword", pw("heidi", k)));
            check(r.status() == 422 && r.body().contains("used recently"), tag + "P" + k + " is within the last " + window + " -> refused (got " + r.status() + ")");
        }
        Response old = call(app, "PUT", "/me/password", auth, Map.of("currentPassword", current, "newPassword", pw("heidi", 0)));
        check(old.status() == 200, tag + "P0 is older than the window -> allowed (got " + old.status() + ")");
        check(call(app, "POST", "/sessions", Map.of(), Map.of("username", "heidi", "password", pw("heidi", 0))).status() == 201, tag + "login with P0 again");

        // admin reset path uses the same policy
        Response ivan = call(app, "POST", "/accounts", Map.of(), Map.of("username", "ivan", "email", "ivan@example.org", "password", pw("ivan", 7)));
        String ivanId = id(ivan);
        Map<String, String> admin = Map.of("X-Admin-Key", "k-admin");
        Response same = call(app, "POST", "/admin/accounts/" + ivanId + "/password-reset", admin, Map.of("newPassword", pw("ivan", 7)));
        check(same.status() == 422, tag + "admin reset to the current password is refused (got " + same.status() + ")");
        Response fresh = call(app, "POST", "/admin/accounts/" + ivanId + "/password-reset", admin, Map.of("newPassword", pw("ivan", 8)));
        check(fresh.status() == 200, tag + "admin reset to a new password works (got " + fresh.status() + ")");
        Response prev = call(app, "POST", "/admin/accounts/" + ivanId + "/password-reset", admin, Map.of("newPassword", pw("ivan", 7)));
        check(prev.status() == (window >= 2 ? 422 : 200), tag + "admin reset back to the previous password (got " + prev.status() + ")");

        // other rules unchanged
        Response shortPw = call(app, "PUT", "/me/password", auth, Map.of("currentPassword", pw("heidi", 0), "newPassword", "x1"));
        check(shortPw.status() == 422 && shortPw.body().contains("too short"), tag + "short password still refused");

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println(tag + "all hidden checks passed");
    }
}
