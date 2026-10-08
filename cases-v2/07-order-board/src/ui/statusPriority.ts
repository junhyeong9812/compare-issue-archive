import { OrderStatus } from "../domain/order";
import { PaymentStatus } from "../domain/payment";
import { ShipmentStatus } from "../domain/shipment";

// Lower number = shown first in the back-office list.
export const ORDER_PRIORITY: Record<OrderStatus, number> = {
  [OrderStatus.Pending]: 0,
  [OrderStatus.Confirmed]: 1,
  [OrderStatus.Shipped]: 2,
  [OrderStatus.Completed]: 4,
  [OrderStatus.Cancelled]: 5,
};

export const PAYMENT_PRIORITY: Record<PaymentStatus, number> = {
  [PaymentStatus.Failed]: 0,
  [PaymentStatus.Pending]: 1,
  [PaymentStatus.Authorized]: 2,
  [PaymentStatus.Captured]: 3,
  [PaymentStatus.Refunded]: 4,
};

export const SHIPMENT_PRIORITY: Record<ShipmentStatus, number> = {
  [ShipmentStatus.Failed]: 0,
  [ShipmentStatus.Returned]: 1,
  [ShipmentStatus.Ready]: 2,
  [ShipmentStatus.InTransit]: 3,
  [ShipmentStatus.Delivered]: 4,
};

export function orderPriority(status: OrderStatus): number {
  return ORDER_PRIORITY[status] ?? 9;
}
