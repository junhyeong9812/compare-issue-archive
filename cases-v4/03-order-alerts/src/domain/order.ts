export type Channel = "direct" | "market";
export type OrderEventKind = "PAID" | "SHIPPED" | "DELIVERED";

export interface OrderEvent {
  kind: OrderEventKind;
  /** 이벤트 기록 순번(앱 전역 단조 증가) */
  seq: number;
  dispatched: boolean;
}

export interface Order {
  orderNo: string;
  customerId: string;
  channel: Channel;
  partner?: string;
  placedOn: string; // YYYY-MM-DD
  events: OrderEvent[];
}

export const EVENT_KINDS: readonly OrderEventKind[] = ["PAID", "SHIPPED", "DELIVERED"];

export function isEventKind(v: unknown): v is OrderEventKind {
  return typeof v === "string" && (EVENT_KINDS as readonly string[]).includes(v);
}

/** 2026-10-07 → 261007 */
export function compactDate(placedOn: string): string {
  const m = /^20(\d{2})-(\d{2})-(\d{2})$/.exec(placedOn);
  if (!m) throw new Error("invalid placedOn");
  return `${m[1]}${m[2]}${m[3]}`;
}

/**
 * 주문번호 체계
 *   자사몰:     O<yymmdd>-<일련 5자리>            예) O261007-00042
 *   오픈마켓:   M-<제휴사 4자>-<yymmdd>-<일련 6자리> 예) M-GMKT-261007-000123
 */
export function formatOrderNo(channel: Channel, placedOn: string, seq: number, partner?: string): string {
  const d = compactDate(placedOn);
  if (channel === "direct") return `O${d}-${String(seq).padStart(5, "0")}`;
  if (!partner || !/^[A-Z]{4}$/.test(partner)) throw new Error("invalid partner");
  return `M-${partner}-${d}-${String(seq).padStart(6, "0")}`;
}
