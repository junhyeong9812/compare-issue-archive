package util;

import java.text.DecimalFormat;

public final class Fmt {
    private Fmt() {}

    public static String won(long amount) {
        return new DecimalFormat("#,##0").format(amount) + "원";
    }

    public static String points(long points) {
        return new DecimalFormat("#,##0").format(points) + "P";
    }

    /** Member number on printed reports: only the last 4 characters are shown. */
    public static String mask(String memberNo) {
        if (memberNo.length() <= 4) return memberNo;
        return "*".repeat(memberNo.length() - 4) + memberNo.substring(memberNo.length() - 4);
    }
}
