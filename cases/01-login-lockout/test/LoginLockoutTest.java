import java.util.Map;

public class LoginLockoutTest {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        if (cond) {
            System.out.println("ok   - " + msg);
        } else {
            System.out.println("FAIL - " + msg);
            failed++;
        }
    }

    public static void main(String[] args) {
        LoginAttemptService attempts = new LoginAttemptService();
        AuthController controller = new AuthController(Map.of("alice", "s3cret", "bob", "hunter2"), attempts);

        // numeric site header: normal behaviour
        for (int i = 0; i < 5; i++) {
            controller.handle(Map.of("X-Site-Id", "12"), "bob", "wrong-" + i);
        }
        check(controller.handle(Map.of("X-Site-Id", "12"), "bob", "hunter2").status() == 423,
                "bob is locked after 5 wrong passwords (X-Site-Id: 12)");

        // the mobile client sends a non-numeric site header
        Map<String, String> mobile = Map.of("X-Site-Id", "mobile-7");
        for (int i = 0; i < 5; i++) {
            controller.handle(mobile, "alice", "guess-" + i);
        }
        check(attempts.failureCount("alice") == 5, "alice has 5 recorded failures (was " + attempts.failureCount("alice") + ")");
        AuthController.Response r = controller.handle(mobile, "alice", "s3cret");
        check(r.status() == 423, "alice is locked after 5 wrong passwords (got " + r.status() + ")");

        if (failed > 0) {
            System.out.println(failed + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
