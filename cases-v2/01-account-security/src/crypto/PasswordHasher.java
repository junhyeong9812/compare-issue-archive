package crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted SHA-256, stored as {@code salt$digest} (both base64). */
public final class PasswordHasher {
    private static final int SALT_BYTES = 16;
    private static final int ROUNDS = 1_000;

    private final SecureRandom random = new SecureRandom();

    public String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return encode(salt) + "$" + encode(digest(salt, raw));
    }

    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        int sep = stored.indexOf('$');
        if (sep <= 0) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
        byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
        return MessageDigest.isEqual(expected, digest(salt, raw));
    }

    private static byte[] digest(byte[] salt, String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = raw.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < ROUNDS; i++) {
                md.reset();
                md.update(salt);
                md.update(out);
                out = md.digest();
            }
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}
