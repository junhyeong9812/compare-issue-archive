import { isLocalDateTime, LocalDateTime } from "../time/localTime";

/** 자동이체(정기 출금) 계약 — 가맹점이 정한 날·시각에 출금된다. */
export interface Autopay {
  id: string;
  memberId: string;
  merchant: string;
  amount: number;
  /** 매월 출금일(1~28) */
  billingDay: number;
  /** 가맹점 출금 배치 시각 "HH:MM" */
  billingTime: string;
  active: boolean;
}

/** 출금 실패 기록 — 재출금은 실패 처리 시각 기준으로 잡힌다. */
export interface BillingFailure {
  autopayId: string;
  failedAt: LocalDateTime;
  reason: string;
}

export function validateAutopay(input: unknown): Omit<Autopay, "id" | "active"> {
  const b = (input ?? {}) as Record<string, unknown>;
  if (typeof b.memberId !== "string") throw new Error("memberId required");
  if (typeof b.merchant !== "string" || b.merchant.trim() === "") throw new Error("merchant required");
  if (typeof b.amount !== "number" || !Number.isInteger(b.amount) || b.amount <= 0) throw new Error("amount must be a positive integer (KRW)");
  if (typeof b.billingDay !== "number" || !Number.isInteger(b.billingDay) || b.billingDay < 1 || b.billingDay > 28) {
    throw new Error("billingDay must be 1..28");
  }
  if (typeof b.billingTime !== "string" || !/^([01]\d|2[0-3]):[0-5]\d$/.test(b.billingTime)) throw new Error("billingTime must be HH:MM");
  return { memberId: b.memberId, merchant: b.merchant.trim(), amount: b.amount, billingDay: b.billingDay, billingTime: b.billingTime };
}

export function validateFailure(autopayId: string, input: unknown): BillingFailure {
  const b = (input ?? {}) as Record<string, unknown>;
  if (!isLocalDateTime(b.failedAt)) throw new Error("failedAt must be YYYY-MM-DDTHH:MM:SS");
  const reason = typeof b.reason === "string" && b.reason !== "" ? b.reason : "INSUFFICIENT_BALANCE";
  return { autopayId, failedAt: b.failedAt, reason };
}
