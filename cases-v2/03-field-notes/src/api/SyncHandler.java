package api;

import domain.Note;
import service.ApiException;
import service.ChangeFeed;
import service.SyncService;
import sync.IncomingChange;
import sync.PushResult;
import util.FormCodec;
import util.Timestamps;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mobile sync. Push body: one change per line, form-encoded
 * ({@code id=..&folder=..&title=..&body=..&editedAt=..[&deleted=true]}).
 * Headers: X-Device-Id, X-Device-Zone (used for timestamps sent without an offset).
 */
public class SyncHandler {
    private final SyncService sync;
    private final ChangeFeed feed;

    public SyncHandler(SyncService sync, ChangeFeed feed) {
        this.sync = sync;
        this.feed = feed;
    }

    Response push(Request req) {
        String device = req.header("X-Device-Id");
        if (device == null || device.isBlank()) {
            throw new ApiException(400, "X-Device-Id required");
        }
        ZoneId zone = zone(req.header("X-Device-Zone"));
        List<IncomingChange> changes = new ArrayList<>();
        for (String line : req.body().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            Map<String, String> f = FormCodec.decode(line);
            if (f.get("id") == null) {
                throw new ApiException(400, "change without id");
            }
            try {
                changes.add(new IncomingChange(f.get("id"), f.get("folder"), f.getOrDefault("title", ""), f.getOrDefault("body", ""),
                        Timestamps.parse(f.get("editedAt"), zone), Boolean.parseBoolean(f.get("deleted"))));
            } catch (IllegalArgumentException e) {
                throw new ApiException(400, e.getMessage());
            }
        }
        PushResult result = sync.push(device, changes);
        StringBuilder sb = new StringBuilder("{\"results\":[");
        for (int i = 0; i < result.items().size(); i++) {
            PushResult.Item item = result.items().get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(Json.str(item.noteId()))
                    .append(",\"status\":").append(Json.str(item.status().name().toLowerCase()))
                    .append(",\"reason\":").append(Json.str(item.reason())).append('}');
        }
        return new Response(200, sb.append("],\"cursor\":").append(result.cursor()).append('}').toString());
    }

    Response changes(Request req) {
        long since;
        try {
            since = Long.parseLong(req.param("since") == null ? "0" : req.param("since"));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "bad cursor");
        }
        List<Note> changed = feed.since(since);
        StringBuilder sb = new StringBuilder("{\"notes\":[");
        for (int i = 0; i < changed.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Json.note(changed.get(i)));
        }
        return new Response(200, sb.append("],\"cursor\":").append(feed.currentRevision()).append('}').toString());
    }

    private static ZoneId zone(String header) {
        if (header == null || header.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(header.trim());
        } catch (DateTimeException e) {
            throw new ApiException(400, "bad X-Device-Zone");
        }
    }
}
