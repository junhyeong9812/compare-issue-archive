import api.Response;
import app.NotesApp;

import java.util.Map;

public class NotesSyncTest {
    public static void main(String[] args) {
        MutableClock clock = new MutableClock("2026-03-02T08:00:00Z");
        NotesApp app = NotesApp.create(clock);
        String folder = TestSupport.field(app.handle("POST", "/folders", Map.of("name", "Plant 4")), "id");
        Response created = app.handle("POST", "/notes", Map.of("folder", folder, "title", "Pump room", "body", "routine check"));
        TestSupport.check(created.status() == 201, "web console creates a note");
        String note = TestSupport.field(created, "id");

        // web console edit
        clock.set("2026-03-02T08:10:00Z");
        app.handle("PUT", "/notes/" + note, Map.of("body", "routine check, pressure ok"));
        TestSupport.check("routine check, pressure ok".equals(TestSupport.field(app.handle("GET", "/notes/" + note, Map.of()), "body")), "web edit is saved");

        // a note created offline on a device shows up
        clock.set("2026-03-02T08:20:00Z");
        Response first = TestSupport.push(app, "tablet-7", "Asia/Seoul",
                TestSupport.change("dev-7-1", folder, "Boiler", "soot on burner", "2026-03-02T17:05:00"));
        TestSupport.check(first.status() == 200 && first.body().contains("\"status\":\"synced\""), "device push accepted");
        TestSupport.check("soot on burner".equals(TestSupport.field(app.handle("GET", "/notes/dev-7-1", Map.of()), "body")), "note created offline is on the server");

        // two inspectors edited the pump-room note while offline:
        //   tablet-7 at 18:00 Seoul time (09:00 UTC), uploaded at 11:00 UTC
        //   phone-2  at 10:00 UTC,                    uploaded at 11:30 UTC
        clock.set("2026-03-02T11:00:00Z");
        TestSupport.push(app, "tablet-7", "Asia/Seoul",
                TestSupport.change(note, folder, "Pump room", "valve 3 leaking", "2026-03-02T18:00:00"));
        clock.set("2026-03-02T11:30:00Z");
        Response late = TestSupport.push(app, "phone-2", null,
                TestSupport.change(note, folder, "Pump room", "valve 3 replaced, no leak", "2026-03-02T10:00:00Z"));
        TestSupport.check(late.status() == 200, "second device push accepted");

        // an even older offline edit arriving last must not win
        clock.set("2026-03-02T12:00:00Z");
        TestSupport.push(app, "laptop-1", "UTC",
                TestSupport.change(note, folder, "Pump room", "pressure ok", "2026-03-02T08:30:00Z"));

        String body = TestSupport.field(app.handle("GET", "/notes/" + note, Map.of()), "body");
        TestSupport.check(!"pressure ok".equals(body), "an edit made before the others does not replace them");
        TestSupport.check("valve 3 replaced, no leak".equals(body), "the most recent inspection edit is the one shown (got '" + body + "')");

        TestSupport.finish("notes sync");
    }
}
