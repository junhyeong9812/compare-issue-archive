package domain;

import java.time.Instant;

public record Folder(String id, String name, Instant createdAt, Instant updatedAt, long revision) implements Audited<Folder> {
    public Folder renamed(String newName, Instant at) {
        return new Folder(id, newName, createdAt, at, revision);
    }

    @Override
    public Folder withAudit(Instant created, Instant updated, long rev) {
        return new Folder(id, name, created, updated, rev);
    }
}
