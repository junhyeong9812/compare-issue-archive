package store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Password hashes per account, oldest first. The newest entry is the current password. */
public class CredentialHistoryStore {
    static final int MAX_ENTRIES = 24;

    private final Map<String, List<String>> history = new HashMap<>();

    public synchronized void append(String accountId, String passwordHash) {
        List<String> entries = history.computeIfAbsent(accountId, k -> new ArrayList<>());
        entries.add(passwordHash);
        if (entries.size() > MAX_ENTRIES) {
            entries.subList(0, entries.size() - MAX_ENTRIES).clear();
        }
    }

    /** The {@code count} most recent hashes, newest first. */
    public synchronized List<String> recent(String accountId, int count) {
        List<String> entries = history.getOrDefault(accountId, List.of());
        List<String> result = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(entries.get(i));
        }
        return result;
    }

    public synchronized int size(String accountId) {
        return history.getOrDefault(accountId, List.of()).size();
    }
}
