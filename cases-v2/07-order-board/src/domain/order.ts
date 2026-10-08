export enum OrderStatus {
  Pending = "PENDING",
  Confirmed = "CONFIRMED",
  Shipped = "SHIPPED",
  Completed = "COMPLETED",
  Cancelled = "CANCELLED",
}

export interface OrderLine {
  sku: string;
  qty: number;
  unitPrice: number; // minor units (e.g. won)
}

export interface Order {
  id: string;
  customer: string;
  status: OrderStatus;
  lines: OrderLine[];
  createdAt: number;
}

export function orderTotal(order: Order): number {
  return order.lines.reduce((sum, l) => sum + l.qty * l.unitPrice, 0);
}

const TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  [OrderStatus.Pending]: [OrderStatus.Confirmed, OrderStatus.Cancelled],
  [OrderStatus.Confirmed]: [OrderStatus.Shipped, OrderStatus.Cancelled],
  [OrderStatus.Shipped]: [OrderStatus.Completed],
  [OrderStatus.Completed]: [],
  [OrderStatus.Cancelled]: [],
};

export function canTransition(from: OrderStatus, to: OrderStatus): boolean {
  return TRANSITIONS[from].includes(to);
}

export function isOrderStatus(s: string): s is OrderStatus {
  return (Object.values(OrderStatus) as string[]).includes(s);
}
