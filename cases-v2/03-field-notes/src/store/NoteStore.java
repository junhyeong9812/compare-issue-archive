package store;

import domain.Note;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NoteStore extends AuditingStore<Note> {
    public NoteStore(Clock clock, AtomicLong revisions) {
        super(clock, revisions);
    }

    public List<Note> inFolder(String folderId) {
        return all().stream().filter(n -> !n.deleted() && folderId.equals(n.folderId())).toList();
    }
}
