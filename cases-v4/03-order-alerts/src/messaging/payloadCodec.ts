import { Notification } from "../domain/notification";
import { isEventKind } from "../domain/order";

const VERSION = 2;
const MAX_PAYLOAD = 900;

interface WirePayload {
  v: number;
  o: string;
  c: string;
  k: string;
}

/** 탭·개행·제어문자는 와이어 형식(<key>TAB<payload>)을 깨므로 제거한다. */
function scrub(s: string): string {
  return s.replace(/[\u0000-\u001f\u007f]/g, "");
}

export function encodePayload(n: Notification): string {
  const body: WirePayload = { v: VERSION, o: scrub(n.orderNo), c: scrub(n.customerId), k: n.kind };
  const json = JSON.stringify(body);
  if (json.length > MAX_PAYLOAD) throw new Error("payload too large");
  return json;
}

export function decodePayload(raw: string): Notification | null {
  let body: Partial<WirePayload>;
  try {
    body = JSON.parse(raw) as Partial<WirePayload>;
  } catch {
    return null;
  }
  if (body.v !== VERSION && body.v !== 1) return null;
  if (typeof body.o !== "string" || typeof body.c !== "string" || !isEventKind(body.k)) return null;
  return { orderNo: body.o, customerId: body.c, kind: body.k };
}
