package domain;

import config.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Password rules shared by sign-up, password change and admin reset. */
public final class PasswordPolicy {
    private static final Settings SETTINGS = Settings.load();
    private static final int MIN_LENGTH = SETTINGS.intValue("password.minLength", 10);
    private static final boolean REQUIRE_DIGIT = SETTINGS.boolValue("password.requireDigit", true);

    /** The policy every account flow uses, built from application settings. */
    public static final PasswordPolicy DEFAULT = new PasswordPolicy();

    // password history
    private static final int REUSE_WINDOW = SETTINGS.intValue("password.reuseWindow", 5);

    private final int minLength;
    private final boolean requireDigit;
    private final int reuseWindow;

    private PasswordPolicy() {
        this(MIN_LENGTH, REQUIRE_DIGIT, REUSE_WINDOW);
    }

    public PasswordPolicy(int minLength, boolean requireDigit, int reuseWindow) {
        this.minLength = minLength;
        this.requireDigit = requireDigit;
        this.reuseWindow = reuseWindow;
    }

    public int minLength() {
        return minLength;
    }

    /** How many of the most recent passwords (current one included) may not be chosen again. */
    public int reuseWindow() {
        return reuseWindow;
    }

    /** Checks the shape of a candidate password. History is checked separately. */
    public List<PolicyViolation> check(String username, String candidate) {
        List<PolicyViolation> violations = new ArrayList<>();
        if (candidate == null || candidate.length() < minLength) {
            violations.add(PolicyViolation.TOO_SHORT);
            return violations;
        }
        if (requireDigit && candidate.chars().noneMatch(Character::isDigit)) {
            violations.add(PolicyViolation.MISSING_DIGIT);
        }
        if (username != null && !username.isBlank()
                && candidate.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            violations.add(PolicyViolation.CONTAINS_USERNAME);
        }
        return violations;
    }
}
