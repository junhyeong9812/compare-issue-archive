import { Order } from "../domain/order";

export class OrderRepository {
  private readonly rows = new Map<string, Order>();

  insert(order: Order): void {
    if (this.rows.has(order.id)) throw new Error(`order ${order.id} already exists`);
    this.rows.set(order.id, structuredCopy(order));
  }

  update(order: Order): void {
    if (!this.rows.has(order.id)) throw new Error(`order ${order.id} not found`);
    this.rows.set(order.id, structuredCopy(order));
  }

  get(id: string): Order | undefined {
    const o = this.rows.get(id);
    return o ? structuredCopy(o) : undefined;
  }

  all(): Order[] {
    return [...this.rows.values()].map(structuredCopy);
  }
}

function structuredCopy(o: Order): Order {
  return { ...o, lines: o.lines.map((l) => ({ ...l })) };
}
