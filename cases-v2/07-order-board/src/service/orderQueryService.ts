import { Order, OrderStatus, isOrderStatus } from "../domain/order";
import { Payment } from "../domain/payment";
import { Shipment } from "../domain/shipment";
import { OrderRepository } from "../repository/orderRepository";
import { PaymentRepository } from "../repository/paymentRepository";
import { ShipmentRepository } from "../repository/shipmentRepository";
import { orderPriority } from "../ui/statusPriority";
import { sortBy } from "../util/collections";

export interface OrderAggregate {
  order: Order;
  payment?: Payment;
  shipment?: Shipment;
}

export interface OrderQuery {
  status?: string;
  customer?: string;
}

export class OrderQueryService {
  constructor(
    private readonly orders: OrderRepository,
    private readonly payments: PaymentRepository,
    private readonly shipments: ShipmentRepository,
  ) {}

  find(q: OrderQuery = {}): OrderAggregate[] {
    if (q.status !== undefined && !isOrderStatus(q.status)) {
      throw new Error(`unknown order status: ${q.status}`);
    }
    const rows = this.orders
      .all()
      .filter((o) => (q.status ? o.status === q.status : true))
      .filter((o) => (q.customer ? o.customer === q.customer : true))
      .map((order) => this.aggregate(order));
    return sortBy(rows, (r) => orderPriority(r.order.status), (r) => -r.order.createdAt);
  }

  byId(id: string): OrderAggregate | undefined {
    const order = this.orders.get(id);
    return order ? this.aggregate(order) : undefined;
  }

  countByStatus(): Record<OrderStatus, number> {
    const counts = Object.fromEntries(Object.values(OrderStatus).map((s) => [s, 0])) as Record<OrderStatus, number>;
    for (const o of this.orders.all()) counts[o.status]++;
    return counts;
  }

  private aggregate(order: Order): OrderAggregate {
    return {
      order,
      payment: this.payments.latestFor(order.id),
      shipment: this.shipments.get(order.id),
    };
  }
}
