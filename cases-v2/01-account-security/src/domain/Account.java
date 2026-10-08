package domain;

import java.time.Instant;

public record Account(String id, String username, String email, String displayName, String passwordHash,
                      Instant passwordChangedAt, boolean disabled) {

    public Account withPassword(String newHash, Instant changedAt) {
        return new Account(id, username, email, displayName, newHash, changedAt, disabled);
    }

    public Account withDisplayName(String name) {
        return new Account(id, username, email, name, passwordHash, passwordChangedAt, disabled);
    }

    public Account withDisabled(boolean value) {
        return new Account(id, username, email, displayName, passwordHash, passwordChangedAt, value);
    }
}
