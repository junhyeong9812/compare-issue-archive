import api.Request;
import api.Response;
import app.Application;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TestSupport {
    static int failed = 0;

    private TestSupport() {
    }

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) {
            failed++;
        }
    }

    static void finish(String suite) {
        if (failed > 0) {
            System.out.println(suite + ": " + failed + " check(s) failed");
            System.exit(1);
        }
        System.out.println(suite + ": all checks passed");
    }

    static Map<String, String> body(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    static Response call(Application app, String method, String path, String token, Map<String, String> body) {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return app.handle(new Request(method, path, headers, body));
    }

    static String field(Response r, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }

    static String signUpAndLogin(Application app, String user, String password) {
        Response s = call(app, "POST", "/accounts", null, body("username", user, "email", user + "@example.com", "password", password));
        if (s.status() != 201) {
            throw new IllegalStateException("sign-up failed: " + s);
        }
        Response l = call(app, "POST", "/sessions", null, body("username", user, "password", password));
        if (l.status() != 201) {
            throw new IllegalStateException("login failed: " + l);
        }
        return field(l, "token");
    }
}
