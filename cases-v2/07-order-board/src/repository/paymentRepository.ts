import { Payment } from "../domain/payment";

/** Latest payment record per order (older attempts are kept for audit only). */
export class PaymentRepository {
  private readonly history: Payment[] = [];

  record(p: Payment): void {
    this.history.push({ ...p });
  }

  latestFor(orderId: string): Payment | undefined {
    let latest: Payment | undefined;
    for (const p of this.history) {
      if (p.orderId !== orderId) continue;
      if (!latest || p.updatedAt >= latest.updatedAt) latest = p;
    }
    return latest ? { ...latest } : undefined;
  }

  attempts(orderId: string): number {
    return this.history.filter((p) => p.orderId === orderId).length;
  }
}
