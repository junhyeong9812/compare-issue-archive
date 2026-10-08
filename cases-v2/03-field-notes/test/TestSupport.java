import api.Request;
import api.Response;
import app.NotesApp;
import util.FormCodec;

import java.util.LinkedHashMap;
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

    static String field(Response r, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }

    static String change(String id, String folder, String title, String body, String editedAt) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("folder", folder);
        f.put("title", title);
        f.put("body", body);
        f.put("editedAt", editedAt);
        return FormCodec.encode(f);
    }

    static Response push(NotesApp app, String device, String zone, String... lines) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Device-Id", device);
        if (zone != null) {
            headers.put("X-Device-Zone", zone);
        }
        return app.handle(new Request("POST", "/sync/push", headers, Map.of(), String.join("\n", lines)));
    }
}
