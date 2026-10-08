package service;

import domain.Note;
import store.FolderStore;
import store.NoteStore;
import sync.IncomingChange;
import sync.PushResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Devices work offline and upload their edits in batches when they get a connection.
 * After a push the device pulls the change feed, so a change the server keeps
 * ({@link ConflictResolver.Decision#KEEP_SERVER}) is still reported as synced: the device will receive the server copy.
 */
public class SyncService {
    private final NoteStore notes;
    private final FolderStore folders;
    private final ConflictResolver resolver;
    private final ChangeFeed feed;

    public SyncService(NoteStore notes, FolderStore folders, ConflictResolver resolver, ChangeFeed feed) {
        this.notes = notes;
        this.folders = folders;
        this.resolver = resolver;
        this.feed = feed;
    }

    public PushResult push(String deviceId, List<IncomingChange> changes) {
        List<IncomingChange> ordered = new ArrayList<>(changes);
        // a device may queue several edits of the same note; apply them in the order they were made
        ordered.sort(Comparator.comparing(IncomingChange::editedAt));
        List<PushResult.Item> results = new ArrayList<>();
        for (IncomingChange change : ordered) {
            if (change.folderId() != null && folders.find(change.folderId()).isEmpty()) {
                results.add(new PushResult.Item(change.noteId(), PushResult.Status.REJECTED, "unknown folder"));
                continue;
            }
            Note stored = notes.find(change.noteId()).orElse(null);
            switch (resolver.decide(stored, change)) {
                case CREATE -> notes.save(Note.draft(change.noteId(), change.folderId(), change.title(), change.body(), change.editedAt()));
                case APPLY -> notes.save(change.deleted()
                        ? stored.asDeleted(change.editedAt())
                        : stored.withContent(change.title(), change.body(), change.editedAt()));
                case KEEP_SERVER -> {
                }
            }
            results.add(new PushResult.Item(change.noteId(), PushResult.Status.SYNCED, null));
        }
        return new PushResult(List.copyOf(results), feed.currentRevision());
    }
}
