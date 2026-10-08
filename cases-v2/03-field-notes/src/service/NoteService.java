package service;

import domain.Note;
import store.FolderStore;
import store.NoteStore;
import util.Ids;

import java.time.Clock;

/** Edits made in the web console (always online). */
public class NoteService {
    private static final int MAX_TITLE = 120;

    private final NoteStore notes;
    private final FolderStore folders;
    private final Clock clock;
    private final Ids ids = new Ids("note-");

    public NoteService(NoteStore notes, FolderStore folders, Clock clock) {
        this.notes = notes;
        this.folders = folders;
        this.clock = clock;
    }

    public Note create(String folderId, String title, String body) {
        requireFolder(folderId);
        return notes.save(Note.draft(ids.next(), folderId, cleanTitle(title), body == null ? "" : body, clock.instant()));
    }

    public Note edit(String id, String title, String body) {
        Note note = get(id);
        String newTitle = title == null ? note.title() : cleanTitle(title);
        String newBody = body == null ? note.body() : body;
        return notes.save(note.withContent(newTitle, newBody, clock.instant()));
    }

    public Note move(String id, String folderId) {
        requireFolder(folderId);
        return notes.save(get(id).movedTo(folderId, clock.instant()));
    }

    public Note get(String id) {
        Note note = notes.find(id).orElseThrow(() -> new ApiException(404, "note not found"));
        if (note.deleted()) {
            throw new ApiException(404, "note not found");
        }
        return note;
    }

    private void requireFolder(String folderId) {
        if (folderId == null || folders.find(folderId).isEmpty()) {
            throw new ApiException(400, "unknown folder");
        }
    }

    private static String cleanTitle(String title) {
        String t = title == null ? "" : title.strip();
        if (t.isEmpty() || t.length() > MAX_TITLE) {
            throw new ApiException(400, "title must be 1-" + MAX_TITLE + " characters");
        }
        return t;
    }
}
