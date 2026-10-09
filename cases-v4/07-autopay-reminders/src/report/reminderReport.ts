import { KIND_LABEL } from "../domain/reminder";
import { maskDevice } from "../push/deviceToken";
import { DeliveredPush, PushqClient } from "../push/pushqClient";
import { ReminderRepository } from "../repository/reminderRepository";
import { compare, LocalDate, LocalDateTime } from "../time/localTime";

export type LineStatus = "발송" | "미발송" | "예약";

export interface ReportLine {
  key: string;
  memberId: string;
  kind: string;
  dueAt: LocalDateTime;
  sendAt: LocalDateTime;
  ticket: string;
  device: string;
  status: LineStatus;
  deliveredAt?: LocalDateTime;
}

export interface ReminderReport {
  date: LocalDate;
  asOf: LocalDateTime | null;
  planned: number;
  delivered: number;
  missing: number;
  pending: number;
  lines: ReportLine[];
  text: string;
}

/** 출금일 기준 알림 발송 현황 — 운영 화면용 */
export class ReminderReportBuilder {
  constructor(private readonly reminders: ReminderRepository, private readonly push: PushqClient) {}

  build(date: LocalDate, asOf: LocalDateTime | undefined): ReminderReport {
    const byTag = new Map<string, DeliveredPush>();
    for (const d of this.push.delivered()) if (!byTag.has(d.tag)) byTag.set(d.tag, d);

    const lines: ReportLine[] = this.reminders.byPlanDate(date).map((r) => {
      const hit = byTag.get(r.key);
      let status: LineStatus;
      if (hit) status = "발송";
      else if (asOf && compare(r.sendAt, asOf) <= 0) status = "미발송";
      else status = "예약";
      return {
        key: r.key,
        memberId: r.memberId,
        kind: KIND_LABEL[r.kind],
        dueAt: r.dueAt,
        sendAt: r.sendAt,
        ticket: r.ticket ?? "-",
        device: maskDevice(r.device),
        status,
        deliveredAt: hit?.at,
      };
    });
    const count = (s: LineStatus) => lines.filter((l) => l.status === s).length;
    const planned = lines.length;
    const delivered = count("발송");
    const missing = count("미발송");
    const pending = count("예약");
    return {
      date,
      asOf: asOf ?? null,
      planned,
      delivered,
      missing,
      pending,
      lines,
      text: `${date} 알림 — 예약 ${planned} / 발송 ${delivered} / 미발송 ${missing} / 대기 ${pending}`,
    };
  }
}
