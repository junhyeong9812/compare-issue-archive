package service;

import config.Settings;
import crypto.PasswordHasher;
import domain.Account;
import domain.PasswordPolicy;
import domain.PolicyViolation;
import store.AccountStore;
import util.EmailAddresses;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Sign-up and profile maintenance. */
public class AccountService {
    private static final Pattern USERNAME = Pattern.compile("[a-z][a-z0-9_.-]{2,31}");

    private final AccountStore accounts;
    private final PasswordHasher hasher;
    private final PasswordHistoryGuard historyGuard;
    private final PasswordPolicy policy;
    private final Clock clock;
    private final int displayNameMax;

    public AccountService(AccountStore accounts, PasswordHasher hasher, PasswordHistoryGuard historyGuard,
                          PasswordPolicy policy, Clock clock, Settings settings) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.historyGuard = historyGuard;
        this.policy = policy;
        this.clock = clock;
        this.displayNameMax = settings.intValue("profile.displayNameMax", 40);
    }

    public Account signUp(String username, String email, String password) {
        List<String> problems = new ArrayList<>();
        String normalizedEmail = EmailAddresses.normalize(email);
        if (username == null || !USERNAME.matcher(username).matches()) {
            problems.add("username must be 3-32 chars: lower-case letters, digits, '_', '.', '-'");
        } else if (accounts.findByUsername(username).isPresent()) {
            throw ValidationException.of(409, "username already taken");
        }
        if (!EmailAddresses.looksValid(normalizedEmail)) {
            problems.add("email is not valid");
        } else if (accounts.emailTaken(normalizedEmail)) {
            throw ValidationException.of(409, "email already registered");
        }
        for (PolicyViolation v : policy.check(username, password)) {
            problems.add(v.message());
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(422, problems);
        }
        String hash = hasher.hash(password);
        Account account = new Account(accounts.nextId(), username, normalizedEmail, username, hash, clock.instant(), false);
        accounts.save(account);
        historyGuard.remember(account.id(), hash);
        return account;
    }

    public Account rename(String accountId, String displayName) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ValidationException.of(404, "account not found"));
        String trimmed = displayName == null ? "" : displayName.strip();
        if (trimmed.isEmpty() || trimmed.length() > displayNameMax) {
            throw ValidationException.of(422, "display name must be 1-" + displayNameMax + " characters");
        }
        Account updated = account.withDisplayName(trimmed);
        accounts.save(updated);
        return updated;
    }

    public void disable(String accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ValidationException.of(404, "account not found"));
        accounts.save(account.withDisabled(true));
    }
}
