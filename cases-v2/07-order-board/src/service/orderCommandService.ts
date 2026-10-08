import { Order, OrderLine, OrderStatus, canTransition } from "../domain/order";
import { PaymentStatus } from "../domain/payment";
import { ShipmentStatus } from "../domain/shipment";
import { OrderRepository } from "../repository/orderRepository";
import { PaymentRepository } from "../repository/paymentRepository";
import { ShipmentRepository } from "../repository/shipmentRepository";

export interface Clock {
  now(): number;
}

export class OrderCommandService {
  private seq = 0;

  constructor(
    private readonly orders: OrderRepository,
    private readonly payments: PaymentRepository,
    private readonly shipments: ShipmentRepository,
    private readonly clock: Clock,
  ) {}

  place(customer: string, lines: OrderLine[]): Order {
    if (!customer) throw new Error("customer is required");
    if (lines.length === 0) throw new Error("order has no lines");
    for (const l of lines) {
      if (!Number.isInteger(l.qty) || l.qty <= 0) throw new Error(`invalid qty for ${l.sku}`);
    }
    const order: Order = {
      id: `o-${++this.seq}`,
      customer,
      status: OrderStatus.Pending,
      lines: lines.map((l) => ({ ...l })),
      createdAt: this.clock.now(),
    };
    this.orders.insert(order);
    return order;
  }

  transition(orderId: string, to: OrderStatus): Order {
    const order = this.orders.get(orderId);
    if (!order) throw new Error(`order ${orderId} not found`);
    if (!canTransition(order.status, to)) throw new Error(`cannot move ${order.status} -> ${to}`);
    order.status = to;
    this.orders.update(order);
    return order;
  }

  recordPayment(orderId: string, status: PaymentStatus, amount: number, method: "card" | "transfer" | "points" = "card"): void {
    if (!this.orders.get(orderId)) throw new Error(`order ${orderId} not found`);
    this.payments.record({ orderId, status, amount, method, updatedAt: this.clock.now() });
  }

  recordShipment(orderId: string, status: ShipmentStatus, carrier = "default"): void {
    if (!this.orders.get(orderId)) throw new Error(`order ${orderId} not found`);
    this.shipments.upsert({ orderId, status, carrier, updatedAt: this.clock.now() });
  }
}
