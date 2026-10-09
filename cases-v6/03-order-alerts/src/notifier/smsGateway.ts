import { createHash } from "crypto";
import { normalizeMobile } from "../util/format";

export interface SmsReceipt {
  ok: boolean;
  receiptId?: string;
  reason?: string;
}

export interface SmsGateway {
  send(from: string, to: string, text: string): SmsReceipt;
}

/**
 * 문자 대행사 실패 응답 코드
 *   invalid-number  수신 번호 형식 오류(우리 쪽 검증)
 *   empty-body      본문 없음(우리 쪽 검증)
 *   E-THROTTLE      초당 발송 한도 초과
 *   E-CARRIER       통신사 일시 반려
 *   E-SPAM          통신사 스팸 차단(수신자 설정)
 */
export type SmsFailure = "invalid-number" | "empty-body" | "E-THROTTLE" | "E-CARRIER" | "E-SPAM";

/**
 * 문자 대행사 모의 게이트웨이 — 받은 문자를 번호별 수신함에 쌓는다.
 * faults: 번호(숫자 11자리)별로 다음 발송 시도들에 차례대로 돌려줄 실패 코드.
 */
export class InMemorySmsGateway implements SmsGateway {
  private readonly inboxes = new Map<string, string[]>();
  private readonly tries = new Map<string, number>();
  private readonly faults = new Map<string, string[]>();
  private sent = 0;

  constructor(faults: Record<string, string[]> = {}) {
    for (const [raw, codes] of Object.entries(faults)) {
      const number = normalizeMobile(raw);
      if (number) this.faults.set(number, [...codes]);
    }
  }

  send(from: string, to: string, text: string): SmsReceipt {
    const number = normalizeMobile(to);
    if (!number) return { ok: false, reason: "invalid-number" };
    if (text.length === 0) return { ok: false, reason: "empty-body" };
    this.tries.set(number, (this.tries.get(number) ?? 0) + 1);
    const fault = this.faults.get(number)?.shift();
    if (fault) return { ok: false, reason: fault };
    this.sent++;
    const list = this.inboxes.get(number) ?? [];
    list.push(text);
    this.inboxes.set(number, list);
    const receiptId = createHash("sha1").update(`${from}|${number}|${this.sent}|${text}`).digest("hex").slice(0, 12);
    return { ok: true, receiptId };
  }

  inbox(phone: string): string[] {
    const number = normalizeMobile(phone);
    return number ? [...(this.inboxes.get(number) ?? [])] : [];
  }

  /** 대행사에 넘긴 발송 요청 수(성공·실패 포함) */
  attempts(phone: string): number {
    const number = normalizeMobile(phone);
    return number ? this.tries.get(number) ?? 0 : 0;
  }
}
