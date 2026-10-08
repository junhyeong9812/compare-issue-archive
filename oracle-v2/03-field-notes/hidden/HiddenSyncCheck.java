import api.Request;
import api.Response;
import app.NotesApp;
import util.FormCodec;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HiddenSyncCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    static final class Tick extends Clock {
        volatile Instant now;
        Tick(String iso) { now = Instant.parse(iso); }
        void at(String iso) { now = Instant.parse(iso); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    static String field(Response r, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(r.body());
        return m.find() ? m.group(1) : null;
    }

    static String line(String id, String folder, String body, String at, boolean deleted) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("folder", folder);
        f.put("title", "T-" + id);
        f.put("body", body);
        f.put("editedAt", at);
        if (deleted) f.put("deleted", "true");
        return FormCodec.encode(f);
    }

    static Response push(NotesApp app, String device, String zone, String... lines) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("X-Device-Id", device);
        if (zone != null) h.put("X-Device-Zone", zone);
        return app.handle(new Request("POST", "/sync/push", h, Map.of(), String.join("\n", lines)));
    }

    static String body(NotesApp app, String id) {
        return field(app.handle("GET", "/notes/" + id, Map.of()), "body");
    }

    public static void main(String[] args) {
        Tick clock = new Tick("2026-06-10T00:00:00Z");
        NotesApp app = NotesApp.create(clock);
        String folder = field(app.handle("POST", "/folders", Map.of("name", "Site B")), "id");

        // 1: device edits arrive in the opposite order of when they were made; upload delays are long
        clock.at("2026-06-10T01:00:00Z");
        push(app, "d1", "UTC", line("h1", folder, "v0", "2026-06-10T00:30:00Z", false), line("h2", folder, "w0", "2026-06-10T00:31:00Z", false));
        clock.at("2026-06-13T09:00:00Z");   // three days later
        push(app, "d2", "America/New_York", line("h1", folder, "v1-old", "2026-06-10T05:00:00", false));   // 09:00Z on 06-10
        clock.at("2026-06-13T09:05:00Z");
        push(app, "d3", null, line("h1", folder, "v2-newer", "2026-06-10T10:00:00Z", false));
        check("v2-newer".equals(body(app, "h1")), "1: newer edit uploaded last wins (got " + body(app, "h1") + ")");
        clock.at("2026-06-13T09:10:00Z");
        push(app, "d4", "UTC", line("h1", folder, "v-between", "2026-06-10T09:30:00Z", false));
        check("v2-newer".equals(body(app, "h1")), "1: edit made between the two, arriving last, does not win (got " + body(app, "h1") + ")");

        // 2: tie keeps the server copy
        clock.at("2026-06-13T10:00:00Z");
        push(app, "d5", "UTC", line("h1", folder, "same-time", "2026-06-10T10:00:00Z", false));
        check("v2-newer".equals(body(app, "h1")), "2: same edit time does not replace the stored note");

        // 3: an offline delete made later than an edit that arrives afterwards
        clock.at("2026-06-14T08:00:00Z");
        push(app, "d1", "Asia/Tokyo", line("h2", folder, "w0", "2026-06-11T09:00:00", true));   // 00:00Z 06-11
        clock.at("2026-06-14T08:30:00Z");
        push(app, "d2", "UTC", line("h2", folder, "w1", "2026-06-10T23:00:00Z", false));
        check(app.handle("GET", "/notes/h2", Map.of()).status() == 404, "3: note deleted later stays deleted");

        // 4: web console edit, then a device edit made after it (but uploaded hours later)
        clock.at("2026-06-15T12:00:00Z");
        String web = field(app.handle("POST", "/notes", Map.of("folder", folder, "title", "Gate", "body", "web-0")), "id");
        clock.at("2026-06-15T12:05:00Z");
        app.handle("PUT", "/notes/" + web, Map.of("body", "web-1"));
        clock.at("2026-06-15T20:00:00Z");
        push(app, "d6", "UTC", line(web, folder, "device-after-web", "2026-06-15T13:00:00Z", false));
        check("device-after-web".equals(body(app, web)), "4: device edit made after the web edit is applied (got " + body(app, web) + ")");
        clock.at("2026-06-15T20:10:00Z");
        push(app, "d7", "UTC", line(web, folder, "device-before-web", "2026-06-15T12:01:00Z", false));
        check("device-after-web".equals(body(app, web)), "4: device edit made before it is not (got " + body(app, web) + ")");

        // 5: one batch carrying several edits of one note, out of order, uploaded much later
        clock.at("2026-06-20T00:00:00Z");
        push(app, "d8", "UTC",
                line("h3", folder, "c", "2026-06-16T03:00:00Z", false),
                line("h3", folder, "a", "2026-06-16T01:00:00Z", false),
                line("h3", folder, "b", "2026-06-16T02:00:00Z", false));
        check("c".equals(body(app, "h3")), "5: latest edit within a batch wins");
        clock.at("2026-06-20T00:01:00Z");
        push(app, "d9", "UTC", line("h3", folder, "d", "2026-06-16T04:00:00Z", false));
        check("d".equals(body(app, "h3")), "5: a later edit from another device still applies (got " + body(app, "h3") + ")");

        // 6: change feed still reports late uploads to other devices
        Response feed = app.handle("GET", "/sync/changes", Map.of("since", "0"));
        check(feed.body().contains("\"id\":\"h3\"") && feed.body().contains("\"id\":\"" + web + "\""), "6: change feed lists synced notes");

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println("all hidden checks passed");
    }
}
