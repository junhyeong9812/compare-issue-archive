import { createHash } from "crypto";
import { OrderEventKind } from "../domain/order";

const KIND_CODE: Record<OrderEventKind, string> = {
  PAID: "paid",
  SHIPPED: "ship",
  DELIVERED: "dlvd",
};

/**
 * 알림 메시지 키 — `<전체 키의 해시 16자>:<네임스페이스>.<이벤트>.<업무 식별자>`.
 * mqpub 는 한 배치 안에서 키의 앞 16바이트가 같은 메시지를 첫 것만 남기고 조용히 버린다.
 * 공통 접두로 시작하는 키는 서로 덮이므로, 메시지마다 다른 결정적 해시를 맨 앞에 둔다.
 * 소비 측은 이 키로 이미 처리한 메시지를 걸러낸다(재전달 대비) — 같은 알림은 늘 같은 키.
 */
export function messageKey(namespace: string, kind: OrderEventKind, orderNo: string): string {
  const logical = `${namespace}.${KIND_CODE[kind]}.${orderNo}`;
  const head = createHash("sha1").update(logical).digest("hex").slice(0, 16);
  return `${head}:${logical}`;
}

export function kindFromCode(code: string): OrderEventKind | undefined {
  return (Object.keys(KIND_CODE) as OrderEventKind[]).find((k) => KIND_CODE[k] === code);
}
