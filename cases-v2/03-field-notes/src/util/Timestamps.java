package util;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Device timestamps arrive as ISO-8601 text, with an offset ("...+09:00", "...Z") or as local time plus a zone. */
public final class Timestamps {
    private Timestamps() {
    }

    public static Instant parse(String text, ZoneId deviceZone) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("timestamp required");
        }
        String s = text.trim();
        try {
            if (s.endsWith("Z") || s.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(s).toInstant();
            }
            return LocalDateTime.parse(s).atZone(deviceZone).toInstant();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("bad timestamp: " + text, e);
        }
    }

    public static String format(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
