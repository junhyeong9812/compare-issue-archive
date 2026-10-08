import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HiddenLoginCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    static long count(List<String> log, String prefix) {
        return log.stream().filter(l -> l.startsWith(prefix)).count();
    }

    static void scenario(String label, Map<String, String> headers) {
        LoginAttemptService attempts = new LoginAttemptService();
        AuthController controller = new AuthController(Map.of("carol", "pw-carol"), attempts);
        int last = -1;
        for (int i = 0; i < 5; i++) {
            last = controller.handle(headers, "carol", "bad" + i).status();
        }
        check(last == 401 || last == 423, label + ": wrong password answers 401/423, not " + last);
        check(attempts.isLocked("carol"), label + ": account locked after 5 failures");
        check(controller.handle(headers, "carol", "pw-carol").status() == 423, label + ": correct password rejected while locked");
        List<String> log = attempts.auditLog();
        check(count(log, "LOGIN_FAILED user=carol") == 5, label + ": 5 failure audit entries (got " + count(log, "LOGIN_FAILED user=carol") + ")");
        check(count(log, "ACCOUNT_LOCKED user=carol") == 1, label + ": lock audit entry present");
    }

    public static void main(String[] args) {
        scenario("missing header", new HashMap<>());
        scenario("empty header", Map.of("X-Site-Id", ""));
        scenario("overflowing header", Map.of("X-Site-Id", "99999999999999999999999"));
        scenario("decimal header", Map.of("X-Site-Id", "4.2"));
        scenario("numeric header", Map.of("X-Site-Id", "42"));

        LoginAttemptService attempts = new LoginAttemptService();
        AuthController controller = new AuthController(Map.of("dave", "pw"), attempts);
        controller.handle(Map.of("X-Site-Id", "42"), "dave", "nope");
        check(attempts.auditLog().contains("LOGIN_FAILED user=dave site=42 count=1"), "numeric site id still recorded in audit log");
        check(controller.handle(Map.of("X-Site-Id", "tablet"), "dave", "pw").status() == 200, "correct password with odd header still logs in");
        check(attempts.failureCount("dave") == 0, "successful login resets failures");

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println("all hidden checks passed");
    }
}
