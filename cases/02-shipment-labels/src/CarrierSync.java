import java.util.List;
import java.util.function.Function;

/** Runs every few seconds on a scheduler thread and copies carrier tracking status into our records. */
public class CarrierSync {
    private final ShipmentRepository repository;
    private final Function<String, String> carrierStatus;
    private final List<String> trackedIds;

    public CarrierSync(ShipmentRepository repository, Function<String, String> carrierStatus, List<String> trackedIds) {
        this.repository = repository;
        this.carrierStatus = carrierStatus;
        this.trackedIds = List.copyOf(trackedIds);
    }

    public void tick() {
        for (String id : trackedIds) {
            String status = carrierStatus.apply(id);
            Shipment current = repository.load(id);
            repository.save(current.withStatus(status));
        }
    }
}
