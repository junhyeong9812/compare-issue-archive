package store;

import domain.Account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AccountStore {
    private final Map<String, Account> byId = new LinkedHashMap<>();
    private final Map<String, String> idByUsername = new LinkedHashMap<>();
    private int sequence = 0;

    public synchronized String nextId() {
        sequence++;
        return "acc-" + sequence;
    }

    public synchronized void save(Account account) {
        Account previous = byId.put(account.id(), account);
        if (previous != null && !previous.username().equals(account.username())) {
            idByUsername.remove(previous.username());
        }
        idByUsername.put(account.username(), account.id());
    }

    public synchronized Optional<Account> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public synchronized Optional<Account> findByUsername(String username) {
        String id = idByUsername.get(username);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public synchronized boolean emailTaken(String email) {
        return byId.values().stream().anyMatch(a -> a.email().equals(email));
    }

    public synchronized List<Account> all() {
        Collection<Account> values = byId.values();
        return new ArrayList<>(values);
    }
}
