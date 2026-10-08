package util;

/** Reference strings sent to the PG: [A-Za-z0-9-] only, at most 24 characters. */
public final class Refs {
    public static final int MAX = 24;

    private Refs() {}

    public static String sanitize(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-') {
                sb.append(c);
            } else if (c == '_' || c == ' ' || c == '/') {
                sb.append('-');
            }
        }
        String s = sb.toString();
        return s.length() > MAX ? s.substring(s.length() - MAX) : s;
    }
}
