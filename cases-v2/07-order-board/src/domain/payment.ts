export enum PaymentStatus {
  Pending = "PENDING",
  Authorized = "AUTHORIZED",
  Captured = "CAPTURED",
  Failed = "FAILED",
  Refunded = "REFUNDED",
}

export interface Payment {
  orderId: string;
  status: PaymentStatus;
  amount: number;
  method: "card" | "transfer" | "points";
  updatedAt: number;
}

export function isPaymentStatus(s: string): s is PaymentStatus {
  return (Object.values(PaymentStatus) as string[]).includes(s);
}
