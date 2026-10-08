import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HiddenShipmentCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    /** Pauses the chosen thread right before its first write to a key with the given prefix. */
    static class PutGate implements KeyValueStore {
        final KeyValueStore delegate = new InMemoryStore();
        final AtomicBoolean fired = new AtomicBoolean();
        final CountDownLatch reached = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile Thread target;
        volatile String prefix;

        public String get(String key) {
            return delegate.get(key);
        }

        public void put(String key, String value) {
            if (Thread.currentThread() == target && key.startsWith(prefix) && fired.compareAndSet(false, true)) {
                reached.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            delegate.put(key, value);
        }
    }

    static void race(PutGate gate, Thread paused, Runnable other, String otherName) throws Exception {
        paused.start();
        gate.reached.await(2, TimeUnit.SECONDS);
        Thread t = new Thread(other, otherName);
        t.start();
        t.join(500);
        gate.release.countDown();
        paused.join(5000);
        t.join(5000);
    }

    public static void main(String[] args) throws Exception {
        // A: the sync is about to write S3 when the operator labels S3
        {
            PutGate gate = new PutGate();
            ShipmentRepository repo = new ShipmentRepository(gate);
            AtomicReference<String> carrier = new AtomicReference<>("LABEL_CREATED");
            CarrierSync sync = new CarrierSync(repo, id -> carrier.get(), List.of("S2", "S3"));
            LabelService labels = new LabelService(repo);
            sync.tick();
            carrier.set("DELIVERED");
            Thread syncThread = new Thread(sync::tick, "carrier-sync");
            gate.prefix = "shipment:S3";
            gate.target = syncThread;
            race(gate, syncThread, () -> labels.setLabel("S3", "VIP"), "operator-ui");
            Shipment s3 = repo.load("S3");
            check("VIP".equals(s3.label()), "A: label written while sync is mid-write survives (got " + s3.label() + ")");
            check("DELIVERED".equals(s3.status()), "A: status from sync stored (got " + s3.status() + ")");
            check("DELIVERED".equals(repo.load("S2").status()), "A: other shipment synced");
        }
        // B: the operator's write to S2 is delayed while a sync runs
        {
            PutGate gate = new PutGate();
            ShipmentRepository repo = new ShipmentRepository(gate);
            AtomicReference<String> carrier = new AtomicReference<>("IN_TRANSIT");
            CarrierSync sync = new CarrierSync(repo, id -> carrier.get(), List.of("S2"));
            LabelService labels = new LabelService(repo);
            sync.tick();
            carrier.set("OUT_FOR_DELIVERY");
            Thread uiThread = new Thread(() -> labels.setLabel("S2", "cold-chain"), "operator-ui");
            gate.prefix = "shipment:S2";
            gate.target = uiThread;
            race(gate, uiThread, sync::tick, "carrier-sync");
            Shipment s2 = repo.load("S2");
            check("OUT_FOR_DELIVERY".equals(s2.status()), "B: status from sync is not rolled back (got " + s2.status() + ")");
            check("cold-chain".equals(s2.label()), "B: label stored (got " + s2.label() + ")");
        }
        // C: sequential behaviour unchanged
        {
            ShipmentRepository repo = new ShipmentRepository(new InMemoryStore());
            AtomicReference<String> carrier = new AtomicReference<>("IN_TRANSIT");
            CarrierSync sync = new CarrierSync(repo, id -> carrier.get(), List.of("S9"));
            LabelService labels = new LabelService(repo);
            check("UNKNOWN".equals(repo.load("S9").status()) && repo.load("S9").label() == null, "C: unknown shipment defaults");
            labels.setLabel("S9", "oversize");
            sync.tick();
            labels.setLabel("S9", "oversize-2");
            carrier.set("DELIVERED");
            sync.tick();
            Shipment s = repo.load("S9");
            check("oversize-2".equals(s.label()) && "DELIVERED".equals(s.status()), "C: sequential updates " + s);
            labels.setLabel("S9", null);
            sync.tick();
            check(repo.load("S9").label() == null, "C: label can be cleared");
        }

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println("all hidden checks passed");
    }
}
