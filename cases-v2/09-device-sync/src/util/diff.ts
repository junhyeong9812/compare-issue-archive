import { SettingValue } from "../domain/profile";
import { valuesEqual } from "./deepEqual";

/** Top-level keys whose values differ (added, removed or changed), sorted. */
export function changedKeys(
  before: Record<string, SettingValue>,
  after: Record<string, SettingValue>,
  ignore: readonly string[] = [],
): string[] {
  const keys = new Set([...Object.keys(before), ...Object.keys(after)]);
  const out: string[] = [];
  for (const k of keys) {
    if (ignore.includes(k)) continue;
    const inBefore = Object.prototype.hasOwnProperty.call(before, k);
    const inAfter = Object.prototype.hasOwnProperty.call(after, k);
    if (inBefore !== inAfter || !valuesEqual(before[k], after[k])) out.push(k);
  }
  return out.sort();
}
