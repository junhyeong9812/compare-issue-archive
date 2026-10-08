package service;

import domain.Note;
import sync.IncomingChange;

/** Whole-note conflicts: the later edit wins, ties keep what the server has. */
public final class ConflictResolver {
    public enum Decision { CREATE, APPLY, KEEP_SERVER }

    public Decision decide(Note stored, IncomingChange change) {
        if (stored == null) {
            return Decision.CREATE;
        }
        if (change.editedAt().isAfter(stored.updatedAt())) {
            return Decision.APPLY;
        }
        return Decision.KEEP_SERVER;
    }
}
