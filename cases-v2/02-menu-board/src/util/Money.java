package util;

public final class Money {
    private Money() {
    }

    public static String format(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return sign + "$" + (abs / 100) + "." + String.format("%02d", abs % 100);
    }

    /** "4.5", "$4.50", "4" → cents. */
    public static long parse(String text) {
        String s = text.trim();
        if (s.startsWith("$")) {
            s = s.substring(1);
        }
        int dot = s.indexOf('.');
        if (dot < 0) {
            return Long.parseLong(s) * 100;
        }
        String frac = (s.substring(dot + 1) + "00").substring(0, 2);
        return Long.parseLong(s.substring(0, dot)) * 100 + Long.parseLong(frac);
    }
}
