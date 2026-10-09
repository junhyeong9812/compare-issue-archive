import { Autopay } from "../domain/autopay";
import { Member } from "../domain/member";
import { Reminder, ReminderKind, reminderKey } from "../domain/reminder";
import { renderMessage } from "../push/messageTemplates";
import { AutopayRepository } from "../repository/autopayRepository";
import { MemberRepository } from "../repository/memberRepository";
import { SendTimePolicy } from "../scheduling/sendTime";
import { at, dateOf, dayOfMonth, LocalDate, LocalDateTime } from "../time/localTime";

export interface DayPlan {
  reminders: Reminder[];
  skippedOptOut: number;
}

/** 어떤 알림을 언제 보낼지 정한다(예약은 하지 않는다). */
export class ReminderPlanner {
  constructor(
    private readonly members: MemberRepository,
    private readonly autopays: AutopayRepository,
    private readonly policy: SendTimePolicy,
    private readonly retryNoticeLeadMinutes: number,
  ) {}

  /** 그날 출금되는 계약마다 출금 예정 알림 1건 */
  billDue(date: LocalDate): DayPlan {
    const reminders: Reminder[] = [];
    let skippedOptOut = 0;
    const seen = new Set<string>();
    for (const a of this.autopays.billingOn(dayOfMonth(date))) {
      const m = this.members.find(a.memberId);
      if (!m) continue;
      if (!m.pushOptIn) {
        skippedOptOut++;
        continue;
      }
      const dueAt = at(date, a.billingTime);
      const r = this.build("BILL_DUE", a, m, dueAt, m.leadMinutes);
      if (seen.has(r.key)) continue;
      seen.add(r.key);
      reminders.push(r);
    }
    return { reminders, skippedOptOut };
  }

  /** 출금 실패 → 재출금 안내 1건(수신 거부면 없음) */
  retryNotice(autopay: Autopay, retryAt: LocalDateTime): Reminder | undefined {
    const m = this.members.find(autopay.memberId);
    if (!m || !m.pushOptIn) return undefined;
    return this.build("RETRY", autopay, m, retryAt, this.retryNoticeLeadMinutes);
  }

  private build(kind: ReminderKind, a: Autopay, m: Member, dueAt: LocalDateTime, lead: number): Reminder {
    return {
      key: reminderKey(kind, a.id, dueAt),
      kind,
      memberId: m.id,
      autopayId: a.id,
      device: m.device,
      dueAt,
      sendAt: this.policy.sendAt(dueAt, lead),
      planDate: dateOf(dueAt),
      message: renderMessage(kind, a, dueAt),
    };
  }
}
