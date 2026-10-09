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

/** 문자 대행사 모의 게이트웨이 — 받은 문자를 번호별 수신함에 쌓는다. */
export class InMemorySmsGateway implements SmsGateway {
  private readonly inboxes = new Map<string, string[]>();
  private sent = 0;

  send(from: string, to: string, text: string): SmsReceipt {
    const number = normalizeMobile(to);
    if (!number) return { ok: false, reason: "invalid-number" };
    if (text.length === 0) return { ok: false, reason: "empty-body" };
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
}
