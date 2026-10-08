import domain.Note;
import service.ConflictResolver;
import sync.IncomingChange;
import util.FormCodec;
import util.Timestamps;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

public class UnitChecks {
    public static void main(String[] args) {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        TestSupport.check(Timestamps.parse("2026-03-02T18:00:00+09:00", ZoneOffset.UTC).equals(Instant.parse("2026-03-02T09:00:00Z")), "timestamps: explicit offset");
        TestSupport.check(Timestamps.parse("2026-03-02T09:00:00Z", seoul).equals(Instant.parse("2026-03-02T09:00:00Z")), "timestamps: Z ignores device zone");
        TestSupport.check(Timestamps.parse("2026-03-02T18:00:00", seoul).equals(Instant.parse("2026-03-02T09:00:00Z")), "timestamps: local time uses device zone");
        TestSupport.check(Timestamps.parse("2026-07-01T08:30:00", ZoneId.of("Europe/Berlin")).equals(Instant.parse("2026-07-01T06:30:00Z")), "timestamps: summer time");
        boolean threw = false;
        try {
            Timestamps.parse("yesterday", seoul);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        TestSupport.check(threw, "timestamps: garbage rejected");

        Map<String, String> m = FormCodec.decode(FormCodec.encode(Map.of("body", "a&b=c 100%\nline2")));
        TestSupport.check("a&b=c 100%\nline2".equals(m.get("body")), "form codec: round trip");

        ConflictResolver resolver = new ConflictResolver();
        Note stored = new Note("n", "f", "t", "b", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"), 3, false);
        TestSupport.check(resolver.decide(null, change("2026-01-01T09:00:00Z")) == ConflictResolver.Decision.CREATE, "resolver: new note");
        TestSupport.check(resolver.decide(stored, change("2026-01-01T10:00:01Z")) == ConflictResolver.Decision.APPLY, "resolver: later edit wins");
        TestSupport.check(resolver.decide(stored, change("2026-01-01T09:59:59Z")) == ConflictResolver.Decision.KEEP_SERVER, "resolver: earlier edit loses");
        TestSupport.check(resolver.decide(stored, change("2026-01-01T10:00:00Z")) == ConflictResolver.Decision.KEEP_SERVER, "resolver: tie keeps server copy");

        TestSupport.finish("unit");
    }

    static IncomingChange change(String at) {
        return new IncomingChange("n", "f", "t2", "b2", Instant.parse(at), false);
    }
}
