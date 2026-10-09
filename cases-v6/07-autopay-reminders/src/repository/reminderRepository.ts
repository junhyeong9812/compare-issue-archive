import { Reminder } from "../domain/reminder";
import { LocalDate } from "../time/localTime";

/** 예약해 둔 알림 — 키 단위로 한 번만 저장된다. */
export class ReminderRepository {
  private readonly rows = new Map<string, Reminder>();

  has(key: string): boolean {
    return this.rows.has(key);
  }

  save(r: Reminder): void {
    this.rows.set(r.key, { ...r });
  }

  find(key: string): Reminder | undefined {
    const r = this.rows.get(key);
    return r ? { ...r } : undefined;
  }

  byPlanDate(date: LocalDate): Reminder[] {
    return [...this.rows.values()]
      .filter((r) => r.planDate === date)
      .sort((a, b) => (a.sendAt < b.sendAt ? -1 : a.sendAt > b.sendAt ? 1 : a.key < b.key ? -1 : 1))
      .map((r) => ({ ...r }));
  }

  byAutopay(autopayId: string): Reminder[] {
    return [...this.rows.values()].filter((r) => r.autopayId === autopayId).map((r) => ({ ...r }));
  }

  remove(key: string): void {
    this.rows.delete(key);
  }
}
