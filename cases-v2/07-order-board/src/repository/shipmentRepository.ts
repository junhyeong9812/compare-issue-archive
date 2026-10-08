import { Shipment } from "../domain/shipment";

export class ShipmentRepository {
  private readonly byOrder = new Map<string, Shipment>();

  upsert(s: Shipment): void {
    const prev = this.byOrder.get(s.orderId);
    if (prev && prev.updatedAt > s.updatedAt) return; // out-of-order carrier webhook
    this.byOrder.set(s.orderId, { ...s });
  }

  get(orderId: string): Shipment | undefined {
    const s = this.byOrder.get(orderId);
    return s ? { ...s } : undefined;
  }
}
