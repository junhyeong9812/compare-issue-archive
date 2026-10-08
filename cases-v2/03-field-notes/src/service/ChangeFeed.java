package service;

import domain.Note;
import store.NoteStore;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** What devices pull after pushing: every note saved after their last cursor (server revision). */
public class ChangeFeed {
    private final NoteStore notes;
    private final AtomicLong revisions;

    public ChangeFeed(NoteStore notes, AtomicLong revisions) {
        this.notes = notes;
        this.revisions = revisions;
    }

    public List<Note> since(long cursor) {
        return notes.changedAfter(cursor);
    }

    public long currentRevision() {
        return revisions.get();
    }
}
