package util;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class Tokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Tokens() {
    }

    public static String newToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
