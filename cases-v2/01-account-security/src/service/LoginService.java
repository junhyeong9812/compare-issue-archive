package service;

import crypto.PasswordHasher;
import domain.Account;
import store.AccountStore;
import store.SessionStore;
import util.Tokens;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LoginService {
    private final AccountStore accounts;
    private final SessionStore sessions;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final Duration ttl;

    public LoginService(AccountStore accounts, SessionStore sessions, PasswordHasher hasher, Clock clock, Duration ttl) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.hasher = hasher;
        this.clock = clock;
        this.ttl = ttl;
    }

    public String login(String username, String password) {
        Optional<Account> found = accounts.findByUsername(username);
        if (found.isEmpty() || found.get().disabled() || !hasher.matches(password, found.get().passwordHash())) {
            throw ValidationException.of(401, "invalid credentials");
        }
        String token = Tokens.newToken();
        sessions.put(new SessionStore.Session(token, found.get().id(), clock.instant().plus(ttl)));
        return token;
    }

    public String accountFor(String token) {
        Instant now = clock.instant();
        return sessions.find(token, now)
                .map(SessionStore.Session::accountId)
                .orElseThrow(() -> ValidationException.of(401, "not signed in"));
    }

    public void logout(String token) {
        sessions.remove(token);
    }
}
