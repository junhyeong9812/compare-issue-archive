package api;

import service.ApiException;

public class Router {
    private final NotesHandler notes;
    private final FolderHandler folders;
    private final SyncHandler sync;

    public Router(NotesHandler notes, FolderHandler folders, SyncHandler sync) {
        this.notes = notes;
        this.folders = folders;
        this.sync = sync;
    }

    public Response handle(Request req) {
        try {
            return dispatch(req);
        } catch (ApiException e) {
            return new Response(e.status(), "{\"error\":" + Json.str(e.getMessage()) + "}");
        } catch (RuntimeException e) {
            return new Response(500, "{\"error\":\"internal error\"}");
        }
    }

    private Response dispatch(Request req) {
        String[] p = req.path().split("/");
        String m = req.method();
        if (p.length == 2 && p[1].equals("folders") && m.equals("POST")) {
            return folders.create(req);
        }
        if (p.length == 3 && p[1].equals("folders") && m.equals("PUT")) {
            return folders.rename(req, p[2]);
        }
        if (p.length == 2 && p[1].equals("notes") && m.equals("POST")) {
            return notes.create(req);
        }
        if (p.length == 3 && p[1].equals("notes")) {
            switch (m) {
                case "GET":
                    return notes.get(p[2]);
                case "PUT":
                    return notes.edit(req, p[2]);
                default:
                    break;
            }
        }
        if (p.length == 4 && p[1].equals("notes") && p[3].equals("folder") && m.equals("PUT")) {
            return notes.move(req, p[2]);
        }
        if (p.length == 3 && p[1].equals("sync")) {
            if (p[2].equals("push") && m.equals("POST")) {
                return sync.push(req);
            }
            if (p[2].equals("changes") && m.equals("GET")) {
                return sync.changes(req);
            }
        }
        return new Response(404, "{\"error\":\"no route\"}");
    }
}
