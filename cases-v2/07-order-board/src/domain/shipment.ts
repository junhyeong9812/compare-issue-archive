export enum ShipmentStatus {
  Ready = "READY",
  InTransit = "IN_TRANSIT",
  Delivered = "DELIVERED",
  Failed = "FAILED",
  Returned = "RETURNED",
}

export interface Shipment {
  orderId: string;
  status: ShipmentStatus;
  carrier: string;
  trackingNo?: string;
  updatedAt: number;
}

export function isShipmentStatus(s: string): s is ShipmentStatus {
  return (Object.values(ShipmentStatus) as string[]).includes(s);
}
