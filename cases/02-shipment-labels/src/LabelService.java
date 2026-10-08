/** Called from the operator UI when someone tags a shipment (e.g. "fragile", "VIP"). */
public class LabelService {
    private final ShipmentRepository repository;

    public LabelService(ShipmentRepository repository) {
        this.repository = repository;
    }

    public void setLabel(String id, String label) {
        if (label != null && (label.contains("\t") || label.contains("\n"))) {
            throw new IllegalArgumentException("label must be a single line");
        }
        Shipment current = repository.load(id);
        repository.save(current.withLabel(label));
    }
}
