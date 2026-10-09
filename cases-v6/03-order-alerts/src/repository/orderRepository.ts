import { Channel, Order, OrderEventKind, formatOrderNo } from "../domain/order";

export interface PendingEvent {
  orderNo: string;
  customerId: string;
  kind: OrderEventKind;
  seq: number;
}

export class OrderRepository {
  private readonly orders = new Map<string, Order>();
  /** 채널·제휴사·주문일별 일련번호 */
  private readonly counters = new Map<string, number>();
  private eventSeq = 0;

  create(customerId: string, channel: Channel, placedOn: string, partner?: string): Order {
    const counterKey = `${channel}|${partner ?? ""}|${placedOn}`;
    const next = (this.counters.get(counterKey) ?? 0) + 1;
    this.counters.set(counterKey, next);
    const order: Order = {
      orderNo: formatOrderNo(channel, placedOn, next, partner),
      customerId,
      channel,
      partner,
      placedOn,
      events: [],
    };
    this.orders.set(order.orderNo, order);
    return order;
  }

  find(orderNo: string): Order | undefined {
    return this.orders.get(orderNo);
  }

  addEvent(orderNo: string, kind: OrderEventKind): number {
    const order = this.orders.get(orderNo);
    if (!order) throw new Error("order not found");
    const seq = ++this.eventSeq;
    order.events.push({ kind, seq, dispatched: false });
    return seq;
  }

  /** 회차에 실렸던 이벤트(주문·종류의 마지막 것)를 다음 회차에 다시 싣는다. */
  requeue(orderNo: string, kind: OrderEventKind): boolean {
    const order = this.orders.get(orderNo);
    const e = order?.events.filter((x) => x.kind === kind && x.dispatched).pop();
    if (!e) return false;
    e.dispatched = false;
    return true;
  }

  /** 아직 발송 회차에 실리지 않은 이벤트를 기록 순으로 꺼내고 실린 것으로 표시한다. */
  takePending(): PendingEvent[] {
    const out: PendingEvent[] = [];
    for (const o of this.orders.values()) {
      for (const e of o.events) {
        if (e.dispatched) continue;
        e.dispatched = true;
        out.push({ orderNo: o.orderNo, customerId: o.customerId, kind: e.kind, seq: e.seq });
      }
    }
    return out.sort((a, b) => a.seq - b.seq);
  }
}
