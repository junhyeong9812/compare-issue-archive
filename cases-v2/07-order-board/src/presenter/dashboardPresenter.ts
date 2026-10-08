import { OrderRow } from "./orderRowPresenter";

export interface DashboardSummary {
  total: number;
  needsAttention: number;
  attentionIds: string[];
  byTone: Record<string, number>;
}

const ATTENTION_TONES = new Set(["warn", "danger"]);

/** A row needs attention when any of its badges is in a warning or danger tone. */
export function summarize(rows: OrderRow[]): DashboardSummary {
  const byTone: Record<string, number> = {};
  const attentionIds: string[] = [];
  for (const row of rows) {
    byTone[row.status.tone] = (byTone[row.status.tone] ?? 0) + 1;
    const badges = [row.status, row.payment, row.shipment].filter((b): b is NonNullable<typeof b> => b !== null);
    if (badges.some((b) => ATTENTION_TONES.has(b.tone))) attentionIds.push(row.id);
  }
  return { total: rows.length, needsAttention: attentionIds.length, attentionIds, byTone };
}
