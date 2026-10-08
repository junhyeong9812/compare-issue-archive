import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Server clock the test can move forward. */
final class MutableClock extends Clock {
    private volatile Instant now;

    MutableClock(String iso) {
        this.now = Instant.parse(iso);
    }

    void set(String iso) {
        now = Instant.parse(iso);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
