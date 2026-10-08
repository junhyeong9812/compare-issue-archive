package store;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SessionStore {
    public record Session(String token, String accountId, Instant expiresAt) {}

    private final Map<String, Session> sessions = new HashMap<>();

    public synchronized void put(Session session) {
        sessions.put(session.token(), session);
    }

    public synchronized Optional<Session> find(String token, Instant now) {
        Session s = sessions.get(token);
        if (s == null || !now.isBefore(s.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public synchronized void remove(String token) {
        sessions.remove(token);
    }

    public synchronized void removeAllFor(String accountId) {
        sessions.values().removeIf(s -> s.accountId().equals(accountId));
    }
}
