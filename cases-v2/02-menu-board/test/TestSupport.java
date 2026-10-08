import api.Response;
import app.MenuApp;

import java.util.ArrayList;
import java.util.List;
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

    static String id(Response r) {
        Matcher m = Pattern.compile("\"id\":\"([^\"]*)\"").matcher(r.body());
        if (!m.find()) {
            throw new IllegalStateException("no id in " + r);
        }
        return m.group(1);
    }

    /** Item names on the public board, top to bottom. */
    static List<String> names(MenuApp app, String category) {
        Response r = app.handle("GET", "/menus/" + category, null, Map.of());
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\"name\":\"([^\"]*)\"").matcher(r.body());
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    static String price(MenuApp app, String category, String name) {
        Response r = app.handle("GET", "/menus/" + category, null, Map.of());
        Matcher m = Pattern.compile("\"name\":\"" + Pattern.quote(name) + "\",\"price\":\"([^\"]*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }
}
