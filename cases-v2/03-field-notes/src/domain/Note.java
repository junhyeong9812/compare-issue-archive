package domain;

import java.time.Instant;

/**
 * An inspection note. {@code updatedAt} is when the content last changed;
 * {@code revision} is the server's change counter used by the change feed.
 */
public record Note(String id, String folderId, String title, String body,
                   Instant createdAt, Instant updatedAt, long revision, boolean deleted) implements Audited<Note> {

    public static Note draft(String id, String folderId, String title, String body, Instant updatedAt) {
        return new Note(id, folderId, title, body, null, updatedAt, 0, false);
    }

    public Note withContent(String newTitle, String newBody, Instant changedAt) {
        return new Note(id, folderId, newTitle, newBody, createdAt, changedAt, revision, deleted);
    }

    public Note asDeleted(Instant changedAt) {
        return new Note(id, folderId, title, body, createdAt, changedAt, revision, true);
    }

    public Note movedTo(String newFolderId, Instant changedAt) {
        return new Note(id, newFolderId, title, body, createdAt, changedAt, revision, deleted);
    }

    @Override
    public Note withAudit(Instant created, Instant updated, long rev) {
        return new Note(id, folderId, title, body, created, updated, rev, deleted);
    }
}
