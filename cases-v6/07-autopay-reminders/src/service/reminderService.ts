import { validateFailure } from "../domain/autopay";
import { Reminder } from "../domain/reminder";
import { PushqClient, PushqError } from "../push/pushqClient";
import { AutopayRepository } from "../repository/autopayRepository";
import { ReminderRepository } from "../repository/reminderRepository";
import { addMinutes, compare, LocalDate, LocalDateTime } from "../time/localTime";
import { ReminderPlanner } from "./reminderPlanner";

export interface PlanResult {
  date: LocalDate;
  planned: number;
  scheduled: number;
  alreadyScheduled: number;
  skippedOptOut: number;
}

/** 알림 예약·취소와 벤더 시계 진행 */
export class ReminderService {
  private clock: LocalDateTime | undefined;

  constructor(
    private readonly planner: ReminderPlanner,
    private readonly autopays: AutopayRepository,
    private readonly reminders: ReminderRepository,
    private readonly push: PushqClient,
    private readonly retryDelayMinutes: number,
  ) {}

  now(): LocalDateTime | undefined {
    return this.clock;
  }

  /** 하루치 출금 예정 알림을 예약한다. 다시 불러도 이미 예약한 알림은 건너뛴다. */
  planDay(date: LocalDate): PlanResult {
    const plan = this.planner.billDue(date);
    let scheduled = 0;
    let already = 0;
    for (const r of plan.reminders) {
      if (this.reminders.has(r.key)) {
        already++;
        continue;
      }
      this.schedule(r);
      scheduled++;
    }
    return { date, planned: plan.reminders.length, scheduled, alreadyScheduled: already, skippedOptOut: plan.skippedOptOut };
  }

  /** 출금 실패를 기록하고 재출금 안내를 예약한다. */
  recordFailure(autopayId: string, body: unknown): { retryAt: LocalDateTime; notice?: { key: string; sendAt: LocalDateTime } } {
    const a = this.autopays.find(autopayId);
    if (!a) throw new Error(`autopay ${autopayId} not found`);
    const f = validateFailure(autopayId, body);
    this.autopays.addFailure(f);
    const retryAt = addMinutes(f.failedAt, this.retryDelayMinutes);
    const r = this.planner.retryNotice(a, retryAt);
    if (!r || this.reminders.has(r.key)) return { retryAt };
    this.schedule(r);
    return { retryAt, notice: { key: r.key, sendAt: r.sendAt } };
  }

  /** 계약 해지 — 아직 나가지 않은 알림 예약을 취소한다. */
  cancelAutopay(autopayId: string): { cancelled: number } {
    this.autopays.deactivate(autopayId);
    let cancelled = 0;
    for (const r of this.reminders.byAutopay(autopayId)) {
      if (!r.ticket || (this.clock && compare(r.sendAt, this.clock) <= 0)) continue;
      try {
        this.push.cancel(r.ticket);
        cancelled++;
      } catch (e) {
        // 이미 나간 티켓은 벤더가 E31 을 준다 — 취소할 것이 없으므로 넘어간다.
        if (!(e instanceof PushqError && e.code === "E31")) throw e;
      }
      this.reminders.remove(r.key);
    }
    return { cancelled };
  }

  /** 벤더 시계를 until 까지 진행한다(운영에서는 벤더가 스스로 흐른다). */
  advance(until: LocalDateTime): { delivered: number } {
    const delivered = this.push.advance(until);
    this.clock = until;
    return { delivered };
  }

  private schedule(r: Reminder): void {
    const ticket = this.push.schedule(r.device, r.sendAt, r.key, r.message);
    this.reminders.save({ ...r, ticket });
  }
}
