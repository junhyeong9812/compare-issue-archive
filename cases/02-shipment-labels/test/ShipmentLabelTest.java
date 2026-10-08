import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ShipmentLabelTest {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    public static void main(String[] args) throws Exception {
        // 1) one after another: label survives the next carrier sync
        {
            KeyValueStore store = new InMemoryStore();
            ShipmentRepository repo = new ShipmentRepository(store);
            AtomicReference<String> carrier = new AtomicReference<>("PICKED_UP");
            CarrierSync sync = new CarrierSync(repo, id -> carrier.get(), List.of("S1"));
            LabelService labels = new LabelService(repo);
            sync.tick();
            labels.setLabel("S1", "fragile");
            carrier.set("IN_TRANSIT");
            sync.tick();
            Shipment s = repo.load("S1");
            check("fragile".equals(s.label()) && "IN_TRANSIT".equals(s.status()), "sequential: " + s);
        }

        // 2) operator tags the shipment while a carrier sync is in progress
        {
            GatedStore store = new GatedStore(new InMemoryStore());
            ShipmentRepository repo = new ShipmentRepository(store);
            AtomicReference<String> carrier = new AtomicReference<>("PICKED_UP");
            CarrierSync sync = new CarrierSync(repo, id -> carrier.get(), List.of("S1"));
            LabelService labels = new LabelService(repo);
            sync.tick();
            carrier.set("IN_TRANSIT");

            Thread syncThread = new Thread(sync::tick, "carrier-sync");
            store.pauseFirstAccess(syncThread, "shipment:S1");
            syncThread.start();
            store.reached.await(2, TimeUnit.SECONDS);

            Thread uiThread = new Thread(() -> labels.setLabel("S1", "fragile"), "operator-ui");
            uiThread.start();
            uiThread.join(500);
            store.release.countDown();
            syncThread.join(5000);
            uiThread.join(5000);

            Shipment s = repo.load("S1");
            check("fragile".equals(s.label()), "label set during sync is kept (got " + s.label() + ")");
            check("IN_TRANSIT".equals(s.status()), "status updated by sync (got " + s.status() + ")");
        }

        if (failed > 0) {
            System.out.println(failed + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
