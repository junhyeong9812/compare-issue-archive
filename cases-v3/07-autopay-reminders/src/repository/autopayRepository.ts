import { Autopay, BillingFailure } from "../domain/autopay";

export class AutopayRepository {
  private readonly rows = new Map<string, Autopay>();
  private readonly failures: BillingFailure[] = [];
  private seq = 0;

  create(input: Omit<Autopay, "id" | "active">): Autopay {
    this.seq++;
    const a: Autopay = { ...input, id: `AP${String(this.seq).padStart(5, "0")}`, active: true };
    this.rows.set(a.id, a);
    return { ...a };
  }

  find(id: string): Autopay | undefined {
    const a = this.rows.get(id);
    return a ? { ...a } : undefined;
  }

  deactivate(id: string): void {
    const a = this.rows.get(id);
    if (a) a.active = false;
  }

  /** 해당 출금일에 출금되는 활성 계약 — 등록 순 */
  billingOn(day: number): Autopay[] {
    return [...this.rows.values()].filter((a) => a.active && a.billingDay === day).map((a) => ({ ...a }));
  }

  addFailure(f: BillingFailure): void {
    this.failures.push({ ...f });
  }

  failuresOf(autopayId: string): BillingFailure[] {
    return this.failures.filter((f) => f.autopayId === autopayId).map((f) => ({ ...f }));
  }
}
