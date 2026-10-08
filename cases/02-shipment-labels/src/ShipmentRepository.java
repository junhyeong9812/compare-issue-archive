public class ShipmentRepository {
    private static final String PREFIX = "shipment:";

    private final KeyValueStore store;

    public ShipmentRepository(KeyValueStore store) {
        this.store = store;
    }

    public Shipment load(String id) {
        String raw = store.get(PREFIX + id);
        if (raw == null) {
            return new Shipment(id, "UNKNOWN", null);
        }
        String[] parts = raw.split("\t", -1);
        String label = parts[1].isEmpty() ? null : parts[1];
        return new Shipment(id, parts[0], label);
    }

    public void save(Shipment shipment) {
        String label = shipment.label() == null ? "" : shipment.label();
        store.put(PREFIX + shipment.id(), shipment.status() + "\t" + label);
    }
}
