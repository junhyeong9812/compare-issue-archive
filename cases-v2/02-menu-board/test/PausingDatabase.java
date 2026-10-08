import db.Database;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test double: holds one chosen thread right after it first reads a given row, so request timing is reproducible. */
public class PausingDatabase implements Database {
    private final Database delegate;
    private final AtomicBoolean fired = new AtomicBoolean();
    private volatile Thread target;
    private volatile String table;
    private volatile String id;
    final CountDownLatch reached = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);

    public PausingDatabase(Database delegate) {
        this.delegate = delegate;
    }

    void pauseAfterRead(Thread thread, String table, String id) {
        this.table = table;
        this.id = id;
        this.target = thread;
    }

    @Override
    public Map<String, Object> select(String t, String i) {
        Map<String, Object> row = delegate.select(t, i);
        if (Thread.currentThread() == target && t.equals(table) && i.equals(id) && fired.compareAndSet(false, true)) {
            reached.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return row;
    }

    @Override
    public List<Map<String, Object>> selectAll(String t) {
        return delegate.selectAll(t);
    }

    @Override
    public void insert(String t, String i, Map<String, Object> row) {
        delegate.insert(t, i, row);
    }

    @Override
    public void update(String t, String i, Map<String, Object> columns) {
        delegate.update(t, i, columns);
    }

    @Override
    public long version() {
        return delegate.version();
    }
}
