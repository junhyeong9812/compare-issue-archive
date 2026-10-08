public record Shipment(String id, String status, String label) {
    public Shipment withStatus(String newStatus) {
        return new Shipment(id, newStatus, label);
    }

    public Shipment withLabel(String newLabel) {
        return new Shipment(id, status, newLabel);
    }
}
