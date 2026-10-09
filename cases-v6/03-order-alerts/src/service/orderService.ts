import { validateCustomer, Customer } from "../domain/customer";
import { Channel, Order, OrderEventKind, isEventKind } from "../domain/order";
import { CustomerRepository } from "../repository/customerRepository";
import { OrderRepository } from "../repository/orderRepository";

export interface PlaceOrderInput {
  customerId: string;
  channel?: Channel;
  partner?: string;
  placedOn: string;
}

export class OrderService {
  constructor(private readonly customers: CustomerRepository, private readonly orders: OrderRepository) {}

  registerCustomer(input: unknown): Customer {
    return this.customers.save(validateCustomer(input));
  }

  placeOrder(input: PlaceOrderInput): Order {
    if (!this.customers.find(input.customerId)) throw new Error("unknown customer");
    const channel = input.channel ?? "direct";
    if (channel !== "direct" && channel !== "market") throw new Error("invalid channel");
    return this.orders.create(input.customerId, channel, input.placedOn, input.partner);
  }

  recordEvent(orderNo: string, kind: unknown): { orderNo: string; kind: OrderEventKind; seq: number } {
    if (!isEventKind(kind)) throw new Error("invalid event kind");
    const seq = this.orders.addEvent(orderNo, kind);
    return { orderNo, kind, seq };
  }
}
