import { ORDER_BADGES } from "./orderBadges";
import { PAYMENT_BADGES } from "./paymentBadges";
import { SHIPMENT_BADGES } from "./shipmentBadges";
import { Badge } from "./types";

export { Badge, Tone } from "./types";

/** Every status the back office can display. */
const ALL_BADGES: Readonly<Record<string, Badge>> = Object.freeze({
  ...ORDER_BADGES,
  ...SHIPMENT_BADGES,
  ...PAYMENT_BADGES,
});

const UNKNOWN_TONE = "neutral" as const;

export function badgeFor(status: string): Badge {
  const badge = ALL_BADGES[status];
  return badge ? { ...badge } : { label: status, tone: UNKNOWN_TONE };
}

export function knownStatuses(): string[] {
  return Object.keys(ALL_BADGES).sort();
}
