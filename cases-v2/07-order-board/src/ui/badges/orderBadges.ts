import { OrderStatus } from "../../domain/order";
import { Badge } from "./types";

export const ORDER_BADGES: Record<OrderStatus, Badge> = {
  [OrderStatus.Pending]: { label: "접수 대기", tone: "warn" },
  [OrderStatus.Confirmed]: { label: "주문 확정", tone: "info" },
  [OrderStatus.Shipped]: { label: "출고", tone: "info" },
  [OrderStatus.Completed]: { label: "완료", tone: "success" },
  [OrderStatus.Cancelled]: { label: "취소", tone: "muted" },
};
