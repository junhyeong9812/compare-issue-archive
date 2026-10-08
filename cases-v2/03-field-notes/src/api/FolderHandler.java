package api;

import domain.Folder;
import service.FolderService;

public class FolderHandler {
    private final FolderService folders;

    public FolderHandler(FolderService folders) {
        this.folders = folders;
    }

    Response create(Request req) {
        Folder f = folders.create(req.param("name"));
        return new Response(201, "{\"id\":" + Json.str(f.id()) + ",\"name\":" + Json.str(f.name()) + "}");
    }

    Response rename(Request req, String id) {
        Folder f = folders.rename(id, req.param("name"));
        return new Response(200, "{\"id\":" + Json.str(f.id()) + ",\"name\":" + Json.str(f.name()) + "}");
    }
}
