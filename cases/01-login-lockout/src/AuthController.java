import java.util.Map;

public class AuthController {
    public record Response(int status, String body) {}

    private final Map<String, String> passwords;
    private final LoginAttemptService attempts;

    public AuthController(Map<String, String> passwords, LoginAttemptService attempts) {
        this.passwords = passwords;
        this.attempts = attempts;
    }

    public Response handle(Map<String, String> headers, String user, String password) {
        try {
            return login(headers, user, password);
        } catch (RuntimeException e) {
            return new Response(500, "internal error");
        }
    }

    private Response login(Map<String, String> headers, String user, String password) {
        if (attempts.isLocked(user)) {
            return new Response(423, "account locked");
        }
        if (!password.equals(passwords.get(user))) {
            attempts.onLoginFailed(parseSiteId(headers.get("X-Site-Id")), user);
            return new Response(401, "invalid credentials");
        }
        attempts.onLoginSucceeded(user);
        return new Response(200, "welcome " + user);
    }

    static Long parseSiteId(String header) {
        return Long.valueOf(header);
    }
}
