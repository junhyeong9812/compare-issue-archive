import { KIND_LABEL } from "../domain/notification";
import { OrderEventKind } from "../domain/order";
import { clip } from "../util/format";

const SMS_WIDTH = 80;

export function renderAlert(kind: OrderEventKind, customerName: string, orderNo: string): string {
  const greeting = `[한빛페이] ${customerName}님, 주문 ${orderNo}`;
  const tail: Record<OrderEventKind, string> = {
    PAID: "결제가 완료되었습니다.",
    SHIPPED: "상품이 출발했습니다.",
    DELIVERED: "배송이 완료되었습니다.",
  };
  return clip(`${greeting} ${KIND_LABEL[kind]} — ${tail[kind]}`, SMS_WIDTH);
}
