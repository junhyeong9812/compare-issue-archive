import config.Settings;
import crypto.PasswordHasher;
import domain.PasswordPolicy;
import domain.PolicyViolation;
import service.PasswordHistoryGuard;
import store.CredentialHistoryStore;
import util.EmailAddresses;

import java.util.List;
import java.util.Properties;

import static java.util.List.of;

public class UnitChecks {
    public static void main(String[] args) {
        PasswordHasher hasher = new PasswordHasher();
        String h1 = hasher.hash("correct horse 1");
        String h2 = hasher.hash("correct horse 1");
        TestSupport.check(!h1.equals(h2), "hasher: same password hashes differently (salted)");
        TestSupport.check(hasher.matches("correct horse 1", h1) && hasher.matches("correct horse 1", h2), "hasher: both hashes verify");
        TestSupport.check(!hasher.matches("correct horse 2", h1), "hasher: wrong password does not verify");
        TestSupport.check(!hasher.matches("x", "garbage"), "hasher: malformed hash does not verify");

        Properties p = new Properties();
        p.setProperty("a.int", " 7 ");
        p.setProperty("a.bad", "seven");
        p.setProperty("a.flag", "false");
        Settings s = Settings.of(p);
        TestSupport.check(s.intValue("a.int", 1) == 7, "settings: trims ints");
        TestSupport.check(s.intValue("a.bad", 3) == 3, "settings: bad int falls back");
        TestSupport.check(s.intValue("missing", 4) == 4, "settings: missing int falls back");
        TestSupport.check(!s.boolValue("a.flag", true), "settings: booleans");

        TestSupport.check("Bob.Smith@example.com".equals(EmailAddresses.normalize(" Bob.Smith@EXAMPLE.com ")), "email: domain lower-cased, local part kept");
        TestSupport.check(!EmailAddresses.looksValid("no-at-sign"), "email: rejects missing @");

        PasswordPolicy policy = new PasswordPolicy(10, true, 5);
        TestSupport.check(policy.check("erin", "short1").equals(of(PolicyViolation.TOO_SHORT)), "policy: too short");
        TestSupport.check(policy.check("erin", "no digits here").equals(of(PolicyViolation.MISSING_DIGIT)), "policy: digit required");
        TestSupport.check(policy.check("erin", "hello-erin-2024").contains(PolicyViolation.CONTAINS_USERNAME), "policy: username inside password");
        TestSupport.check(policy.check("erin", "a perfectly fine 9").isEmpty(), "policy: good password");

        CredentialHistoryStore store = new CredentialHistoryStore();
        for (int i = 0; i < 30; i++) {
            store.append("u1", "hash-" + i);
        }
        TestSupport.check(store.size("u1") == 24, "history: capped");
        TestSupport.check(store.recent("u1", 3).equals(List.of("hash-29", "hash-28", "hash-27")), "history: newest first");
        TestSupport.check(store.recent("nobody", 3).isEmpty(), "history: unknown account");

        PasswordHistoryGuard guard = new PasswordHistoryGuard(new CredentialHistoryStore(), hasher);
        guard.remember("u2", hasher.hash("first password 1"));
        guard.remember("u2", hasher.hash("second password 2"));
        TestSupport.check(guard.usedRecently("u2", "first password 1", 2), "guard: older password inside window");
        TestSupport.check(!guard.usedRecently("u2", "first password 1", 1), "guard: older password outside window");

        TestSupport.finish("unit");
    }
}
