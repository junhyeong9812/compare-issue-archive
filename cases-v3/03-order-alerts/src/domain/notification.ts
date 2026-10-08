import { OrderEventKind } from "./order";

/** 고객에게 보낼 알림 1건 */
export interface Notification {
  orderNo: string;
  customerId: string;
  kind: OrderEventKind;
}

/** 발송 회차 결과 */
export interface DispatchRun {
  runId: number;
  planned: Notification[];
  skippedOptOut: number;
  published: number;
  batch: number;
  delivered: DeliveredAlert[];
  failed: { orderNo: string; kind: OrderEventKind; reason: string }[];
}

export interface DeliveredAlert {
  runId: number;
  orderNo: string;
  customerId: string;
  kind: OrderEventKind;
  phone: string;
  receiptId: string;
}

export const KIND_LABEL: Record<OrderEventKind, string> = {
  PAID: "결제 완료",
  SHIPPED: "배송 시작",
  DELIVERED: "배송 완료",
};
