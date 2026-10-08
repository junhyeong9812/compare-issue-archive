package store;

import domain.Folder;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FolderStore extends AuditingStore<Folder> {
    public FolderStore(Clock clock, AtomicLong revisions) {
        super(clock, revisions);
    }

    public Optional<Folder> byName(String name) {
        return all().stream().filter(f -> f.name().equalsIgnoreCase(name)).findFirst();
    }
}
