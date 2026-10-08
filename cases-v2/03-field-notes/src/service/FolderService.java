package service;

import domain.Folder;
import store.FolderStore;
import util.Ids;

import java.time.Clock;

public class FolderService {
    private final FolderStore folders;
    private final Clock clock;
    private final Ids ids = new Ids("folder-");

    public FolderService(FolderStore folders, Clock clock) {
        this.folders = folders;
        this.clock = clock;
    }

    public Folder create(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(400, "name required");
        }
        if (folders.byName(name.strip()).isPresent()) {
            throw new ApiException(409, "folder exists");
        }
        return folders.save(new Folder(ids.next(), name.strip(), null, null, 0));
    }

    public Folder rename(String id, String name) {
        Folder f = folders.find(id).orElseThrow(() -> new ApiException(404, "folder not found"));
        return folders.save(f.renamed(name.strip(), clock.instant()));
    }
}
