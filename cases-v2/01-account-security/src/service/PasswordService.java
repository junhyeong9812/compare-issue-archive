package service;

import crypto.PasswordHasher;
import domain.Account;
import domain.PasswordPolicy;
import domain.PolicyViolation;
import store.AccountStore;
import store.SessionStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Password change (by the user) and password reset (by an administrator). */
public class PasswordService {
    private final AccountStore accounts;
    private final SessionStore sessions;
    private final PasswordHasher hasher;
    private final PasswordHistoryGuard historyGuard;
    private final PasswordPolicy policy;
    private final Clock clock;

    public PasswordService(AccountStore accounts, SessionStore sessions, PasswordHasher hasher,
                           PasswordHistoryGuard historyGuard, PasswordPolicy policy, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.hasher = hasher;
        this.historyGuard = historyGuard;
        this.policy = policy;
        this.clock = clock;
    }

    public void change(String accountId, String currentPassword, String newPassword) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ValidationException.of(404, "account not found"));
        if (!hasher.matches(currentPassword, account.passwordHash())) {
            throw ValidationException.of(403, "current password is incorrect");
        }
        apply(account, newPassword);
    }

    public void adminReset(String accountId, String newPassword) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ValidationException.of(404, "account not found"));
        apply(account, newPassword);
        sessions.removeAllFor(accountId);
    }

    private void apply(Account account, String newPassword) {
        List<String> problems = new ArrayList<>();
        for (PolicyViolation v : policy.check(account.username(), newPassword)) {
            problems.add(v.message());
        }
        if (problems.isEmpty() && historyGuard.usedRecently(account.id(), newPassword, policy.reuseWindow())) {
            problems.add(PolicyViolation.RECENTLY_USED.message());
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(422, problems);
        }
        String hash = hasher.hash(newPassword);
        accounts.save(account.withPassword(hash, clock.instant()));
        historyGuard.remember(account.id(), hash);
    }
}
