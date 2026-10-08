import { ShipmentStatus } from "../../domain/shipment";
import { Badge } from "./types";

export const SHIPMENT_BADGES: Record<ShipmentStatus, Badge> = {
  [ShipmentStatus.Ready]: { label: "출고 준비", tone: "neutral" },
  [ShipmentStatus.InTransit]: { label: "배송 중", tone: "info" },
  [ShipmentStatus.Delivered]: { label: "배송 완료", tone: "success" },
  [ShipmentStatus.Failed]: { label: "배송 실패", tone: "danger" },
  [ShipmentStatus.Returned]: { label: "반송", tone: "warn" },
};
