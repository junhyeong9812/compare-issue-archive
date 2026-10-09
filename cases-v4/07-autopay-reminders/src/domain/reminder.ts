import { LocalDate, LocalDateTime } from "../time/localTime";

export type ReminderKind = "BILL_DUE" | "RETRY";

export const KIND_LABEL: Record<ReminderKind, string> = {
  BILL_DUE: "출금 예정",
  RETRY: "재출금 안내",
};

const KIND_CODE: Record<ReminderKind, string> = {
  BILL_DUE: "due",
  RETRY: "retry",
};

export interface Reminder {
  /** 푸시 태그로도 쓰는 업무 키 — 같은 알림은 늘 같은 키 */
  key: string;
  kind: ReminderKind;
  memberId: string;
  autopayId: string;
  device: string;
  /** 이 알림이 안내하는 출금(또는 재출금) 시각 */
  dueAt: LocalDateTime;
  /** 실제 발송 예약 시각 */
  sendAt: LocalDateTime;
  /** 리포트 묶음 기준일(= 출금일) */
  planDate: LocalDate;
  message: string;
  ticket?: string;
}

export function reminderKey(kind: ReminderKind, autopayId: string, dueAt: LocalDateTime): string {
  return `rmd.${autopayId}.${KIND_CODE[kind]}.${dueAt.slice(0, 10).replace(/-/g, "")}`;
}
