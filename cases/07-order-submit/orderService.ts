export interface OrderLine {
  sku: string;
  qty: number;
}

export interface Order {
  customerId: string;
  lines: OrderLine[];
}

export interface HttpClient {
  post(path: string, body: unknown): Promise<{ id: string }>;
}

export interface Result {
  ok: boolean;
  id?: string;
  error?: string;
}

function validate(order: Order): void {
  if (!order.customerId) throw new Error("customerId is required");
  if (order.lines.length === 0) throw new Error("order has no lines");
  for (const line of order.lines) {
    if (!Number.isInteger(line.qty) || line.qty <= 0) {
      throw new Error(`invalid qty for ${line.sku}`);
    }
  }
}

export class OrderService {
  readonly failures: string[] = [];

  constructor(private readonly http: HttpClient) {}

  async submit(order: Order): Promise<Result> {
    try {
      validate(order);
      return this.http
        .post("/orders", order)
        .then((res) => ({ ok: true, id: res.id }));
    } catch (e) {
      return this.fail("submit", e);
    }
  }

  async cancel(orderId: string): Promise<Result> {
    try {
      if (!orderId) throw new Error("orderId is required");
      return this.http
        .post(`/orders/${orderId}/cancel`, {})
        .then(() => ({ ok: true, id: orderId }));
    } catch (e) {
      return this.fail("cancel", e);
    }
  }

  private fail(op: string, e: unknown): Result {
    const message = e instanceof Error ? e.message : String(e);
    this.failures.push(`${op}: ${message}`);
    return { ok: false, error: message };
  }
}
