import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test double: pauses one chosen thread at its first access to a key, so interleavings are reproducible. */
public class GatedStore implements KeyValueStore {
    private final KeyValueStore delegate;
    private final AtomicBoolean fired = new AtomicBoolean();
    private volatile Thread target;
    private volatile String keyPrefix;
    final CountDownLatch reached = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);

    public GatedStore(KeyValueStore delegate) {
        this.delegate = delegate;
    }

    void pauseFirstAccess(Thread thread, String prefix) {
        this.keyPrefix = prefix;
        this.target = thread;
    }

    @Override
    public String get(String key) {
        String value = delegate.get(key);
        maybePause(key);
        return value;
    }

    @Override
    public void put(String key, String value) {
        maybePause(key);
        delegate.put(key, value);
    }

    private void maybePause(String key) {
        if (Thread.currentThread() == target && key.startsWith(keyPrefix) && fired.compareAndSet(false, true)) {
            reached.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
