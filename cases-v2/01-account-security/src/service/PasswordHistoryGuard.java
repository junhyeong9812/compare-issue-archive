package service;

import crypto.PasswordHasher;
import store.CredentialHistoryStore;

import java.util.List;

/** Answers "has this account used that password recently?" against the stored hashes. */
public class PasswordHistoryGuard {
    private final CredentialHistoryStore history;
    private final PasswordHasher hasher;

    public PasswordHistoryGuard(CredentialHistoryStore history, PasswordHasher hasher) {
        this.history = history;
        this.hasher = hasher;
    }

    public boolean usedRecently(String accountId, String candidate, int window) {
        if (window <= 0) {
            return false;
        }
        List<String> recent = history.recent(accountId, window);
        for (String hash : recent) {
            if (hasher.matches(candidate, hash)) {
                return true;
            }
        }
        return false;
    }

    public void remember(String accountId, String passwordHash) {
        history.append(accountId, passwordHash);
    }
}
