package util;

import java.util.concurrent.atomic.AtomicLong;

public final class Ids {
    private final AtomicLong next = new AtomicLong();
    private final String prefix;

    public Ids(String prefix) {
        this.prefix = prefix;
    }

    public String next() {
        return prefix + next.incrementAndGet();
    }
}
