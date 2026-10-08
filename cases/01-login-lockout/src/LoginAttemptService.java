import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginAttemptService {
    static final int MAX_FAILURES = 5;

    private final Map<String, Integer> failures = new HashMap<>();
    private final Set<String> locked = new HashSet<>();
    private final List<String> auditLog = new ArrayList<>();

    public void onLoginFailed(Long siteId, String user) {
        int count = failures.merge(user, 1, Integer::sum);
        auditLog.add("LOGIN_FAILED user=" + user + " site=" + siteId + " count=" + count);
        if (count >= MAX_FAILURES && locked.add(user)) {
            auditLog.add("ACCOUNT_LOCKED user=" + user);
        }
    }

    public void onLoginSucceeded(String user) {
        failures.remove(user);
    }

    public boolean isLocked(String user) {
        return locked.contains(user);
    }

    public int failureCount(String user) {
        return failures.getOrDefault(user, 0);
    }

    public List<String> auditLog() {
        return List.copyOf(auditLog);
    }
}
