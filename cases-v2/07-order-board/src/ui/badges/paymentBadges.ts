import { PaymentStatus } from "../../domain/payment";
import { Badge } from "./types";

export const PAYMENT_BADGES: Record<PaymentStatus, Badge> = {
  [PaymentStatus.Pending]: { label: "결제 대기", tone: "neutral" },
  [PaymentStatus.Authorized]: { label: "승인", tone: "info" },
  [PaymentStatus.Captured]: { label: "결제 완료", tone: "success" },
  [PaymentStatus.Failed]: { label: "결제 실패", tone: "danger" },
  [PaymentStatus.Refunded]: { label: "환불", tone: "muted" },
};
