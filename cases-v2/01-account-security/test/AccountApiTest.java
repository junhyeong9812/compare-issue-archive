import api.Response;
import app.Application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static java.util.Map.of;

public class AccountApiTest {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T09:00:00Z"), ZoneOffset.UTC);
        Application app = Application.create(clock, "admin-key");

        // sign-up validation
        Response weak = TestSupport.call(app, "POST", "/accounts", null,
                TestSupport.body("username", "frank", "email", "frank@example.com", "password", "short"));
        TestSupport.check(weak.status() == 422, "sign-up with a short password is rejected (got " + weak.status() + ")");
        String token = TestSupport.signUpAndLogin(app, "grace", "first-Pass-001");
        Response dup = TestSupport.call(app, "POST", "/accounts", null,
                TestSupport.body("username", "grace", "email", "other@example.com", "password", "first-Pass-001"));
        TestSupport.check(dup.status() == 409, "duplicate username is rejected (got " + dup.status() + ")");

        // profile
        Response renamed = TestSupport.call(app, "PUT", "/me/profile", token, of("displayName", "  Grace H. "));
        TestSupport.check(renamed.status() == 200 && "Grace H.".equals(TestSupport.field(renamed, "displayName")), "display name can be changed");

        // password change: wrong current password, policy, then success
        Response wrongCurrent = TestSupport.call(app, "PUT", "/me/password", token,
                of("currentPassword", "nope", "newPassword", "second-Pass-002"));
        TestSupport.check(wrongCurrent.status() == 403, "change with wrong current password is refused (got " + wrongCurrent.status() + ")");
        Response noDigit = TestSupport.call(app, "PUT", "/me/password", token,
                of("currentPassword", "first-Pass-001", "newPassword", "no-digits-at-all"));
        TestSupport.check(noDigit.status() == 422, "new password without a digit is refused (got " + noDigit.status() + ")");
        Response changed = TestSupport.call(app, "PUT", "/me/password", token,
                of("currentPassword", "first-Pass-001", "newPassword", "second-Pass-002"));
        TestSupport.check(changed.status() == 200, "password change succeeds (got " + changed.status() + ")");
        Response relogin = TestSupport.call(app, "POST", "/sessions", null, of("username", "grace", "password", "second-Pass-002"));
        TestSupport.check(relogin.status() == 201, "login with the new password works");

        // security policy: recently used passwords cannot be chosen again
        Response back = TestSupport.call(app, "PUT", "/me/password", token,
                of("currentPassword", "second-Pass-002", "newPassword", "first-Pass-001"));
        TestSupport.check(back.status() == 422, "switching back to the previous password is refused (got " + back.status() + " " + back.body() + ")");

        TestSupport.finish("account api");
    }
}
