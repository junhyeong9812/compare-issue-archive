package app;

import api.FolderHandler;
import api.NotesHandler;
import api.Request;
import api.Response;
import api.Router;
import api.SyncHandler;
import service.ChangeFeed;
import service.ConflictResolver;
import service.FolderService;
import service.NoteService;
import service.SyncService;
import store.FolderStore;
import store.NoteStore;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Field-inspection notes: web console + offline-capable mobile app syncing through /sync. */
public final class NotesApp {
    private final Router router;

    private NotesApp(Router router) {
        this.router = router;
    }

    public static NotesApp create(Clock clock) {
        AtomicLong revisions = new AtomicLong();
        NoteStore notes = new NoteStore(clock, revisions);
        FolderStore folders = new FolderStore(clock, revisions);
        ChangeFeed feed = new ChangeFeed(notes, revisions);
        Router router = new Router(
                new NotesHandler(new NoteService(notes, folders, clock)),
                new FolderHandler(new FolderService(folders, clock)),
                new SyncHandler(new SyncService(notes, folders, new ConflictResolver(), feed), feed));
        return new NotesApp(router);
    }

    public Response handle(Request request) {
        return router.handle(request);
    }

    public Response handle(String method, String path, Map<String, String> params) {
        return router.handle(new Request(method, path, Map.of(), params, null));
    }
}
