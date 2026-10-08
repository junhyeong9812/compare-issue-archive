package domain;

import java.time.Instant;

/** Entities whose rows carry createdAt / updatedAt and a server revision number. */
public interface Audited<T> {
    String id();

    Instant createdAt();

    Instant updatedAt();

    long revision();

    T withAudit(Instant createdAt, Instant updatedAt, long revision);
}
