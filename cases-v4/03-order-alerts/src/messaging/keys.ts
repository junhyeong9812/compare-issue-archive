import { OrderEventKind } from "../domain/order";

const KIND_CODE: Record<OrderEventKind, string> = {
  PAID: "paid",
  SHIPPED: "ship",
  DELIVERED: "dlvd",
};

/**
 * 알림 메시지 키 — 사내 키 규약 `<네임스페이스>.<이벤트>.<업무 식별자>`.
 * 소비 측은 이 키로 이미 처리한 메시지를 걸러낸다(재전달 대비).
 */
export function messageKey(namespace: string, kind: OrderEventKind, orderNo: string): string {
  return `${namespace}.${KIND_CODE[kind]}.${orderNo}`;
}

export function kindFromCode(code: string): OrderEventKind | undefined {
  return (Object.keys(KIND_CODE) as OrderEventKind[]).find((k) => KIND_CODE[k] === code);
}
