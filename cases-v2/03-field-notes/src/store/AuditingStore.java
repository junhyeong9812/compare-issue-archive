package store;

import domain.Audited;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base table for audited entities. Every save fills the audit columns
 * (createdAt on first save, updatedAt, and the next revision) so callers cannot forget them.
 */
public class AuditingStore<T extends Audited<T>> {
    private final Map<String, T> rows = new LinkedHashMap<>();
    private final Clock clock;
    private final AtomicLong revisions;

    protected AuditingStore(Clock clock, AtomicLong revisions) {
        this.clock = clock;
        this.revisions = revisions;
    }

    public synchronized T save(T entity) {
        Instant now = clock.instant();
        T previous = rows.get(entity.id());
        Instant created = previous != null ? previous.createdAt() : (entity.createdAt() != null ? entity.createdAt() : now);
        T stamped = entity.withAudit(created, now, revisions.incrementAndGet());
        rows.put(stamped.id(), stamped);
        return stamped;
    }

    public synchronized Optional<T> find(String id) {
        return Optional.ofNullable(rows.get(id));
    }

    public synchronized List<T> all() {
        return new ArrayList<>(rows.values());
    }

    /** Rows saved after the given revision, oldest change first. */
    public synchronized List<T> changedAfter(long revision) {
        List<T> result = new ArrayList<>();
        for (T row : rows.values()) {
            if (row.revision() > revision) {
                result.add(row);
            }
        }
        result.sort(Comparator.comparingLong(Audited::revision));
        return result;
    }
}
