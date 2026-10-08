package util;

import java.util.Locale;

public final class EmailAddresses {
    private EmailAddresses() {
    }

    /** Lower-cases the domain part and trims; the local part is kept as typed. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        int at = s.lastIndexOf('@');
        if (at <= 0 || at == s.length() - 1) {
            return s;
        }
        return s.substring(0, at) + "@" + s.substring(at + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean looksValid(String email) {
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        return at > 0 && at < email.length() - 1 && email.indexOf(' ') < 0 && email.substring(at + 1).contains(".");
    }
}
